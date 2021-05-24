package http.clients

import org.http4s.circe.CirceEntityEncoder._
import cats.effect.IO
import config.data._
import domain.video._
import eu.timepit.refined.auto._
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.{HttpRoutes, Response}
import org.scalacheck.Gen
import scodec.bits.ByteVector
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers
import org.scalacheck.Arbitrary._

object VideoClientSuite extends SimpleIOSuite with Checkers {

  val config = VideoUrlConfig(VideoCompanyUrl("http://localhost"))

  def routes(mkResponse: IO[Response[IO]]) =
    HttpRoutes.of[IO] { case POST -> Root / "" => mkResponse }
      .orNotFound

  val byteArray: Gen[Array[Byte]] =
    Gen.listOf(arbitrary[Byte]).map(_.toArray)

  val byteVector: Gen[ByteVector] =
    byteArray.map(ByteVector.apply)

  val nonEmptyStringGen: Gen[String] =
    Gen
      .chooseNum(10, 55)
      .flatMap { n =>
        Gen.buildableOfN[String, Char](n, Gen.alphaChar)
      }

  val gen = for {
    assetIdGen <- nonEmptyStringGen
    videoFileGen <- byteVector
  } yield assetIdGen -> videoFileGen

  test("Response Ok (200)") {
    forall(gen) {
      case (assetId, videoFileGen) =>
        val client = Client.fromHttpApp(routes(Ok(videoFileGen)))

        VideoClient
          .make[IO](config, client)
          .download(assetId)
          .map(expect.same(assetId, _))
    }
  }

  test("Response Not Found (404)") {
    forall(gen) {
      case (assetId, videoFileGen) =>
        val client = Client.fromHttpApp(routes(NotFound(videoFileGen)))

        VideoClient
          .make[IO](config, client)
          .download(assetId)
          .map(expect.same(assetId, _))
    }
  }

  test("Internal Server Error response (500)") {
    forall(nonEmptyStringGen) { assetId =>
      val client = Client.fromHttpApp(routes(InternalServerError()))

      VideoClient
        .make[IO](config, client)
        .download(assetId)
        .attempt
        .map {
          case Left(e)  => expect.same(UnknownNetworkException("Internal Server Error"), e)
          case Right(_) => failure("expected video client error")
        }
    }
  }
}