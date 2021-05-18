import cats.effect._
import cats.effect.std.Console
import cats.implicits._
import com.monovore.decline._
import com.monovore.decline.effect._

case class Config(
  inputFile: String,
  outputDir: String,
  maxLines: Option[Int] = None,
  maxBytes: Option[Int] = None)

object Main extends CommandIOApp(
  name = "CSV File Handler",
  header = s"""
    |  #####                       ###
    | #     # ######   ##   #    # ###  ####
    | #       #       #  #  ##   #  #  #
    |  #####  #####  #    # # #  # #    ####
    |       # #      ###### #  # #          #
    | #     # #      #    # #   ##     #    #
    |  #####  ###### #    # #    #      ####
    |
    |  #####   #####  #     #    #######
    | #     # #     # #     #    #       # #      ######
    | #       #       #     #    #       # #      #
    | #        #####  #     #    #####   # #      #####
    | #             #  #   #     #       # #      #
    | #     # #     #   # #      #       # #      #
    |  #####   #####     #       #       # ###### ######
    |
    | #     #
    | #     #   ##   #    # #####  #      ###### #####
    | #     #  #  #  ##   # #    # #      #      #    #
    | ####### #    # # #  # #    # #      #####  #    #
    | #     # ###### #  # # #    # #      #      #####
    | #     # #    # #   ## #    # #      #      #   #
    | #     # #    # #    # #####  ###### ###### #    #
    |
    |
    |Sean's CSV File Handler Command Line Tool
    |""".stripMargin, version = "0.0.1") {

  val inputFile: Opts[String] =
    Opts.argument[String](metavar = "input-csv-file")

  val outputOpts: Opts[String] =
    Opts.argument[String](metavar = "outputDir")

  val maxLinesOpts: Opts[Option[Int]] =
    Opts.option[Int]("max-lines", "maxlines that one file can support", short = "ml").orNone

  val maxBytesOpts: Opts[Option[Int]] =
    Opts.option[Int]("max-bytes", "maxbytes that one file can support", short = "mb").orNone

  val parseOpts: Opts[IO[ExitCode]] =
    Opts.subcommand("parse", "divide a csv file into multiple files") {
      (inputFile, outputOpts, maxLinesOpts, maxBytesOpts).mapN{ case (i, o, ml, mb) => FMain.parse[IO](Config(i, o, ml, mb)) }
    }

  override def main: Opts[IO[ExitCode]] =
    parseOpts
}

object FMain {
  def parse[F[_]: Async : Console](config: Config): F[ExitCode] = {
    val reader = CSVReader.make[F]()
    val manipulator = TextManipulator.make[F]()
    val fileSaver = FileSaver.make[F]()

    for {
      _ <- Sync[F].delay(println())
      _ <- Sync[F].delay(println(s"Processing"))
      strLines <- reader.parseFile(config.inputFile)
      _ <- Sync[F].delay(println(s"Created a text group list"))
      textGroups <- manipulator.divide(strLines, config.maxLines, config.maxBytes)
      _ <- fileSaver.saveAsFiles(textGroups, config.outputDir)
      _ <- Sync[F].delay(println(s"Done, Program Exiting"))
    } yield ExitCode.Success
  }
}