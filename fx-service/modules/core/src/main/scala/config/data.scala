package config

import config.environments.AppEnvironment
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype
import com.comcast.ip4s.{ Host, Port }

import scala.concurrent.duration._

object data {
  case class AppConfig(httpServerConfig: HttpServerConfig, httpClientConfig: HttpClientConfig, fxURIConfig: FxUriConfig, appEnv: AppEnvironment)

  @newtype case class FxUrl(value: NonEmptyString)
  @newtype case class FxUriConfig(uri: FxUrl)

  case class HttpServerConfig(host: Host, port: Port)

  case class HttpClientConfig(
      timeout: FiniteDuration,
      idleTimeInPool: FiniteDuration
  )

}
