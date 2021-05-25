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
    HttpRoutes.of[IO] { case GET -> Root / _ / "metadata" => mkResponse }
      .orNotFound

  val nonEmptyStringGen: Gen[String] =
    Gen.chooseNum(10, 55).flatMap { n => Gen.buildableOfN[String, Char](n, Gen.alphaChar) }

  def nesGen[A](f: String => A): Gen[A] = nonEmptyStringGen.map(f)

  val frameRateGen: Gen[FrameRate] = nesGen(FrameRate.apply)
  val resolutionGen: Gen[Resolution] = nesGen(Resolution.apply)
  val dynamicRangeGen: Gen[DynamicRange] = nesGen(DynamicRange.apply)
  val productionIdGen: Gen[ProductionId] = nesGen(ProductionId.apply)
  val titleGen: Gen[Title] = nesGen(Title.apply)
  val durationGen: Gen[Duration] = nesGen(Duration.apply)
  val sha1Gen: Gen[Sha1] = nesGen(Sha1.apply)
  val sha256Gen: Gen[Sha256] = nesGen(Sha256.apply)
  val md5Gen: Gen[Md5] = nesGen(Md5.apply)
  val crc32: Gen[Crc32] = nesGen(Crc32.apply)

  val videoQualityGen: Gen[VideoQuality] =
    for {
      frameRate <- frameRateGen
      resolution <- resolutionGen
      dynamicRange <- dynamicRangeGen
    } yield VideoQuality(frameRate, resolution, dynamicRange)

  val videoIdGen: Gen[VideoIdentifier] =
    for {
      productionId <- productionIdGen
      title <- titleGen
      duration <- durationGen
    } yield VideoIdentifier(productionId, title, duration)


  val metaDataGen: Gen[MetaData] =
    for {
      sha1 <- sha1Gen
      sha256 <- sha256Gen
      md5 <- md5Gen
      crc32 <- crc32
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
