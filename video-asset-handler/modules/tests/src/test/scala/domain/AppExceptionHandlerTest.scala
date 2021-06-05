package domain

import cats.effect.{ExitCode, IO}
import domain.video.{AssetIdNotFound, VideoCheckerException}
import weaver.SimpleIOSuite
import AppExceptionHandler._
import domain.downloader.DownloaderException
import domain.metadata.{MetaDataException, MetaDataNetworkException}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.testing.TestingLogger
import org.typelevel.log4cats.testing.TestingLogger._

object AppExceptionHandlerTest extends SimpleIOSuite {

  test("Should handle DownloaderException") {
    implicit val logger = TestingLogger.impl()

    val exceptionIO = IO(Logger[IO].info("Some side effect")) *>
      IO.raiseError(AssetIdNotFound("Asset ID not found")) *>
      IO(ExitCode.Success)

    for {
      exception   <- exceptionIO.handleAppError[DownloaderException]()
      logs        <- logger.logged
      contained   = logs.contains(ERROR("Supplied Asset ID is not usable, try a different ID", None))
      contained2  = logs.contains(ERROR("Exiting the program", None))
      logTest     = expect(contained & contained2)
      finalEC     = expect(exception == ExitCode.Error)
    } yield logTest and finalEC
  }

  test("Should handle MetaDataException") {
    implicit val logger = TestingLogger.impl()

    val exceptionIO = IO(Logger[IO].info("Some side effect")) *>
      IO.raiseError(MetaDataNetworkException("Asset ID not found")) *>
      IO(ExitCode.Success)

    for {
      exception   <- exceptionIO.handleAppError[MetaDataException]()
      logs        <- logger.logged
      contained   = logs.contains(ERROR("Asset ID not found", None))
      contained2  = logs.contains(ERROR("Exiting the program", None))
      logTest     = expect(contained & contained2)
      finalEC     = expect(exception == ExitCode.Error)
    } yield logTest and finalEC
  }

  test("Should not do anything if there is no exception") {
    implicit val logger = TestingLogger.impl()

    val normalEffect = IO(Logger[IO].info("Some side effect")) *> IO(ExitCode.Success)

    for {
      exception   <- normalEffect.handleAppError[VideoCheckerException]()
      logs        <- logger.logged
      contained   = logs.contains(ERROR("Supplied Asset ID is not usable, try a different ID", None))
      contained2  = logs.contains(ERROR("Exiting the program", None))
      logTest     = expect(!(contained & contained2))
      finalEC     = expect(exception == ExitCode.Success)
    } yield logTest and finalEC
  }

}