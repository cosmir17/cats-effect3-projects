package modules

import cats.effect._
import config.data.VideoUrlConfig
import http.clients.{ MetaDataClient, VideoClient }
import org.http4s.client.Client

object HttpClients {
  def make[F[_]: Async: Concurrent: MonadCancelThrow](urlConfig: VideoUrlConfig, client: Client[F]): HttpClients[F] =
    new HttpClients[F] {
      def downloader: VideoClient[F]         = VideoClient.make[F](urlConfig, client)
      def metaDataQuerier: MetaDataClient[F] = MetaDataClient.make[F](urlConfig, client)
    }
}

sealed trait HttpClients[F[_]] {
  def downloader: VideoClient[F]
  def metaDataQuerier: MetaDataClient[F]
}
