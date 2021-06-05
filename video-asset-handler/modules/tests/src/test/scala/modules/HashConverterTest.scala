package modules

import cats.effect.{IO, Resource}
import modules.HashConverter.Hashes
import weaver.SimpleIOSuite

import java.io.{BufferedInputStream, ByteArrayInputStream}

object HashConverterTest extends SimpleIOSuite {

  test("Should yield the right hash set") {
    val text   = "abcdefg_string"
    val sha1   = "72900d8bb6c7f8156ff70cd53b5c600cd1e80708"
    val sha256 = "078c67d095ad4643b45c67630dc1b78b2a1cb5f79bfb4b2f4a1f8ca1a0d2a257"
    val md5    = "003e6c84ef4a5cdc271aed02440dc1be"
    val crc32  = "297f3723"
    val resource = createResource(text)

    for {
      Hashes(sha1T, sha256T, md5T, crc32T) <- resource.use(HashConverter.convert[IO])
      s1       = expect.same(sha1T, sha1)
      s256     = expect.same(sha256T, sha256)
      m5       = expect.same(md5T, md5)
      c        = expect.same(crc32T, crc32)
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
      Hashes(sha1T, sha256T, md5T, crc32T) <- resource.use(HashConverter.convert[IO])
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
