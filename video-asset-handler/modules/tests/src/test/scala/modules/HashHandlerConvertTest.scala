package modules

import cats.effect.{IO, Resource}
import modules.HashHandler.Hashes
import weaver.SimpleIOSuite
import org.typelevel.log4cats.slf4j.Slf4jLogger
import weaver.scalacheck.Checkers

import java.io.{BufferedInputStream, ByteArrayInputStream}

object HashHandlerConvertTest extends SimpleIOSuite with Checkers with HashesTestTrait {
  implicit val logger = Slf4jLogger.getLogger[IO]
  private val hashHandler = HashHandler.make[IO]()

  test("Should yield the right hash set") {
    val resource = createResource(normalText)

    for {
      Hashes(sha1T, sha256T, md5T, crc32T) <- resource.use(hashHandler.convert)
      s1 = expect.same(sha1T, normalSha1)
      s256 = expect.same(sha256T, normalSha256)
      m5 = expect.same(md5T, normalMd5)
      c = expect.same(crc32T, normalCrc32)
    } yield s1 and s256 and m5 and c
  }

  test("Should yield empty strings for an empty input") {
    val text   = ""
    val sha1   = "da39a3ee5e6b4b0d3255bfef95601890afd80709"
    val sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    val md5    = "d41d8cd98f00b204e9800998ecf8427e"
    val crc32  = "0"
    val resource = createResource(text)

    for {
      Hashes(sha1T, sha256T, md5T, crc32T) <- resource.use(hashHandler.convert)
      s1          = expect.same(sha1T, sha1)
      s256        = expect.same(sha256T, sha256)
      m5          = expect.same(md5T, md5)
      c           = expect.same(crc32T, crc32)
    } yield  s1 and s256 and m5 and c
  }

  test("Should yield empty strings for an empty input") {
    val text   = ""
    val sha1   = "da39a3ee5e6b4b0d3255bfef95601890afd80709"
    val sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    val md5    = "d41d8cd98f00b204e9800998ecf8427e"
    val crc32  = "0"
    val resource = createResource(text)

    for {
      Hashes(sha1T, sha256T, md5T, crc32T) <- resource.use(hashHandler.convert)
      s1          = expect.same(sha1T, sha1)
      s256        = expect.same(sha256T, sha256)
      m5          = expect.same(md5T, md5)
      c           = expect.same(crc32T, crc32)
    } yield  s1 and s256 and m5 and c
  }

  private def createResource(text: String) =
    Resource.make {
      IO.blocking(new BufferedInputStream(new ByteArrayInputStream(
        text.getBytes(java.nio.charset.StandardCharsets.UTF_8.name)
      )))                                                           // build
    } { inStream =>
      IO.blocking(inStream.close()).handleErrorWith(_ => IO.unit)   // release
    }
}
