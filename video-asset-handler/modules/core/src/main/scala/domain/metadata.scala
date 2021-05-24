package domain

import derevo.cats._
import derevo.circe.magnolia.{ decoder, encoder }
import derevo.derive
import eu.timepit.refined._
import eu.timepit.refined.api._
import io.circe.Decoder

import scala.util.control.NoStackTrace

object metadata {
  def decoderOf[T, P](implicit v: Validate[T, P], d: Decoder[T]): Decoder[T Refined P] =
    d.emap(refineV[P].apply[T](_))

  @derive(decoder, encoder, show)
  case class VideoQuality(
      frameRate: String,
      resolution: String,
      dynamicRange: String
  )

  @derive(decoder, encoder, show)
  case class VideoIdentifier(
      productionId: String,
      title: String,
      duration: String
  )

  @derive(decoder, encoder, show)
  case class MetaData(
      sha1: String,
      sha256: String,
      md5: String,
      crc32: String,
      videoQuality: VideoQuality,
      identifiers: VideoIdentifier
  )

  abstract class MetaDataException(msg: String) extends Exception(msg) with NoStackTrace

  @derive(eqv, show)
  case class AssetIdNotFound(msg: String)         extends MetaDataException(msg)
  @derive(eqv, show)
  case class UnknownNetworkException(msg: String) extends MetaDataException(msg)
}
