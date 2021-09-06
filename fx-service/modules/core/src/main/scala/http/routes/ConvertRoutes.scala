package http.routes

import cats.syntax.all._
import ext.http4s.refined._
import cats.MonadThrow
import domain.convertor._
import org.http4s._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.typelevel.log4cats.Logger
import services.FxConverter

case class ConvertRoutes[F[_]: JsonDecoder: Logger : MonadThrow](
     convertor: FxConverter[F]
) extends Http4sDsl[F] {

  private[routes] val prefixPath = "/convert"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case r @ POST -> Root =>
      r.decodeR[FxReq] { fx =>
        for {
          fxRefined <- fx.refine
          result <- convertor.execute(fxRefined._1, fxRefined._2, fxRefined._3)
            .flatMap(Ok(_))
            .recoverWith {
              case BaseCurrencyNotFound(msg) =>
                Logger[F].error(msg)
                NotFound(s"Currency is not supported")
              case ResponseMalformedException(msg) =>
                Logger[F].error(msg)
                InternalServerError("Downstream server is not working properly")
              case FxClientNetworkException(msg) =>
                Logger[F].error(msg)
                BadGateway(msg)
            }
        } yield result
      }
  }

  val routes: HttpRoutes[F] = Router(prefixPath -> httpRoutes)
}

