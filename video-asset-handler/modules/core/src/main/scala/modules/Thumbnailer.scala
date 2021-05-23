package modules

import cats.effect._
import cats.effect.std.Console
import cats.syntax.all._
import config.environments.AppEnvironment
import config.environments.AppEnvironment.Test
import domain.thumbnail.VideoFilesDoNotExist

import java.io.File
import scala.language.postfixOps
import scala.sys.process._

object Thumbnailer {
  def make[F[_]: Sync: MonadCancelThrow](appEnv: AppEnvironment): Thumbnailer[F] =
    new Thumbnailer[F](appEnv) {}
}

class Thumbnailer[F[_]: Sync: MonadCancelThrow] private (appEnv: AppEnvironment) {
  val isTest = appEnv match { case Test => true; case _ => false }

  def choseFile(): F[String] =
    for {
      files <- Sync[F].delay(new File(".").listFiles.filter(_.isFile).filter(_.getName.endsWith(".mov")).toList)
      _             <- if (files.isEmpty) Sync[F].raiseError(VideoFilesDoNotExist("no video file is present, please produce a video first, exiting program"))
                       else Sync[F].delay(println(s"Listing video files"))
      _             <- Sync[F].delay(println(s"_______________________________________________________"))
      _             <- Sync[F].delay(files.foreach(file => println(s"${file.getName}")))
      _             <- Sync[F].delay(println(s"_______________________________________________________"))
      _             <- Sync[F].delay(println(s"type the name of a video file you like for a thumbnail"))
      videoFileName <- if (isTest) Sync[F].delay("test_video_file.mov") else Console.make.readLine
      matchingFile  <- Sync[F].delay(new File(".").listFiles.filter(_.isFile).filter(_.getName == videoFileName).toList)
      newName       <- if (matchingFile.isEmpty) Sync[F].delay(println(s"File doesn't exists, please enter a matching name")) *> choseFile()
                       else Sync[F].delay(videoFileName)
    } yield newName

  def create(videoFileName: String): F[Unit] =
    for {
      _                 <- Sync[F].delay(println(s"provide a name of your thumbnail file without a format keyword, will be stored as png format"))
      thumbnailFileName <- pleaseEnterDifferentFileName()
      _                 <- Sync[F].delay(println(s"Execution Result :"))
      executionResult   <- Sync[F].delay[String](s"ffmpeg -ss ${if(isTest) "00:00:05" else "00:00:5"} -i $videoFileName -vframes 1 $thumbnailFileName.png" !!)
//    _                 <- if //todo
      _                 <- Sync[F].delay(println(s"$executionResult"))
    } yield ()

  private def pleaseEnterDifferentFileName(): F[String] = {
    for {
      thumbnailFileName <- if (isTest) Sync[F].delay("test_thumbnail") else Console.make.readLine
      files             <- Sync[F].delay(new File(".").listFiles.filter(_.isFile).filter(_.getName == thumbnailFileName + ".png").toList)
      newName           <- if (files.nonEmpty) Sync[F].delay(println(s"File already exists, please enter a different name")) *> pleaseEnterDifferentFileName()
                           else Sync[F].delay(thumbnailFileName)
    } yield newName
  }

}
