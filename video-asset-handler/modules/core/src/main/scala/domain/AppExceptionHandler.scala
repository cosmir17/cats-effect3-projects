package domain

import cats.effect.{Async, ExitCode, Sync}
import cats.implicits._
import org.typelevel.log4cats.Logger

import scala.reflect.ClassTag
import scala.util.control.NoStackTrace

object AppExceptionHandler {
  abstract class AppException(msg: String)    extends Exception(msg) with NoStackTrace {
    def getConsoleMsg: String
  }

  implicit class AppErrorHandler[F[_]: Async : Logger](effect: F[ExitCode]) {

    def handleAppError[ExtendedException <: AppException : ClassTag](): F[ExitCode] = effect.handleErrorWith {
      case e: ExtendedException =>
        Logger[F].error(s"${e.getConsoleMsg}") *>
          Logger[F].error(s"Exiting the program") *> Sync[F].delay(ExitCode.Error)
      case e => Logger[F].error(s"Exiting the program, because ${e.getMessage}") *> Sync[F].delay(ExitCode.Error)
    }
  }
}


