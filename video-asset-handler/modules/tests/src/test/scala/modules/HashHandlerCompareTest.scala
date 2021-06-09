package modules

import cats.effect._
import domain.generators._
import domain.metadata.MetaData
import modules.HashHandler.Hashes
import org.scalacheck.Gen
import org.typelevel.log4cats.testing.TestingLogger
import org.typelevel.log4cats.testing.TestingLogger._
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object HashHandlerCompareTest extends SimpleIOSuite with Checkers with HashesTestTrait {
  implicit val logger = TestingLogger.impl()
  private val hashHandler = HashHandler.make[IO]()

  val gen: Gen[(MetaData, Hashes)] = for {
    meta <- metaDataGen
    hash <- normalHashesGen
  } yield (meta, hash)

  test("Should pass for valid hash datasets") {
    forall(gen) {
      case (meta, hash) =>
      for {
        _            <- hashHandler.compare(hash, meta)
        logs         <- logger.logged
        contained1   = logs.contains(INFO("the video is valid according to sha1 hash validation", None))
        contained2   = logs.contains(INFO("the video is valid according to sha256 hash validation", None))
        contained3   = logs.contains(INFO("the video is valid according to md5 hash validation", None))
        contained4   = logs.contains(INFO("the video is valid according to crc32 hash validation", None))
        logTest      = expect(contained1 & contained2 & contained3 & contained4)
      } yield logTest
    }
  }

  test("Should print invalid message and throw an exception if the hashes do not match with metadata") {
    forall(gen) {
      case (meta, hash) =>
        for {
          _            <- hashHandler.compare(hash, meta)
          logs         <- logger.logged
          contained1   = logs.contains(ERROR("the video's is invalid as it's hash doesn't match to the sha1 hash in the metadata response", None))
          contained2   = logs.contains(ERROR("the video's is invalid as it's hash doesn't match to the sha256 hash in the metadata response", None))
          contained3   = logs.contains(ERROR("the video's is invalid as it's hash doesn't match to the md5 hash in the metadata response", None))
          contained4   = logs.contains(ERROR("the video's is invalid as it's hash doesn't match to the crc32 hash in the metadata response", None))
          logTest      = expect(contained1 & contained2 & contained3 & contained4)
        } yield logTest
    }
  }

}