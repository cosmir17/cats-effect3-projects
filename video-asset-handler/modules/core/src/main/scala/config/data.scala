package config

import config.environments.AppEnvironment
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype

import scala.concurrent.duration._

object data {
  case class AppConfig(httpClientConfig: HttpClientConfig, vcURIConfig: VideoUrlConfig, appEnv: AppEnvironment)

  @newtype case class VideoCompanyUrl(value: NonEmptyString)
  @newtype case class VideoUrlConfig(uri: VideoCompanyUrl)

  case class HttpClientConfig(
      timeout: FiniteDuration,
      idleTimeInPool: FiniteDuration
  )

}
