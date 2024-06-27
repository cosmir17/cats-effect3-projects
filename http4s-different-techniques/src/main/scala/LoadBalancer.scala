package http.routes

import cats.effect.{ExitCode, IO, IOApp, Ref, Resource}
//import cats.syntax.all._
//import org.http4s._
import org.http4s.dsl.Http4sDsl
import com.comcast.ip4s._
import http.routes.LoadBalancer.Urls
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.{HttpRoutes, Request, Uri}
import org.http4s.implicits._
//import pureconfig.ConfigSource

object LoadBalancer {
  type Urls = Vector[Uri]
  object Urls {
    def apply(urls: Vector[Uri]): Urls = urls

    val roundRobin: Urls => Urls =
      vector =>
        if (vector.isEmpty) vector
        else vector.tail :+ vector.head

    val first: Urls => Option[Uri] =
      _.headOption
  }
  // http post      :  mytwitter.com/'user/rockthejvm' -> replica:8081/'user/rockthejvm'

  def apply(
             backends: Ref[IO, Urls],
             sendAndExpect: (Request[IO], Uri) => IO[String], // redirecting of HTTP requests
             addPathToBackend: (Request[IO], Uri) => IO[Uri], // massage the HTTP request for the replica
             updateFunction: Urls => Urls, //shuffle the list of backends
             extractor: Urls => Option[Uri] //extract the backend for which I will send my new HTTP request
           ): Resource[IO, HttpRoutes[IO]] = {
    val dsl = Http4sDsl[IO]
    import dsl._

    val routes = HttpRoutes.of[IO] { request =>
      IO.println(s"Request routed: ${request.uri.path}     method : ${request.method}") *>
      backends.getAndUpdate(updateFunction).map(extractor).flatMap {
        _.fold(Ok("All backends are inactive")) { backendUri =>
          for {
            uri <- addPathToBackend(request, backendUri)
            response <- sendAndExpect(request, uri) // String
            result <- Ok(response)
          } yield result
        }
      }
      // extract the first replica
      // forward the request
      // shuffle the replicas
      // return the response to the user
    }

    Resource.pure(routes)
  }
}

object Replica extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    val port = args.head.toInt
    val host = "localhost"

    val dsl = Http4sDsl[IO]
    import dsl._

    val routes = HttpRoutes.of[IO] { request =>
      IO.println(s"Request received: ${request.uri.path}     method : ${request.method}") *>
        Ok(s"[replica:$port] You've accessed: ${request.uri.path}")
    }

    val maybeServer = for {
      h <- Host.fromString(host)
      p <- Port.fromInt(port)
    } yield EmberServerBuilder
      .default[IO]
      .withHost(h)
      .withPort(p)
      .withHttpApp(routes.orNotFound)
      .build

    maybeServer.map(_.use(_ => IO.println(s"Replica - port $port") *> IO.never))
      .getOrElse(IO.println("Host/port combo not ok"))
      .as(ExitCode.Success)
  }

}

object BigApp extends IOApp.Simple {

  def sendReq(client: Client[IO]) = (req: Request[IO], uri: Uri) =>
    client.expect[String](req.withUri(uri))

  def addReqPathToBackend(req: Request[IO], uri: Uri): IO[Uri] =
    IO.pure {
      uri / req.uri.path.renderString.dropWhile(_ != '/') // /user/my_endpoint?id=123
    }

//  def getSeedNodes: IO[Urls] = IO.fromEither(
//    ConfigSource.default.at("backend").load[List[String]]
//      .map(string => string.map(Uri.unsafeFromString)) // IO[List[Uri]]
//      .map(_.toVector)
//      .map(_.toUrls) //IO[Urls]
//      .leftMap(e => new RuntimeException("Can't parse: " + e))
//  )

  override def run: IO[Unit] = {
    val serverResource = for {
      seedNodes <- Resource.pure(Vector(
        uri"http://localhost:8090",
        uri"http://localhost:8091",
        uri"http://localhost:8092",
      ))
      backends <- Resource.eval(Ref.of[IO, Urls](seedNodes))
      client <- EmberClientBuilder.default[IO].build
      loadBalancer <- LoadBalancer(
        backends, sendReq(client), addReqPathToBackend,  Urls.roundRobin, Urls.first
      )
      server <- EmberServerBuilder
        .default[IO].withHost(host"localhost").withPort(port"8099").withHttpApp(loadBalancer.orNotFound).build
    } yield server

    serverResource.use(_ => IO.println("Rock the JVM - load balancer!") *> IO.never)
  }

}