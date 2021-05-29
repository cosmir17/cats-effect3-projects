package modules

import cats.effect.Async
import cats.syntax.all._
import org.apache.commons.codec.digest.PureJavaCrc32

import java.io.BufferedInputStream
import scodec.bits.ByteVector

object HashConverter {
  case class Hashes(sha1: String, sha256: String, md5: String, crc32: String)

  def convert[F[_] : Async](input: BufferedInputStream): F[Hashes] = for {
      byteArray   <- Async[F].delay(LazyList.continually(input.read).takeWhile(_ != -1).map(_.toByte).toArray)
      sha1T       =  ByteVector.view(byteArray).digest("SHA-1").toHex
      sha256T     =  ByteVector.view(byteArray).digest("SHA-256").toHex
      md5T        =  ByteVector.view(byteArray).digest("MD5").toHex
      crc32Hasher =  new PureJavaCrc32()
      _           =  crc32Hasher.update(byteArray, 0, byteArray.length)
      crc32T      =  crc32Hasher.getValue.toHexString
    } yield Hashes(sha1T, sha256T, md5T, crc32T)
}
