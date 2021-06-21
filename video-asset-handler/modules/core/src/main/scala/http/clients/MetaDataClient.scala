package http.clients

import cats.effect.{Async, Concurrent}
import domain.metadata._
import config.data.VideoUrlConfig
import cats.syntax.all._
import eu.timepit.refined.auto._
import org.http4s.Method._
import org.http4s._
import org.http4s.circe._
import org.http4s.client._
import org.http4s.client.dsl.Http4sClientDsl

trait MetaDataClient[F[_]] {
  def query(payment: String): F[MetaData]
}

object MetaDataClient {
  val hashErrorMsg = "The hash data does not conform to the hash standard"

  def make[F[_]: Async : JsonDecoder: Concurrent](cfg: VideoUrlConfig, client: Client[F]): MetaDataClient[F] =
    new MetaDataClient[F] with Http4sClientDsl[F] {
      def query(assetId: String): F[MetaData] =
        Uri.fromString(cfg.uri.value + s"/$assetId" + "/metadata").liftTo[F].flatMap { uri =>
          client.run(GET(uri)).use { resp =>
            resp.status match {
              case Status.Ok | Status.Conflict =>
                resp.asJsonDecode[MetaData]
              case st @ Status.NotFound =>
                AssetIdNotFound(Option(st.reason).getOrElse("unknown")).raiseError[F, MetaData]
              case st =>
                resp.bodyText.compile.string.flatMap { bodyString =>
                  MetaDataNetworkException(Option(s"${st.reason}${if (bodyString.isEmpty) "" else s" $bodyString"}")
                    .getOrElse("unknown")).raiseError[F, MetaData]
                }
            }
          }.adaptError {
            case InvalidMessageBodyFailure(_, _) => MetaDataHashMalformedException(hashErrorMsg)
          }
        }
    }
}
