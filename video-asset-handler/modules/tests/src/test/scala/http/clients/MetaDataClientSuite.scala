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

}
