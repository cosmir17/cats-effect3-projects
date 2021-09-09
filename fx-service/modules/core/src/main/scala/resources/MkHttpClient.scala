package resources

import cats.effect.kernel.{ Async, Resource }
import config.data.HttpClientConfig
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder

/**
  * copied from https://github.com/gvolpe/pfps-shopping-cart/blob/second-edition/modules/core/src/main/scala/shop/resources/MkHttpClient.scala
  * @tparam F
  */
trait MkHttpClient[F[_]] {
  def newEmber(c: HttpClientConfig): Resource[F, Client[F]]
}

object MkHttpClient {
  def apply[F[_]: MkHttpClient]: MkHttpClient[F] = implicitly

  implicit def forAsync[F[_]: Async]: MkHttpClient[F] =
    new MkHttpClient[F] {
      def newEmber(c: HttpClientConfig): Resource[F, Client[F]] =
        EmberClientBuilder
          .default[F]
          .withTimeout(c.timeout)
          .withIdleTimeInPool(c.idleTimeInPool)
          .build
    }
}
