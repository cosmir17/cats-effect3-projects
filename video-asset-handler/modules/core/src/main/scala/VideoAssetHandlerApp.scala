import com.monovore.decline.{Command, Opts}
import cats.implicits._
import com.monovore.decline.effect.CommandIOApp
import modules.{HttpClients, Thumbnailer, VideoEnquirer}
import cats.effect._
import domain.AppExceptionHandler._
import domain.downloader.DownloaderException
import domain.thumbnail.ThumbnailException
import org.typelevel.log4cats.Logger
import resources.AppResources
import org.typelevel.log4cats.slf4j.Slf4jLogger

object VideoAssetHandlerApp extends CommandIOApp(name = "video-handler", header = s"""
    | _______ _______ _______ __    _ __ _______
    ||       |       |   _   |  |  | |  |       |
    ||  _____|    ___|  |_|  |   |_| |__|  _____|
    || |_____|   |___|       |       |  | |_____
    ||_____  |    ___|       |  _    |  |_____  |
    | _____| |   |___|   _   | | |   |   _____| |
    ||_______|__________| _______ ______________|
    ||  | |  |   |      ||       |       |
    ||  |_|  |   |  _    |    ___|   _   |
    ||       |   | | |   |   |___|  | |  |
    ||       |   | |_|   |    ___|  |_|  |
    | |     ||   |       |   |___|       |
    | _______|__________||_______________|    _______ ______
    ||  | |  |   _   |  |  | |      ||   |   |       |    _ |
    ||  |_|  |  |_|  |   |_| |  _    |   |   |    ___|   | ||
    ||       |       |       | | |   |   |   |   |___|   |_||_
    ||       |       |  _    | |_|   |   |___|    ___|    __  |
    ||   _   |   _   | | |   |       |       |   |___|   |  | |
    ||__| |__|__| |__|_|  |__|______||_______|_______|___|  |_|
    |
    |Sean's Video Asset Handler Command Line Tool
    |""".stripMargin, version = "0.0.1") {
  implicit val logger = Slf4jLogger.getLogger[IO]

  sealed trait Cmd
  case class VideoAssetCmd(onlyQuery: Boolean, assetId: String) extends Cmd
  case class ThumbnailCmd(fileName: String)                     extends Cmd

  lazy val downloadAsset = {
    Command(
      name = "download-asset",
      header = "download a video with an asset id from the server"
    )(Opts.argument[String](metavar = "asset-id").map(FMain.downloadAsset[IO](_)))
  }

  lazy val thumbnailAsset =
    Command(
      name = "produce-thumbnail",
      header = "generate a thumbnail from a video file already downloaded"
    )(FMain.produceThumbnail[IO]().pure[Opts])

  lazy val command =
    Opts.subcommands(
      downloadAsset,
      thumbnailAsset
    )

  override def main: Opts[IO[ExitCode]] =
    command
}

object FMain {

  def downloadAsset[F[_]: Async : Logger](assetId: String): F[ExitCode] =
    config.load[F].flatMap { cfg =>
      AppResources
        .make[F](cfg)
        .map(res => HttpClients.make[F](cfg.vcURIConfig, res.client))
        .use { clients =>
          val checker = VideoEnquirer.make[F](clients, cfg.appEnv)
          for {
            _             <- Logger[F].info("")
            _             <- Logger[F].info(s"Sending a download request to the ${cfg.vcURIConfig} endpoints")
            _             <- if (assetId == "") Sync[F].raiseError(new IllegalArgumentException("Asset ID can't be empty"))
                             else Sync[F].unit
            videoResource <- checker.downloadAndCheckIntegrity(assetId)
            _             <- checker.saveAsFile(videoResource)
            _             <- Logger[F].info(s"Program Exiting")
          } yield ExitCode.Success
        }
    }.handleAppError[DownloaderException]()

  def produceThumbnail[F[_]: Async : Logger](): F[ExitCode] =
    config.load[F].flatMap { cfg =>
      val thumbnailer = Thumbnailer.make[F](cfg.appEnv)
      for {
        _             <- Logger[F].info("")
        _             <- Logger[F].info(s"Starting thumbnail producer..")
        videoFileName <- thumbnailer.choseFile()
        _             <- thumbnailer.create(videoFileName)
        _             <- Logger[F].info(s"A thumbnail is produced, exiting program")
      } yield ExitCode.Success
    }.handleAppError[ThumbnailException]()

}
