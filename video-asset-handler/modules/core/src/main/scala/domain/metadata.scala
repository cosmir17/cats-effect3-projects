package domain

import cats.{Eq, Show}
import cats.syntax.all.*
import io.circe.Codec
import domain.downloader.DownloaderException
//import eu.timepit.refined.*
//import eu.timepit.refined.api.*
//import eu.timepit.refined.cats.*
//import eu.timepit.refined.collection.Size
//import eu.timepit.refined.string.MatchesRegex
//import io.estatico.newtype.macros.newtype

import io.github.iltotore.iron.*
import io.github.iltotore.iron.constraint.all.*

import io.circe.Decoder
import io.circe.refined.*

object metadata {
//  implicit def validateSizeN[N <: Int, R](implicit w: ValueOf[N]): Validate.Plain[R, Size[N]] =
//    Validate.fromPredicate[R, Size[N]](
//      _.toString.size == w.value,
//      _ => s"Must have ${w.value} digits",
//      Size[N](w.value)
//    )

//  def decoderOf[T, P](implicit v: Validate[T, P], d: Decoder[T]): Decoder[T Refined P] =
//    d.emap(refineV[P].apply[T](_))
  type DurationPred   = String :| Match["^\\d+:\\d{2}:\\d{2}$"]

  type Sha1Pred       = String :| Length[40]
  type Sha256Pred     = String :| Length[64]
  type Md5Pred        = String :| Length[32]
  type Crc32Pred      = String :| Length[8]

  case class FrameRate(value: String) derives Codec.AsObject, Eq, Show

  case class Resolution(value: String) derives Codec.AsObject, Eq, Show

  case class DynamicRange(value: String) derives Codec.AsObject, Eq, Show

  case class VideoQuality(
      frameRate: FrameRate,
      resolution: Resolution,
      dynamicRange: DynamicRange
  ) derives Codec.AsObject, Eq, Show

  case class ProductionId(value: String) derives Codec.AsObject, Eq, Show

  case class Title(value: String) derives Codec.AsObject, Eq, Show

  case class Duration(value: DurationPred) derives Codec.AsObject, Show
  
  case class VideoIdentifier(
      productionId: ProductionId,
      title: Title,
      duration: Duration
  ) derives Codec.AsObject, Show

  case class Sha1(value: Sha1Pred) derives Codec.AsObject, Show
  object Sha1 {
    implicit val jsonDecoder: Decoder[Sha1] =
      decoderOf[String, Size[40]].map(Sha1(_))
  }

  case class Sha256(value: Sha256Pred) derives Codec.AsObject, Show
  object Sha256 {
    implicit val jsonDecoder: Decoder[Sha256] =
      decoderOf[String, Size[64]].map(Sha256(_))
  }

  case class Md5(value: Md5Pred) derives Codec.AsObject, Show
  object Md5 {
    implicit val jsonDecoder: Decoder[Md5] =
      decoderOf[String, Size[32]].map(Md5(_))
  }

  case class Crc32(value: Crc32Pred) derives Codec.AsObject, Show
  object Crc32 {
    implicit val jsonDecoder: Decoder[Crc32] =
      decoderOf[String, Size[8]].map(Crc32(_))
  }

  case class MetaData(
      sha1: Sha1,
      sha256: Sha256,
      md5: Md5,
      crc32: Crc32,
      videoQuality: VideoQuality,
      identifiers: VideoIdentifier
  ) derives Codec.AsObject, Show

  abstract class MetaDataException(msg: String)    extends DownloaderException(msg)

  case class AssetIdNotFound(msg: String)          extends MetaDataException(msg) derives Eq, Show {
    override def getConsoleMsg = "Supplied Asset ID is not usable, try a different ID"
  }

  case class MetaDataHashMalformedException(msg: String)  extends MetaDataException(msg) derives Eq, Show {
    override def getConsoleMsg = "The hash data does not conform to the hash standard"
  }

  case class MetaDataNetworkException(msg: String) extends MetaDataException(msg) derives Eq, Show {
    override def getConsoleMsg = msg
  }
}
