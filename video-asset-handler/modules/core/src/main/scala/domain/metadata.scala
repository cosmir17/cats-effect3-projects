package domain

import derevo.cats._
import derevo.circe.magnolia._
import derevo.derive
import io.estatico.newtype.macros.newtype

import scala.util.control.NoStackTrace

object metadata {

  @derive(decoder, encoder, show, eqv)
  @newtype
  case class FrameRate(value: String)

  @derive(decoder, encoder, show, eqv)
  @newtype
  case class Resolution(value: String)

  @derive(decoder, encoder, show, eqv)
  @newtype
  case class DynamicRange(value: String)

  @derive(decoder, encoder, show, eqv)
  case class VideoQuality(
      frameRate: FrameRate,
      resolution: Resolution,
      dynamicRange: DynamicRange
  )

  @derive(decoder, encoder, show, eqv)
  @newtype
  case class ProductionId(value: String)

  @derive(decoder, encoder, show, eqv)
  @newtype
  case class Title(value: String)

  @derive(decoder, encoder, show, eqv)
  @newtype
  case class Duration(value: String)

  @derive(decoder, encoder, show, eqv)
  case class VideoIdentifier(
      productionId: ProductionId,
      title: Title,
      duration: Duration
  )

  @derive(decoder, encoder, show, eqv)
  @newtype
  case class Sha1(value: String)

  @derive(decoder, encoder, show, eqv)
  @newtype
  case class Sha256(value: String)

  @derive(decoder, encoder, show, eqv)
  @newtype
  case class Md5(value: String)

  @derive(decoder, encoder, show, eqv)
  @newtype
  case class Crc32(value: String)

  @derive(decoder, encoder, show, eqv)
  case class MetaData(
      sha1: Sha1,
      sha256: Sha256,
      md5: Md5,
      crc32: Crc32,
      videoQuality: VideoQuality,
      identifiers: VideoIdentifier
  )

  abstract class MetaDataException(msg: String) extends Exception(msg) with NoStackTrace

  @derive(eqv, show)
  case class MetaDataNetworkException(msg: String) extends MetaDataException(msg)
}
