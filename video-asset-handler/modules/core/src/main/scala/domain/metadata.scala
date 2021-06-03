package domain

import derevo.cats._
import derevo.derive
import derevo.circe.magnolia.{decoder, encoder}
import domain.downloader.DownloaderException
import eu.timepit.refined._
import eu.timepit.refined.api._
import eu.timepit.refined.cats._
import eu.timepit.refined.collection.Size
import eu.timepit.refined.string.MatchesRegex
import io.estatico.newtype.macros.newtype
import io.circe.Decoder
import io.circe.refined._

object metadata {
  implicit def validateSizeN[N <: Int, R](implicit w: ValueOf[N]): Validate.Plain[R, Size[N]] =
    Validate.fromPredicate[R, Size[N]](
      _.toString.size == w.value,
      _ => s"Must have ${w.value} digits",
      Size[N](w.value)
    )

  def decoderOf[T, P](implicit v: Validate[T, P], d: Decoder[T]): Decoder[T Refined P] =
    d.emap(refineV[P].apply[T](_))
  type DurationRgxT    = W.`"""^\\d+:\\d{2}:\\d{2}$"""`.T
  type DurationPred   = String Refined MatchesRegex[DurationRgxT]

  type Sha1Pred       = String Refined Size[40]
  type Sha256Pred     = String Refined Size[64]
  type Md5Pred        = String Refined Size[32]
  type Crc32Pred      = String Refined Size[8]

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

  @derive(decoder, encoder, show)
  @newtype
  case class Duration(value: DurationPred)

  @derive(decoder, encoder, show)
  case class VideoIdentifier(
      productionId: ProductionId,
      title: Title,
      duration: Duration
  )

  @derive(encoder, show)
  @newtype
  case class Sha1(value: Sha1Pred)
  object Sha1 {
    implicit val jsonDecoder: Decoder[Sha1] =
      decoderOf[String, Size[40]].map(Sha1(_))
  }

  @derive(encoder, show)
  @newtype
  case class Sha256(value: Sha256Pred)
  object Sha256 {
    implicit val jsonDecoder: Decoder[Sha256] =
      decoderOf[String, Size[64]].map(Sha256(_))
  }

  @derive(encoder, show)
  @newtype
  case class Md5(value: Md5Pred)
  object Md5 {
    implicit val jsonDecoder: Decoder[Md5] =
      decoderOf[String, Size[32]].map(Md5(_))
  }

  @derive(encoder, show)
  @newtype
  case class Crc32(value: Crc32Pred)
  object Crc32 {
    implicit val jsonDecoder: Decoder[Crc32] =
      decoderOf[String, Size[8]].map(Crc32(_))
  }

  @derive(decoder, encoder, show)
  case class MetaData(
      sha1: Sha1,
      sha256: Sha256,
      md5: Md5,
      crc32: Crc32,
      videoQuality: VideoQuality,
      identifiers: VideoIdentifier
  )

  abstract class MetaDataException(msg: String)    extends DownloaderException(msg)

  @derive(eqv, show)
  case class AssetIdNotFound(msg: String)          extends MetaDataException(msg)  {
    override def getConsoleMsg = "Supplied Asset ID is not usable, try a different ID"
  }

  @derive(eqv, show)
  case class MetaDataNetworkException(msg: String) extends MetaDataException(msg) {
    override def getConsoleMsg = msg
  }
}
