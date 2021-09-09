package config

import cats.effect.Async
import ciris._
import config.data._
import config.environments.AppEnvironment._
import config.environments._
import eu.timepit.refined.auto._
import com.comcast.ip4s._

import scala.concurrent.duration._

object load {
  // Ciris promotes configuration as code
  def apply[F[_]: Async]: F[AppConfig] =
    env("VH_APP_ENV")
      .default("prod")
      .as[AppEnvironment]
      .flatMap {
        case e @ Test =>
          default[F](FxUrl("http://localhost:8080"), e)
        case e @ Prod =>
          default[F](FxUrl("http://943r6.mocklab.io"), e)
      }
      .load[F]

  private def default[F[_]](url: FxUrl, appEnv: AppEnvironment): ConfigValue[F, AppConfig] =
    env("DUMMY").as[Duration].default(10.seconds).map { _ =>
      AppConfig(
        HttpServerConfig(host = host"0.0.0.0", port = port"8080"),
        HttpClientConfig(timeout = 60.seconds, idleTimeInPool = 30.seconds),
        FxUriConfig(url),
        appEnv
      )
    }
}
