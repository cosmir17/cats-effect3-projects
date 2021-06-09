package modules

import cats.effect._
import cats.syntax.all._
import derevo.cats.show
import domain.metadata.MetaData
import derevo.derive
import org.apache.commons.codec.digest.PureJavaCrc32
import org.typelevel.log4cats.Logger
import domain.video.VideoCorrupted
import modules.HashHandler.Hashes

import java.io.BufferedInputStream
import scodec.bits.ByteVector

object HashHandler {
  @derive(show)
  case class Hashes(sha1: String, sha256: String, md5: String, crc32: String)

  def make[F[_]: Async : Logger](): HashHandler[F] = new HashHandler[F]() {}
}

class HashHandler[F[_] : Async : Logger] {

  def convert(input: BufferedInputStream): F[Hashes] = for {
      byteArray   <- Async[F].delay(LazyList.continually(input.read).takeWhile(_ != -1).map(_.toByte).toArray)
      sha1T       =  ByteVector.view(byteArray).digest("SHA-1").toHex
      sha256T     =  ByteVector.view(byteArray).digest("SHA-256").toHex
      md5T        =  ByteVector.view(byteArray).digest("MD5").toHex
      crc32Hasher =  new PureJavaCrc32()
      _           =  crc32Hasher.update(byteArray, 0, byteArray.length)
      crc32T      =  crc32Hasher.getValue.toHexString
  } yield Hashes(sha1T, sha256T, md5T, crc32T)

  def compare(h: Hashes, metaData: MetaData): F[Unit] = for {
    _     <- if (h.sha1 == metaData.sha1.value.value) validCase("sha1") else invalidCase("sha1")
    _     <- if (h.sha256 == metaData.sha256.value.value) validCase("sha256") else invalidCase("sha256")
    _     <- if (h.md5 == metaData.md5.value.value) validCase("md5") else invalidCase("md5")
    _     <- if (h.crc32 == metaData.crc32.value.value) validCase("crc32") else invalidCase(s"crc32")
  } yield ()

  private def validCase(hash: String): F[Unit] =
    Logger[F].info(s"the video is valid according to $hash hash validation") *> Sync[F].unit

  private def invalidCase(hash: String): F[Unit] =
    Sync[F].raiseError(VideoCorrupted(s"the video's is invalid as it's hash doesn't match to " +
      s"the $hash hash in the metadata response")) *> Sync[F].unit
}
