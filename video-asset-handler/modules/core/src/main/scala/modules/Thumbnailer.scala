package modules

import cats.effect._
import cats.effect.std.Console
import cats.syntax.all._
import config.environments.AppEnvironment
import config.environments.AppEnvironment.Test
import domain.thumbnail.VideoFilesDoNotExist
import org.typelevel.log4cats.Logger

import java.io.File
import scala.language.postfixOps
import scala.sys.process._

object Thumbnailer {
  def make[F[_]: Sync : Logger](appEnv: AppEnvironment): Thumbnailer[F] =
    new Thumbnailer[F](appEnv) {}
}

class Thumbnailer[F[_]: Sync : Logger] private (appEnv: AppEnvironment) {
  val isTest = appEnv match { case Test => true; case _ => false }

  def choseFile(): F[String] =
    for {
      files <- Sync[F].delay(new File(".").listFiles.filter(_.isFile).filter(_.getName.endsWith(".mov")).toList)
      _             <- if (files.isEmpty) Sync[F].raiseError(VideoFilesDoNotExist("no video file is present"))
                       else Logger[F].info(s"Listing video files")
      _             <- Logger[F].info(s"_______________________________________________________")
      _             <- files.map(file => Logger[F].info(s"${file.getName}")).traverse(identity)
      _             <- Logger[F].info(s"_______________________________________________________")
      _             <- Logger[F].info(s"type the name of a video file you like for a thumbnail")
      videoFileName <- if (isTest) Sync[F].delay("test_video_file.mov") else Console.make.readLine
      matchingFile  <- Sync[F].delay(new File(".").listFiles.filter(_.isFile).filter(_.getName == videoFileName).toList)
      newName       <- if (matchingFile.isEmpty) Logger[F].info(s"File doesn't exists, please enter a matching name") *> choseFile()
                       else Sync[F].delay(videoFileName)
    } yield newName

  def create(videoFileName: String): F[Unit] =
    for {
      _                 <- Logger[F].info(s"provide a name of your thumbnail file without a format keyword, will be stored as png format")
      thumbnailFileName <- pleaseEnterDifferentFileName()
      _                 <- Logger[F].info(s"Execution Result :")
      executionResult   <- Sync[F].delay[String](s"ffmpeg -ss 00:00:00 -i $videoFileName -vframes 1 $thumbnailFileName.png" !!)
      _                 <- Logger[F].info(s"$executionResult")
    } yield ()

  private def pleaseEnterDifferentFileName(): F[String] = {
    for {
      thumbnailFileName <- if (isTest) Sync[F].delay("test_thumbnail") else Console.make.readLine
      files             <- Sync[F].delay(new File(".").listFiles.filter(_.isFile).filter(_.getName == thumbnailFileName + ".png").toList)
      newName           <- if (files.nonEmpty) Logger[F].info(s"File already exists, please enter a different name") *> pleaseEnterDifferentFileName()
                           else Sync[F].delay(thumbnailFileName)
    } yield newName
  }

}
