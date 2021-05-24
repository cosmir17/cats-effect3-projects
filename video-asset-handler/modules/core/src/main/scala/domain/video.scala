package domain

import scala.util.control.NoStackTrace
import derevo.cats._
import derevo.derive

object video {
  abstract class VideoCheckerException(msg: String) extends Exception(msg) with NoStackTrace

  @derive(eqv, show)
  case class AssetIdNotFound(msg: String)         extends VideoCheckerException(msg)
  @derive(eqv, show)
  case class UnknownNetworkException(msg: String) extends VideoCheckerException(msg)
  @derive(eqv, show)
  case class VideoCorrupted(msg: String)          extends VideoCheckerException(msg)
}
