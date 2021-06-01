package domain

import cats.effect.{Async, ExitCode, Sync}
import cats.implicits._
import domain.metadata.MetaDataException
import domain.video.VideoCheckerException

import scala.util.control.NoStackTrace

object AppExceptionHandler {
  abstract class AppException(msg: String)    extends Exception(msg) with NoStackTrace {
    def getConsoleMsg: String
  }

  implicit class errorHandler[F[_]: Async](effect: F[ExitCode]) { //Todo upper bound didn't work. need to find other way.
    def handleVideoErrors(): F[ExitCode] = effect.handleErrorWith {
      case e: VideoCheckerException =>
        Sync[F].delay(println(s"${e.getConsoleMsg}")) *>
          Sync[F].delay(println(s"Exiting the program")) *> Sync[F].delay(ExitCode.Error)
      case e => Sync[F].delay(println(s"Exiting the program, because ${e.getMessage}")) *> Sync[F].delay(ExitCode.Error)
    }

    def handleMetadataErrors(): F[ExitCode] = effect.handleErrorWith { //Todo upper bound didn't work. need to find other way.
      case e: MetaDataException =>
        Sync[F].delay(println(s"${e.getConsoleMsg}")) *>
          Sync[F].delay(println(s"Exiting the program")) *> Sync[F].delay(ExitCode.Error)
      case e => Sync[F].delay(println(s"Exiting the program, because ${e.getMessage}")) *> Sync[F].delay(ExitCode.Error)
    }
  }
}
