package config

import config.environments.AppEnvironment
import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*

import scala.concurrent.duration._

object data {
  case class AppConfig(httpClientConfig: HttpClientConfig, vcURIConfig: VideoUrlConfig, appEnv: AppEnvironment)

  case class VideoCompanyUrl(value: String :| Match["^(https?:\\/\\/)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([\\/\\w \\.-]*)*\\/?$"])
  case class VideoUrlConfig(uri: VideoCompanyUrl)

  case class HttpClientConfig(
      timeout: FiniteDuration,
      idleTimeInPool: FiniteDuration
  )

}
