package domain

import derevo.cats.{eqv, show}
import derevo.derive
import domain.AppExceptionHandler.AppException

object thumbnail {
  abstract class ThumbnailException(msg: String) extends AppException(msg)

  @derive(eqv, show)
  case class VideoFilesDoNotExist(msg: String) extends ThumbnailException(msg) {
    override def getConsoleMsg: String = "No video file is present, please produce a video first"
  }
}
