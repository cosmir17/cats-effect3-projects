import cats.effect.{ExitCode, IO}
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import domain.video.VideoCorrupted
import org.typelevel.log4cats.slf4j.Slf4jLogger
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

  implicit val logger = Slf4jLogger.getLogger[IO]

  test("downloadAsset should pass for valid asset-id") {
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
      _       =  expect(result == ExitCode.Success).failFast
      file    <- IO(new File(".").listFiles.filter(_.isFile).filter(_.getName == "test_video_file.mov").toList)
      created <- IO(expect(file.nonEmpty))
      _       <- IO(new File("test_video_file.mov").delete())
      _       <- IO(wireMockServer.stop())
    } yield created
  }

  test("should make http requests in parallel") {
    val wm = IO(wireMockServer.start()) *> IO(WireMock.configureFor(Host, Port))
    val path = "/playground/"
    val assetId = "valid"

    val stubOne = IO(stubFor(get(urlEqualTo(path + assetId))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBodyFile("rabbit.mov")
          .withFixedDelay(300)
      )))

    val stubTwo = IO(stubFor(get(urlEqualTo(path + assetId + "/metadata"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json;charset=UTF-8")
          .withBodyFile("json/rabbit-metadata.json")
          .withFixedDelay(300)
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
      _           <- expect(processTime < 450L).failFast
      _           =  expect(result == ExitCode.Success).failFast
      file        <- IO(new File(".").listFiles.filter(_.isFile).filter(_.getName == "test_video_file.mov").toList)
      created     <- IO(expect(file.nonEmpty))
      _           <- IO(new File("test_video_file.mov").delete())
      _           <- IO(wireMockServer.stop())
    } yield created
  }

  test("downloadAsset should not pass for invalid asset-id") {
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
      result       <- FMain.downloadAsset("invalid").handleErrorWith {
        case VideoCorrupted("the video is invalid, the program exits") => IO(ExitCode.Success);
        case e => IO(failure("not an expected exception: " + e.toString))
      }
      _            =  expect(result == ExitCode.Error).failFast
      file         <- IO(new File(".").listFiles.filter(_.isFile).filter(_.getName == "test_video_file.mov").toList)
      notCreated   <- IO(expect(file.isEmpty))
      _            <- IO(new File("test_video_file.mov").delete())
      _            <- IO(wireMockServer.stop())
    } yield notCreated
  }

  test("downloadAsset should not pass for an asset-id without a valid video file in the server") {
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
      _           =  expect(result == ExitCode.Error).failFast
      file        <- IO(new File(".").listFiles.filter(_.isFile).filter(_.getName == "test_video_file.mov").toList)
      notCreated  <- IO(expect(file.isEmpty))
      _           <- IO(new File("test_video_file.mov").delete())
      _           <- IO(wireMockServer.stop())
    } yield notCreated
  }

  test("downloadAsset should not pass for an empty id asset-id") {
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
      _           =  expect(result == ExitCode.Error).failFast
      file        <- IO(new File(".").listFiles.filter(_.isFile).filter(_.getName == "test_video_file.mov").toList)
      notCreated  <- IO(expect(file.isEmpty))
      _           <- IO(new File("test_video_file.mov").delete())
      _           <- IO(wireMockServer.stop())
    } yield notCreated
  }


}