package domain

import derevo.cats.{ eqv, show }
import derevo.derive

import scala.util.control.NoStackTrace

object thumbnail {
  abstract class ThumbnailException(msg: String) extends Exception(msg) with NoStackTrace

  @derive(eqv, show)
  case class VideoFilesDoNotExist(msg: String) extends ThumbnailException(msg)
}
