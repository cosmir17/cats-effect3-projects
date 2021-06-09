package modules

import cats.effect._
import domain.generators._
import domain.metadata._
import domain.video.VideoCorrupted
import modules.HashHandler.Hashes
import org.scalacheck.Gen
import org.typelevel.log4cats.testing.TestingLogger
import org.typelevel.log4cats.testing.TestingLogger._
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object HashHandlerCompareTest extends SimpleIOSuite with Checkers with HashesTestTrait {

  test("Should pass for valid hash datasets") {
    implicit val logger = TestingLogger.impl()
    val hashHandler = HashHandler.make[IO]()

    forall(metaHashPairGen) {
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
    implicit val logger = TestingLogger.impl()
    val hashHandler = HashHandler.make[IO]()

    val differentSeedGen: Gen[(MetaData, Hashes)] = for {
      meta <- metaDataGen
      hash <- normalHashesGen
    } yield (meta, hash)

    forall(differentSeedGen) {
      case (meta, hash) =>
        for {
          expectedException            <- hashHandler.compare(hash, meta).attempt.map {
              case Left(e)  => expect.same(VideoCorrupted("the video's is invalid as it's hash doesn't match to the sha1 hash in the metadata response"), e)
              case Right(_) => failure("expected payment error")
            }
          logs         <- logger.logged
          contained1   = logs.contains(INFO("the video is valid according to sha1 hash validation", None))
          logTest      = expect(!contained1)
        } yield expectedException and logTest
    }
  }

}