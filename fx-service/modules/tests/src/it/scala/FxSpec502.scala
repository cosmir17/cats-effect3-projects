import cats.effect.{IO, Resource}
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import config.data.{FxUriConfig, FxUrl, HttpClientConfig}
import domain.convertor._
import eu.timepit.refined.auto._
import http.clients.FxClient
import http.routes.ConvertRoutes
import io.circe.generic.auto._
import org.http4s._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.client.Client
import org.http4s.syntax.literals._
import org.typelevel.log4cats.testing.TestingLogger
import resources.MkHttpClient
import services.FxConverter

import scala.concurrent.duration._

/*
FxSpec502 and FxSpec200 are to be together. I don't know how to reset SharedResource for each test case.
 */
object FxSpec502 extends HttpSuite {
  override type Res = Client[IO]
  override def sharedResource: Resource[IO, Res] =
    MkHttpClient[IO].newEmber(HttpClientConfig(timeout = 60.seconds, idleTimeInPool = 30.seconds))

  override def maxParallelism = 1
  val Port = 8080
  val Host = "localhost"

  val wireMockServer = new WireMockServer(wireMockConfig().port(Port))

  test("fx-service should return 502 when the downstream call is not available") { client =>
    implicit val logger = TestingLogger.impl()

    val wm = IO(wireMockServer.start()) *> IO(WireMock.configureFor(Host, Port))
    val path = "/exchange-rates/"
    val currency = "GBP"

    val fxStub = IO(stubFor(get(urlEqualTo(path + currency))
      .willReturn(
        aResponse().withStatus(500))))

    val fxClient = FxClient.make[IO](FxUriConfig(FxUrl("http://localhost:8080")), client)
    val reqEntityBody = FxReq("GBP", "EUR", 102.6) //not ideal but circe doesn't like raw string value

    val responseBody = "\"Server Error\""

    val req = Request[IO](method = Method.POST, uri = uri"/convert").withEntity(reqEntityBody)

    for {
      _        <- wm
      _        <- fxStub
      routes   =  ConvertRoutes[IO](FxConverter.make[IO](fxClient)).routes
      expected <- expectHttpBodyAndStatus(routes, req)(responseBody, Status.BadGateway)
      _        <- IO(wireMockServer.stop())
    } yield expected
  }
}