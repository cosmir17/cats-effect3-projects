package config

import cats.effect.Async
import ciris._
import config.data._
import config.environments.AppEnvironment._
import config.environments._
import eu.timepit.refined.auto._

import scala.concurrent.duration._

object load {
  // Ciris promotes configuration as code
  def apply[F[_]: Async]: F[AppConfig] =
    env("VH_APP_ENV")
      .default("prod")
      .as[AppEnvironment]
      .flatMap {
        case e @ Test =>
          default[F](VideoCompanyUrl("http://localhost:8080/playground"), e)
        case e @ Prod =>
          default[F](VideoCompanyUrl("http://localhost:8080/playground"), e) //The URL point is removed
      }
      .load[F]

  private def default[F[_]](url: VideoCompanyUrl, appEnv: AppEnvironment): ConfigValue[F, AppConfig] =
    env("DUMMY").as[Duration].default(10.seconds).map { _ =>
      AppConfig(
        HttpClientConfig(timeout = 60.seconds, idleTimeInPool = 30.seconds),
        VideoUrlConfig(url),
        appEnv
      )
    }
}
