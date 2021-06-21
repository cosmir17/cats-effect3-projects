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
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers
import domain.generators._
import eu.timepit.refined.api.Refined
import org.scalacheck.Gen

object MetaDataClientSuite extends SimpleIOSuite with Checkers {

  val config = VideoUrlConfig(VideoCompanyUrl("http://localhost"))

  def routes(mkResponse: IO[Response[IO]]) =
    HttpRoutes.of[IO] { case GET -> Root / _ / "metadata" => mkResponse }
      .orNotFound

  val gen = for {
    assetIdGen      <- nonEmptyStringGen(5, 20)
    metaData        <- metaDataGen
  } yield assetIdGen -> metaData

  test("Response Ok (200)") {
    forall(gen) {
      case (assetId, metaData) =>
        val client = Client.fromHttpApp(routes(Ok(metaData)))

        MetaDataClient
          .make[IO](config, client)
          .query(assetId)
          .map(expect.same(metaData, _))
    }
  }

  test("Response Not Found (404)") {
    forall(gen) {
      case (assetId, _) =>
        val client = Client.fromHttpApp(routes(NotFound()))

        MetaDataClient
          .make[IO](config, client)
          .query(assetId)
          .attempt
          .map {
            case Left(e)  => expect.same(AssetIdNotFound("Not Found"), e)
            case Right(_) => failure("unexpected error")
          }
    }
  }

  test("Empty asset ID should yield Asset ID not found exception") {
    val messageFromEndpoint = """{
                                |    "error": "VideoAssetNotFound",
                                |    "description": "The video of the provided asset `metadata` could not be found"
                                |}""".stripMargin

    forall(nonEmptyStringGen(0, 0)) { assetId =>
      val client = Client.fromHttpApp(routes(NotFound(messageFromEndpoint)))

      MetaDataClient
        .make[IO](config, client)
        .query(assetId)
        .attempt
        .map {
          case Left(e)  => expect.same(AssetIdNotFound("Not Found"), e)
          case Right(_) => failure("expected metadata client error")
        }
    }
  }

  test("Internal Server Error response (500)") {
    forall(nonEmptyStringGen(5, 10)) { assetId =>
      val client = Client.fromHttpApp(routes(InternalServerError()))

      MetaDataClient
        .make[IO](config, client)
        .query(assetId)
        .attempt
        .map {
          case Left(e)  => expect.same(MetaDataNetworkException("Internal Server Error"), e)
          case Right(_) => failure("expected metadata client error")
        }
    }
  }

  test("Response Ok (200) but the hashes are malformed") {
    val sha1Gen: Gen[Sha1Pred] = nonEmptyStringGen(10,20).map[Sha1Pred](Refined.unsafeApply)
    val sha256Gen: Gen[Sha256Pred] = nonEmptyStringGen(30, 30).map[Sha256Pred](Refined.unsafeApply)
    val md5Gen: Gen[Md5Pred] = nonEmptyStringGen(70, 70).map[Md5Pred](Refined.unsafeApply)
    val crc32: Gen[Crc32Pred] = nonEmptyStringGen(10, 10).map[Crc32Pred](Refined.unsafeApply)

    val metaDataGen: Gen[MetaData] =
      for {
        sha1          <- sha1Gen
        sha256        <- sha256Gen
        md5           <- md5Gen
        crc32         <- crc32
        vq            <- videoQualityGen
        vi            <- videoIdGen
      } yield MetaData(Sha1(sha1), Sha256(sha256), Md5(md5), Crc32(crc32), vq, vi)

    val gen = for {
      assetIdGen      <- nonEmptyStringGen(5, 20)
      metaData        <- metaDataGen
    } yield assetIdGen -> metaData

    forall(gen) {
      case (assetId, metaData) =>
        val client = Client.fromHttpApp(routes(Ok(metaData)))

        MetaDataClient
          .make[IO](config, client)
          .query(assetId)
          .attempt
          .map {
            case Left(e)  => expect.same(MetaDataHashMalformedException("The hash data does not conform to the hash standard"), e)
            case Right(_) => failure("expected metadata client error")
          }
    }
  }
}
