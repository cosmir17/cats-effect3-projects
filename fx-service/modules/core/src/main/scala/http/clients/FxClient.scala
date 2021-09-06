package http.clients

import domain.convertor._
import cats.effect.{Async, Concurrent}
import config.data.FxUriConfig
import cats.syntax.all._
import eu.timepit.refined.auto._
import io.circe.generic.auto._
import org.http4s.Method._
import org.http4s._
import org.http4s.circe._
import org.http4s.client._
import org.http4s.client.dsl.Http4sClientDsl
import squants.market.Currency

trait FxClient[F[_]] {
  def query(currency: String): F[Map[Currency, BigDecimal]]
}

object FxClient {
  val fxFormatErrorMsg = "The fx response data does not conform to the format we are expecting"

  def make[F[_]: Async : JsonDecoder: Concurrent](cfg: FxUriConfig, client: Client[F]): FxClient[F] =
    new FxClient[F] with Http4sClientDsl[F] {
      def query(currency: String): F[Map[Currency, BigDecimal]] =
        Uri.fromString(cfg.uri.value + s"/$currency").liftTo[F].flatMap { uri =>
          client.run(GET(uri)).use { resp =>
            resp.status match {
              case Status.Ok | Status.Conflict =>
                resp.asJsonDecode[Map[Currency, BigDecimal]]
              case st @ Status.NotFound =>
                BaseCurrencyNotFound(Option(st.reason).getOrElse("unknown")).raiseError[F, Map[Currency, BigDecimal]]
              case st =>
                resp.bodyText.compile.string.flatMap { bodyString =>
                  FxClientNetworkException(Option(s"${st.reason}${if (bodyString.isEmpty) "" else s" $bodyString"}")
                    .getOrElse("unknown")).raiseError[F, Map[Currency, BigDecimal]]
                }
            }
          }.adaptError {
            case InvalidMessageBodyFailure(_, _) => ResponseMalformedException(fxFormatErrorMsg)
          }
        }
    }
}
