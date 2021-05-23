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
      _       <- IO(FileUtils.copyFile(videoFile, copiedFile))
      result  <- FMain.produceThumbnail()
      _       =  expect(result == ExitCode.Success)
      files   <- IO(new File(".").listFiles.filter(_.isFile).filter(_.getName == "test_thumbnail.png").toList)
      created <- IO(expect(files.nonEmpty))
//      _       <- IO(copiedFile.delete())
//      _       <- IO(new java.io.File("test_thumbnail.png").delete())
    } yield created
  }

//  test("downloadAsset should not pass for invalid asset-id") {
//    val wm = IO(wireMockServer.start()) *> IO(WireMock.configureFor(Host, Port))
//    val path = "/playground/"
//    val assetId = "invalid"
//
//
//    for {
//      _            <- IO(new File("test_video_file.mov").delete())
//      _            <- wm
//      _            <- stubOne
//      _            <- stubTwo
//      result       <- FMain.downloadAsset("invalid").handleErrorWith {
//        case VideoCorrupted("the video is invalid, the program exits") => IO(ExitCode.Success);
//        case e => IO(failure("not an expected exception: " + e.toString))
//      }
//      _            =  expect(result == ExitCode.Error)
//      file         <- IO(new File(".").listFiles.filter(_.isFile).filter(_.getName == "test_video_file.mov").toList)
//      notCreated   <- IO(expect(file.isEmpty))
//      _            <- IO(new File("test_video_file.mov").delete())
//      _            <- IO(wireMockServer.stop())
//    } yield notCreated
//  }
//
//  test("downloadAsset should not pass for an asset-id without a valid video file in the server") {
//    val wm = IO(wireMockServer.start()) *> IO(WireMock.configureFor(Host, Port))
//    val path = "/playground/"
//    val assetId = "valid"
//
//    val stubOne = IO(stubFor(get(urlEqualTo(path + assetId))
//      .willReturn(aResponse().withStatus(200))))
//
//    val stubTwo = IO(stubFor(get(urlEqualTo(path + assetId + "/metadata"))
//      .willReturn(
//        aResponse()
//          .withStatus(200)
//          .withHeader("Content-Type", "application/json;charset=UTF-8")
//          .withBodyFile("json/rabbit-metadata.json"))))
//
//    for {
//      _           <- IO(new File("test_video_file.mov").delete())
//      _           <- wm
//      _           <- stubOne
//      _           <- stubTwo
//      result      <- FMain.downloadAsset("invalid").handleErrorWith {
//        case VideoCorrupted("the video is invalid, the program exits") => IO(ExitCode.Success);
//        case e => IO(failure("not an expected exception: " + e.toString))
//      }
//      _           =  expect(result == ExitCode.Error)
//      file        <- IO(new File(".").listFiles.filter(_.isFile).filter(_.getName == "test_video_file.mov").toList)
//      notCreated  <- IO(expect(file.isEmpty))
//      _           <- IO(new File("test_video_file.mov").delete())
//      _           <- IO(wireMockServer.stop())
//    } yield notCreated
//  }
//
//  test("downloadAsset should not pass for an empty id asset-id") {
//    val wm = IO(wireMockServer.start()) *> IO(WireMock.configureFor(Host, Port))
//    val path = "/playground/"
//    val assetId = ""
//
//    val stubOne = IO(stubFor(get(urlEqualTo(path + assetId))
//      .willReturn(aResponse()
//        .withStatus(404)
//      )))
//
//    val stubTwo = IO(stubFor(get(urlEqualTo(path + assetId + "/metadata"))
//      .willReturn(
//        aResponse()
//          .withStatus(404)
//          .withHeader("Content-Type", "application/json;charset=UTF-8")
//          .withBody("The requested resource was not found.")
//      )))
//
//    for {
//      _           <- IO(new File("test_video_file.mov").delete())
//      _           <- wm
//      _           <- stubOne
//      _           <- stubTwo
//      result      <- FMain.downloadAsset("").handleErrorWith {
//        case e: IllegalArgumentException if e.getMessage == "Asset ID can't be empty" => IO(success)
//        case e => IO(failure("not an expected exception: " + e.toString))
//      }
//      _           =  expect(result == ExitCode.Error)
//      file        <- IO(new File(".").listFiles.filter(_.isFile).filter(_.getName == "test_video_file.mov").toList)
//      notCreated  <- IO(expect(file.isEmpty))
//      _           <- IO(new File("test_video_file.mov").delete())
//      _           <- IO(wireMockServer.stop())
//    } yield notCreated
//  }
}