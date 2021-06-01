package domain

import derevo.cats._
import derevo.derive
import domain.AppExceptionHandler.AppException

object video {
  abstract class VideoCheckerException(msg: String)  extends AppException(msg)

  @derive(eqv, show)
  case class AssetIdNotFound(msg: String)            extends VideoCheckerException(msg) {
    override def getConsoleMsg = "Supplied Asset ID is not usable, try a different ID"
  }
  @derive(eqv, show)
  case class OtherVideoNetworkException(msg: String) extends VideoCheckerException(msg) {
    override def getConsoleMsg = msg
  }
  @derive(eqv, show)
  case class VideoCorrupted(msg: String)             extends VideoCheckerException(msg) {
    override def getConsoleMsg = s"It's not a valid video, Hash validation failed, $msg"
  }
}
