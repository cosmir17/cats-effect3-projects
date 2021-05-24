package http.clients

import cats.effect.IO
import config.data._
import domain.metadata._
import eu.timepit.refined.auto._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.{HttpRoutes, Response}
import org.scalacheck.Gen
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object MetaDataClientSuite extends SimpleIOSuite with Checkers {

  val config = VideoUrlConfig(VideoCompanyUrl("http://localhost"))

  def routes(mkResponse: IO[Response[IO]]) =
    HttpRoutes.of[IO] { case POST -> Root / "" => mkResponse }
      .orNotFound

  val nonEmptyStringGen: Gen[String] =
    Gen
      .chooseNum(10, 55)
      .flatMap { n =>
        Gen.buildableOfN[String, Char](n, Gen.alphaChar)
      }

  val videoQualityGen: Gen[VideoQuality] =
    for {
      frameRate <- nonEmptyStringGen
      resolution <- nonEmptyStringGen
      dynamicRange <- nonEmptyStringGen
    } yield VideoQuality(frameRate, resolution, dynamicRange)

  val videoIdGen: Gen[VideoIdentifier] =
    for {
      productionId <- nonEmptyStringGen
      title <- nonEmptyStringGen
      duration <- nonEmptyStringGen
    } yield VideoIdentifier(productionId, title, duration)


  val metaDataGen: Gen[MetaData] =
    for {
      sha1 <- nonEmptyStringGen
      sha256 <- nonEmptyStringGen
      md5 <- nonEmptyStringGen
      crc32 <- nonEmptyStringGen

      vq <- videoQualityGen
      vi <- videoIdGen
    } yield MetaData(sha1, sha256, md5, crc32, vq, vi)

  val gen = for {
    assetIdGen <- nonEmptyStringGen
    metaData <- metaDataGen
  } yield assetIdGen -> metaData

  test("Response Ok (200)") {
    forall(gen) {
      case (assetId, metaData) =>
        val client = Client.fromHttpApp(routes(Ok(metaData)))

        MetaDataClient
          .make[IO](config, client)
          .query(assetId)
          .map(expect.same(assetId, _))
    }
  }

  test("Response Not Found (404)") {
    forall(gen) {
      case (assetId, metaData) =>
        val client = Client.fromHttpApp(routes(NotFound(metaData)))

        MetaDataClient
          .make[IO](config, client)
          .query(assetId)
          .map(expect.same(assetId, _))
    }
  }

  test("Internal Server Error response (500)") {
    forall(nonEmptyStringGen) { assetId =>
      val client = Client.fromHttpApp(routes(InternalServerError()))

      MetaDataClient
        .make[IO](config, client)
        .query(assetId)
        .attempt
        .map {
          case Left(e)  => expect.same(UnknownNetworkException("Internal Server Error"), e)
          case Right(_) => failure("expected metadata client error")
        }
    }
  }
}
