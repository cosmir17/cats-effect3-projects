import cats.effect.{ExitCode, IO}
import domain.video.VideoCorrupted
import org.apache.commons.io.FileUtils
import org.typelevel.log4cats.slf4j.Slf4jLogger
import weaver.SimpleIOSuite
import weaver.specs2compat.IOMatchers

import java.io.File

object ProduceThumbnailSpec extends SimpleIOSuite with IOMatchers {

  override def maxParallelism = 1
  implicit val logger = Slf4jLogger.getLogger[IO]

  test("should produce a thumbnail of a video file") {
    val videoFile  =  new java.io.File("src/it/resources/server_path/__files/rabbit.mov")
    val copiedFile  =  new java.io.File("test_video_file.mov")

    for {
      _       <- IO(copiedFile.delete())
      _       <- IO(new java.io.File("test_thumbnail.png").delete())
      _       <- IO(FileUtils.copyFile(videoFile, copiedFile)) //temporarily coping a video file for thumbnail production
      result  <- FMain.produceThumbnail()
      _       =  expect(result == ExitCode.Success)
      files   <- IO(new File(".").listFiles.filter(_.isFile).filter(_.getName == "test_thumbnail.png").toList)
      created <- IO(expect(files.nonEmpty))
      _       <- IO(copiedFile.delete())
      _       <- IO(new java.io.File("test_thumbnail.png").delete())
    } yield created
  }

  test("should have a video file produced before running Thumbnailer. " +
    "FMain.downloadAsset method should run first and prepare a video file") {
    val videoFile  =  new java.io.File("src/it/resources/server_path/__files/rabbit.mov")
    val copiedFile  =  new java.io.File("test_video_file.mov")

    for {
      _       <- IO(copiedFile.delete())
      _       <- IO(new java.io.File("test_thumbnail.png").delete())
      result  <- FMain.produceThumbnail().handleErrorWith {
        case VideoCorrupted("the video is invalid, the program exits") => IO(ExitCode.Success);
        case e => IO(failure("not an expected exception: " + e.toString))
      }
      _       =  expect(result == ExitCode.Error)
      files   <- IO(new File(".").listFiles.filter(_.isFile).filter(_.getName == "test_thumbnail.png").toList)
      created <- IO(expect(files.isEmpty))
      _       <- IO(copiedFile.delete())
    } yield created
  }
}