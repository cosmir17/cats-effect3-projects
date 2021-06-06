import cats.effect.{ExitCode, IO}
import domain.video.VideoCorrupted
import org.apache.commons.io.FileUtils
import org.typelevel.log4cats.testing.TestingLogger
import org.typelevel.log4cats.testing.TestingLogger._
import weaver.SimpleIOSuite
import weaver.specs2compat.IOMatchers

import java.io.File

object ProduceThumbnailSpec extends SimpleIOSuite with IOMatchers {

  override def maxParallelism = 1

  test("should produce a thumbnail of a video file") {
    implicit val logger = TestingLogger.impl()
    val videoFile  =  new java.io.File("src/it/resources/server_path/__files/rabbit.mov")
    val copiedFile  =  new java.io.File("test_video_file.mov")

    for {
      _             <- IO(copiedFile.delete())
      _             <- IO(new java.io.File("test_thumbnail.png").delete())
      _             <- IO(FileUtils.copyFile(videoFile, copiedFile)) //temporarily coping a video file for thumbnail production
      result        <- FMain.produceThumbnail()
      logs          <- logger.logged
      contained1    =  logs.contains(INFO("Listing video files", None))
      contained2    =  logs.contains(INFO("test_video_file.mov", None))
      contained3    =  logs.contains(INFO("provide a name of your thumbnail file without a format keyword, will be stored as png format", None))
      contained4    =  logs.contains(INFO("A thumbnail is produced, exiting program", None))
      logTest       =  expect(contained1 & contained2 & contained3 & contained4)
      rTest         =  expect(result == ExitCode.Success)
      files         <- IO(new File(".").listFiles.filter(_.isFile).filter(_.getName == "test_thumbnail.png").toList)
      created       =  expect(files.nonEmpty)
      _             <- IO(copiedFile.delete())
      _             <- IO(new java.io.File("test_thumbnail.png").delete())
    } yield logTest and rTest and created
  }

  test("should have a video file produced before running Thumbnailer. " +
    "FMain.downloadAsset method should run first and prepare a video file") {
    implicit val logger = TestingLogger.impl()
    val copiedFile  =  new java.io.File("test_video_file.mov")

    for {
      _             <- IO(copiedFile.delete())
      _             <- IO(new java.io.File("test_thumbnail.png").delete())
      result        <- FMain.produceThumbnail().handleErrorWith {
        case VideoCorrupted("the video is invalid, the program exits") => IO(ExitCode.Success);
        case e => IO(failure("not an expected exception: " + e.toString))
      }
      logs          <- logger.logged
      contained1    =  logs.contains(INFO("Starting thumbnail producer..", None))
      contained2    =  logs.contains(ERROR("No video file is present, please produce a video first", None))
      contained3    =  logs.contains(ERROR("Exiting the program", None))
      logTest       =  expect(contained1 & contained2 & contained3)
      rTest         =  expect(result == ExitCode.Error)
      files         <- IO(new File(".").listFiles.filter(_.isFile).filter(_.getName == "test_thumbnail.png").toList)
      created       =  expect(files.isEmpty)
      _             <- IO(copiedFile.delete())
    } yield logTest and rTest and created
  }
}