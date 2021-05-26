package http.clients

import cats.effect.{Async, Concurrent, MonadCancelThrow, Sync}
import domain.metadata._
import config.data.VideoUrlConfig
import cats.syntax.all._
import eu.timepit.refined.auto._
import org.http4s.Method._
import org.http4s._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe._
import org.http4s.client._
import org.http4s.client.dsl.Http4sClientDsl

trait MetaDataClient[F[_]] {
  def query(payment: String): F[MetaData]
}

object MetaDataClient {
  def make[F[_]: JsonDecoder: Concurrent: MonadCancelThrow](cfg: VideoUrlConfig, client: Client[F]): MetaDataClient[F] =
    new MetaDataClient[F] with Http4sClientDsl[F] {
      def query(assetId: String): F[MetaData] =
        Uri.fromString(cfg.uri.value + s"/$assetId" + "/metadata").liftTo[F].flatMap { uri =>
          client.run(GET(uri)).use { resp =>
            resp.status match {
              case Status.Ok | Status.Conflict =>
                resp.asJsonDecode[MetaData]
              case st =>
                resp.bodyText.compile.string.flatMap { bodyString =>
                  MetaDataNetworkException(Option(s"${st.reason} $bodyString")
                    .getOrElse("unknown")).raiseError[F, MetaData]
                }
            }
          }
        }
    }
}
