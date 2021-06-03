package domain

import cats.effect.{Async, ExitCode, Sync}
import cats.implicits._

import scala.reflect.ClassTag
import scala.util.control.NoStackTrace

object AppExceptionHandler {
  abstract class AppException(msg: String)    extends Exception(msg) with NoStackTrace {
    def getConsoleMsg: String
  }

  implicit class AppErrorHandler[F[_]: Async](effect: F[ExitCode]) {

    def handleAppError[ExtendedException <: AppException : ClassTag](): F[ExitCode] = effect.handleErrorWith {
      case e: ExtendedException =>
        Sync[F].delay(println(s"${e.getConsoleMsg}")) *>
          Sync[F].delay(println(s"Exiting the program")) *> Sync[F].delay(ExitCode.Error)
      case e => Sync[F].delay(println(s"Exiting the program, because ${e.getMessage}")) *> Sync[F].delay(ExitCode.Error)
    }
  }
}


