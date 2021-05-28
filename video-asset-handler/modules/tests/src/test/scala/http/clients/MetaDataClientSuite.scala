package http.clients

import cats.effect.IO
import config.data._
import domain.metadata._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.{HttpRoutes, Response}
import org.scalacheck.Gen
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers
import antimirov.Rx
import antimirov.check.Regex

object MetaDataClientSuite extends SimpleIOSuite with Checkers {

  val config = VideoUrlConfig(VideoCompanyUrl("http://localhost"))

  def routes(mkResponse: IO[Response[IO]]) =
    HttpRoutes.of[IO] { case GET -> Root / _ / "metadata" => mkResponse }
      .orNotFound

  def nonEmptyStringGen(min: Int, max: Int): Gen[String] =
    Gen.chooseNum(min, max).flatMap { n => Gen.buildableOfN[String, Char](n, Gen.alphaChar) }

  def nesGen[A](f: String => A, min: Int, max: Int): Gen[A] = nonEmptyStringGen(min, max).map(f)
  val durationRegex = "[0-9]+:[0-9]{2}:[0-9]{2}" //the library doesn't support the escape character

  val frameRateGen: Gen[FrameRate] = nesGen(FrameRate.apply, 5, 5)
  val resolutionGen: Gen[Resolution] = nesGen(Resolution.apply, 8, 8)
  val dynamicRangeGen: Gen[DynamicRange] = nesGen(DynamicRange.apply, 3, 3)
  val productionIdGen: Gen[ProductionId] = nesGen(ProductionId.apply, 10, 10)
  val titleGen: Gen[Title] = nesGen(Title.apply, 10, 40)
  val durationGen: Gen[DurationPred] = Regex.gen(Rx.parse(durationRegex)).map[DurationPred](Refined.unsafeApply)
  val sha1Gen: Gen[Sha1Pred] = nonEmptyStringGen(40,40).map[Sha1Pred](Refined.unsafeApply)
  val sha256Gen: Gen[Sha256Pred] = nonEmptyStringGen(64, 64).map[Sha256Pred](Refined.unsafeApply)
  val md5Gen: Gen[Md5Pred] = nonEmptyStringGen(32, 32).map[Md5Pred](Refined.unsafeApply)
  val crc32: Gen[Crc32Pred] = nonEmptyStringGen(8, 8).map[Crc32Pred](Refined.unsafeApply)

  val videoQualityGen: Gen[VideoQuality] =
    for {
      frameRate     <- frameRateGen
      resolution    <- resolutionGen
      dynamicRange  <- dynamicRangeGen
    } yield VideoQuality(frameRate, resolution, dynamicRange)

  val videoIdGen: Gen[VideoIdentifier] =
    for {
      productionId  <- productionIdGen
      title         <- titleGen
      duration      <- durationGen
    } yield VideoIdentifier(productionId, title, Duration(duration))


  val metaDataGen: Gen[MetaData] =
    for {
      sha1          <- sha1Gen
      sha256        <- sha256Gen
      md5           <- md5Gen
      crc32         <- crc32
      vq            <- videoQualityGen
      vi            <- videoIdGen
    } yield MetaData(Sha1(sha1), Sha256(sha256), Md5(md5), Crc32(crc32), vq, vi)

  val gen = for {
    assetIdGen      <- nonEmptyStringGen(5, 20)
    metaData        <- metaDataGen
  } yield assetIdGen -> metaData

  test("Response Ok (200)") {
    forall(gen) {
      case (assetId, metaData) =>
        val client = Client.fromHttpApp(routes(Ok(metaData)))

        MetaDataClient
          .make[IO](config, client)
          .query(assetId)
          .map(expect.same(metaData, _))
    }
  }

  test("Response Not Found (404)") {
    forall(gen) {
      case (assetId, _) =>
        val client = Client.fromHttpApp(routes(NotFound()))

        MetaDataClient
          .make[IO](config, client)
          .query(assetId)
          .attempt
          .map {
            case Left(e)  => expect.same(AssetIdNotFound("Not Found"), e)
            case Right(_) => failure("unexpected error")
          }
    }
  }

  test("Empty asset ID should yield Asset ID not found exception") {
    val messageFromEndpoint = """{
                                |    "error": "VideoAssetNotFound",
                                |    "description": "The video of the provided asset `metadata` could not be found"
                                |}""".stripMargin

    forall(nonEmptyStringGen(0, 0)) { assetId =>
      val client = Client.fromHttpApp(routes(NotFound(messageFromEndpoint)))

      MetaDataClient
        .make[IO](config, client)
        .query(assetId)
        .attempt
        .map {
          case Left(e)  => expect.same(AssetIdNotFound("Not Found"), e)
          case Right(_) => failure("expected metadata client error")
        }
    }
  }

  test("Internal Server Error response (500)") {
    forall(nonEmptyStringGen(5, 10)) { assetId =>
      val client = Client.fromHttpApp(routes(InternalServerError()))

      MetaDataClient
        .make[IO](config, client)
        .query(assetId)
        .attempt
        .map {
          case Left(e)  => expect.same(MetaDataNetworkException("Internal Server Error"), e)
          case Right(_) => failure("expected metadata client error")
        }
    }
  }

}
