package domain

import scala.util.control.NoStackTrace

object AppExceptionHandler {
  abstract class AppException(msg: String) extends Exception(msg) with NoStackTrace
}


