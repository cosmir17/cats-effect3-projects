package http.clients

import config.data.VideoUrlConfig
import domain.video.{ AssetIdNotFound, _ }
import cats.effect.MonadCancelThrow
import cats.effect.Concurrent
import cats.syntax.all._
import eu.timepit.refined.auto._
import org.http4s.Method._
import org.http4s._
import org.http4s.client._
import org.http4s.client.dsl.Http4sClientDsl
import cats.effect._
import scodec.bits.ByteVector

trait VideoClient[F[_]] {
  def download(assetId: String): F[ByteVector]
}

object VideoClient {
  def make[F[_]: Async: Concurrent: MonadCancelThrow](cfg: VideoUrlConfig, client: Client[F]): VideoClient[F] =
    new VideoClient[F] with Http4sClientDsl[F] {
      def download(assetId: String): F[ByteVector] =
        Uri.fromString(cfg.uri.value + s"/$assetId").liftTo[F].flatMap { uri =>
          client.run(GET(uri)).use { resp =>
            resp.status match {
              case Status.Ok =>
                resp.body.compile.to(ByteVector)
              case st =>
                resp.bodyText.compile.string.flatMap { bodyString =>
                  {
                    if (bodyString.contains("VideoAssetNotFound"))
                      AssetIdNotFound(Option(st.reason).getOrElse("unknown")).raiseError[F, ByteVector]
                    else UnknownNetworkException(Option(st.reason).getOrElse("unknown")).raiseError[F, ByteVector]
                  }
                }
            }
          }
        }
    }
}
