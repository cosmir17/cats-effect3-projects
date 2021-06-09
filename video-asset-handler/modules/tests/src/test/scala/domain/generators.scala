package domain

import domain.metadata._
import org.scalacheck.Gen
import antimirov.Rx
import antimirov.check.Regex
import eu.timepit.refined.api.Refined
import modules.HashHandler.Hashes


object generators {
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

  val normalHashesGen: Gen[Hashes] =
    for {
      sha1          <- sha1Gen
      sha256        <- sha256Gen
      md5           <- md5Gen
      crc32         <- crc32
    } yield Hashes(sha1.value, sha256.value, md5.value, crc32.value)

  val metaHashPairGen: Gen[(MetaData, Hashes)] = for {
    sha1          <- sha1Gen
    sha256        <- sha256Gen
    md5           <- md5Gen
    crc32         <- crc32
    vq            <- videoQualityGen
    vi            <- videoIdGen
  } yield (MetaData(Sha1(sha1), Sha256(sha256), Md5(md5), Crc32(crc32), vq, vi), Hashes(sha1.value, sha256.value, md5.value, crc32.value))

}
