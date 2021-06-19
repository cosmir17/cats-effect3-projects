import cats.effect.{ExitCode, IO}
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import domain.video.VideoCorrupted
import org.typelevel.log4cats.testing.TestingLogger
import org.typelevel.log4cats.testing.TestingLogger._
import weaver.SimpleIOSuite
import weaver.specs2compat.IOMatchers

import java.io.File

object DownloadAssetSpec extends SimpleIOSuite with IOMatchers {
  val Port = 8080
  val Host = "localhost"

  override def maxParallelism = 1

  val wireMockServer = new WireMockServer(wireMockConfig()
    .usingFilesUnderClasspath("src/it/resources/server_path/")
    .port(Port)
  )

  test("downloadAsset should pass for valid asset-id") {
    implicit val logger = TestingLogger.impl()

    val wm = IO(wireMockServer.start()) *> IO(WireMock.configureFor(Host, Port))
    val path = "/playground/"
    val assetId = "valid"

    val stubOne = IO(stubFor(get(urlEqualTo(path + assetId))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBodyFile("rabbit.mov"))))

    val stubTwo = IO(stubFor(get(urlEqualTo(path + assetId + "/metadata"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json;charset=UTF-8")
          .withBodyFile("json/rabbit-metadata.json"))))

    for {
      _       <- IO(new File("test_video_file.mov").delete())
      _       <- wm
      _       <- stubOne
      _       <- stubTwo
      result  <- FMain.downloadAsset("valid")
      rTest   =  expect(result == ExitCode.Success)
      file    <- IO(new File(".").listFiles.filter(_.isFile).filter(_.getName == "test_video_file.mov").toList)
      created <- IO(expect(file.nonEmpty))
      _       <- IO(new File("test_video_file.mov").delete())
      _       <- IO(wireMockServer.stop())
    } yield rTest and created
  }

  test("downloadAsset should not pass when a hash validation fails for crc32 and md5") {
    implicit val logger = TestingLogger.impl()

    val wm = IO(wireMockServer.start()) *> IO(WireMock.configureFor(Host, Port))
    val path = "/playground/"
    val assetId = "valid"

    val stubOne = IO(stubFor(get(urlEqualTo(path + assetId))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBodyFile("rabbit.mov"))))

    val stubTwo = IO(stubFor(get(urlEqualTo(path + assetId + "/metadata"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json;charset=UTF-8")
          .withBodyFile("json/rabbit-metadata-wrong-crc32-md5.json"))))

    for {
      _       <- IO(new File("test_video_file.mov").delete())
      _       <- wm
      _       <- stubOne
      _       <- stubTwo
      result  <- FMain.downloadAsset("valid")
      rTest   =  expect(result == ExitCode.Error)
      file    <- IO(new File(".").listFiles.filter(_.isFile).filter(_.getName == "test_video_file.mov").toList)
      created =  expect(file.isEmpty)
      _       <- IO(new File("test_video_file.mov").delete())
      _       <- IO(wireMockServer.stop())
    } yield rTest and created
  }

  test("should make http requests in parallel") {
    implicit val logger = TestingLogger.impl()

    val wm = IO(wireMockServer.start()) *> IO(WireMock.configureFor(Host, Port))
    val path = "/playground/"
    val assetId = "valid"

    val stubOne = IO(stubFor(get(urlEqualTo(path + assetId))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBodyFile("rabbit.mov")
          .withFixedDelay(800)
      )))

    val stubTwo = IO(stubFor(get(urlEqualTo(path + assetId + "/metadata"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json;charset=UTF-8")
          .withBodyFile("json/rabbit-metadata.json")
          .withFixedDelay(800)
      )))

    for {
      _           <- IO(new File("test_video_file.mov").delete())
      _           <- wm
      _           <- stubOne
      _           <- stubTwo
      initial     <- IO.realTime
      result      <- FMain.downloadAsset("valid")
      end         <- IO.realTime
      processTime =  end.minus(initial).toMillis
      ptTest      =  expect(processTime < 1500L)
      rTest       =  expect(result == ExitCode.Success)
      file        <- IO(new File(".").listFiles.filter(_.isFile).filter(_.getName == "test_video_file.mov").toList)
      created     =  expect(file.nonEmpty)
      _           <- IO(new File("test_video_file.mov").delete())
      _           <- IO(wireMockServer.stop())
    } yield ptTest and rTest and created
  }

  test("downloadAsset should not pass for invalid asset-id") {
    implicit val logger = TestingLogger.impl()

    val wm = IO(wireMockServer.start()) *> IO(WireMock.configureFor(Host, Port))
    val path = "/playground/"
    val assetId = "invalid"

    val stubOne = IO(stubFor(get(urlEqualTo(path + assetId))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBodyFile("rabbit.mov"))))

    val stubTwo = IO(stubFor(get(urlEqualTo(path + assetId + "/metadata"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json;charset=UTF-8")
          .withBodyFile("json/random-metadata.json"))))

    for {
      _            <- IO(new File("test_video_file.mov").delete())
      _            <- wm
      _            <- stubOne
      _            <- stubTwo
      result       <- FMain.downloadAsset("invalid")
      logs         <- logger.logged
      _            =  println(logs.mkString(","))
      sending      =  logs.exists { case INFO(a, None) => a.contains("Sending a download request to the"); case _ => false }
      decodingErMsg=  logs.exists { case ERROR(a, None) => a.contains("Invalid message body: Could not decode JSON"); case _ => false }
      contained2   =  logs.exists { case ERROR(a, None) => a.contains("It's not a valid video, Hash validation failed, the video's is invalid as it's hash doesn't match to the sha1 hash in the metadata response"); case _ => false }
      contained3   =  logs.exists { case ERROR(a, None) => a.contains("Exiting the program"); case _ => false }
      logTest      =  expect(sending & !decodingErMsg & contained2 & contained3)
      rTest        =  expect(result == ExitCode.Error)
      file         <- IO(new File(".").listFiles.filter(_.isFile).filter(_.getName == "test_video_file.mov").toList)
      notCreated   =  expect(file.isEmpty)
      _            <- IO(new File("test_video_file.mov").delete())
      _            <- IO(wireMockServer.stop())
    } yield logTest and rTest and notCreated
  }

  test("downloadAsset should not pass for an asset-id without a valid video file in the server") {
    implicit val logger = TestingLogger.impl()

    val wm = IO(wireMockServer.start()) *> IO(WireMock.configureFor(Host, Port))
    val path = "/playground/"
    val assetId = "valid"

    val stubOne = IO(stubFor(get(urlEqualTo(path + assetId))
      .willReturn(aResponse().withStatus(200))))

    val stubTwo = IO(stubFor(get(urlEqualTo(path + assetId + "/metadata"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json;charset=UTF-8")
          .withBodyFile("json/rabbit-metadata.json"))))

    for {
      _           <- IO(new File("test_video_file.mov").delete())
      _           <- wm
      _           <- stubOne
      _           <- stubTwo
      result      <- FMain.downloadAsset("invalid").handleErrorWith {
        case VideoCorrupted("the video is invalid, the program exits") => IO(ExitCode.Success);
        case e => IO(failure("not an expected exception: " + e.toString))
      }
      rTest       =  expect(result == ExitCode.Error)
      file        <- IO(new File(".").listFiles.filter(_.isFile).filter(_.getName == "test_video_file.mov").toList)
      notCreated  =  expect(file.isEmpty)
      _           <- IO(new File("test_video_file.mov").delete())
      _           <- IO(wireMockServer.stop())
    } yield rTest and notCreated
  }

  test("downloadAsset should not pass for an empty asset-id") {
    implicit val logger = TestingLogger.impl()

    val wm = IO(wireMockServer.start()) *> IO(WireMock.configureFor(Host, Port))
    val path = "/playground/"
    val assetId = ""

    val stubOne = IO(stubFor(get(urlEqualTo(path + assetId))
      .willReturn(aResponse()
        .withStatus(404)
      )))

    val stubTwo = IO(stubFor(get(urlEqualTo(path + assetId + "/metadata"))
      .willReturn(
        aResponse()
          .withStatus(404)
          .withHeader("Content-Type", "application/json;charset=UTF-8")
          .withBody("The requested resource was not found.")
      )))

    for {
      _           <- IO(new File("test_video_file.mov").delete())
      _           <- wm
      _           <- stubOne
      _           <- stubTwo
      result      <- FMain.downloadAsset("").handleErrorWith {
        case e: IllegalArgumentException if e.getMessage == "Asset ID can't be empty" => IO(success)
        case e => IO(failure("not an expected exception: " + e.toString))
      }
      rTest       =  expect(result == ExitCode.Error)
      file        <- IO(new File(".").listFiles.filter(_.isFile).filter(_.getName == "test_video_file.mov").toList)
      notCreated  =  expect(file.isEmpty)
      _           <- IO(new File("test_video_file.mov").delete())
      _           <- IO(wireMockServer.stop())
    } yield rTest and notCreated
  }


}