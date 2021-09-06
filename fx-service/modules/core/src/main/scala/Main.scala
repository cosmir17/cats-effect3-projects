import cats.effect._
import cats.effect.std.Supervisor
import eu.timepit.refined.auto._
import modules.{HttpApi, HttpClients, Programs}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import resources.{AppResources, MkHttpServer}

object Main extends IOApp.Simple {

  implicit val logger = Slf4jLogger.getLogger[IO]

  override def run: IO[Unit] =
    config.load[IO].flatMap { cfg =>
      Logger[IO].info(s"Loaded config $cfg") >>
        Supervisor[IO].use { implicit sp =>
          AppResources
            .make[IO](cfg)
            .map { res =>
              val clients  = HttpClients.make[IO](cfg.fxURIConfig, res.client)
              val programs = Programs.make[IO](clients)
              val api      = HttpApi.make[IO](programs)
              cfg.httpServerConfig -> api.httpApp
            }
            .flatMap {
              case (cfg, httpApp) =>
                MkHttpServer[IO].newEmber(cfg, httpApp)
            }
            .useForever
        }
    }

}