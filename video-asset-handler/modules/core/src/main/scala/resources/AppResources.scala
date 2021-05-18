package resources

import cats.effect.std.Console
import cats.effect.{ Concurrent, Resource }
import config.data.AppConfig
import fs2.io.net.Network
import org.http4s.client.Client
import org.typelevel.log4cats.Logger

sealed abstract class AppResources[F[_]](val client: Client[F])

object AppResources {
  def make[F[_]: Concurrent: Console: Logger: MkHttpClient: Network](
      cfg: AppConfig
  ): Resource[F, AppResources[F]] = {
    (MkHttpClient[F].newEmber(cfg.httpClientConfig)).map(new AppResources[F](_) {})
  }

}
