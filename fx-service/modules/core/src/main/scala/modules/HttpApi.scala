package modules

import scala.concurrent.duration._
import http.routes._
import cats.effect.Async
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.middleware._
import org.typelevel.log4cats.Logger

object HttpApi {
  def make[F[_]: Async : Logger](programs: Programs[F]): HttpApi[F] =
    new HttpApi[F](programs) {}
}

sealed abstract class HttpApi[F[_]: Async : Logger] private (programs: Programs[F]) {
  private val convertRoutes = ConvertRoutes[F](programs.fxConvertor).routes

  private val routes: HttpRoutes[F] = Router( "/api" -> convertRoutes)

  private val middleware: HttpRoutes[F] => HttpRoutes[F] = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    } andThen { http: HttpRoutes[F] =>
      CORS(http)
    } andThen { http: HttpRoutes[F] =>
      Timeout(60.seconds)(http)
    }
  }

  private val loggers: HttpApp[F] => HttpApp[F] = {
    { http: HttpApp[F] =>
      RequestLogger.httpApp(true, true)(http)
    } andThen { http: HttpApp[F] =>
      ResponseLogger.httpApp(true, true)(http)
    }
  }

  val httpApp: HttpApp[F] = loggers(middleware(routes).orNotFound)

}
