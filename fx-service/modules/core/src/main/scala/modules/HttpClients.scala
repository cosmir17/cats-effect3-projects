package modules

import cats.effect._
import config.data.FxUriConfig
import http.clients.FxClient
import org.http4s.client.Client

object HttpClients {
  def make[F[_]: Async](urlConfig: FxUriConfig, client: Client[F]): HttpClients[F] =
    new HttpClients[F] {
      def fxQuerier: FxClient[F] = FxClient.make[F](urlConfig, client)
    }
}

sealed trait HttpClients[F[_]] {
  def fxQuerier: FxClient[F]
}
