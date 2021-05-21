import cats.effect.{Resource, _}
import cats.effect.std.Console
import cats.syntax.all._

import java.io.{File, FileOutputStream}
import java.nio.file.{Files, Paths}
import scala.language.postfixOps

object FileSaver {
  def make[F[_] : Sync : Console](): FileSaver[F] =
    new FileSaver[F]() {}
}

class FileSaver[F[_] : Sync : Console] private() {

  def saveAsFiles(textGroupsR: Resource[F, List[Iterator[String]]], outputDir: String): F[Unit] =
    for {
      validOutputDir   <- pathShouldNotStartWithSlash(outputDir)
      _                <- Sync[F].delay(println(s"Valid output path : $validOutputDir"))
      currentLocation  <- Sync[F].delay(new java.io.File(".").getCanonicalPath)
      folderPath       <- Sync[F].delay(Paths.get(currentLocation + "/" + validOutputDir))
      _                <- if(Files.exists(folderPath)) Sync[F].delay(println(s"$validOutputDir already exists and not creating one, " +
                                                                             s"please check the folder if csv files exists inside"))
                          else Sync[F].delay(Files.createDirectory(folderPath)) *> Sync[F].delay(println(s"Folder created : $validOutputDir"))
      _                <- Sync[F].delay(println(s"Please enter a prefixed file name you desire"))
      prefixedFileName <- Console.make.readLine
      lineSeparator    <- Sync[F].delay(System.getProperty("line.separator"))

      _                <- textGroupsR.use(tg => {
        val fileStrings = tg.indices.map(i => validOutputDir + "/" + prefixedFileName + "_" + i + "_.csv").toList
        val files = fileStrings.map(name => new File(name))
        val outputFileR = files.traverse(b => textGroupsToResource(b))
        //ToDo partraverse

        outputFileR.use(_.zipWithIndex.traverse(pair => {
          val listF = tg(pair._2).map(textLine =>
            Sync[F].delay(pair._1.write(textLine.getBytes("UTF-8"))) *>
            Sync[F].delay(pair._1.write(lineSeparator.getBytes("UTF-8")))
          ).toList
          listF.traverse(_ *> Sync[F].unit) *> Sync[F].delay(println(fileStrings(pair._2) + " file created"))
        }))
      })
    } yield ()

  def textGroupsToResource(f: File): Resource[F, FileOutputStream] =
    Resource.make {
      Sync[F].blocking(new FileOutputStream(f))                               // build
    } { outStream =>
      Sync[F].blocking(outStream.close()).handleErrorWith(_ => Sync[F].unit)  // release
    }

  private def pathShouldNotStartWithSlash(outputDir: String): F[String] = for {
    validOutputPath <- if (outputDir.startsWith("/") || outputDir.startsWith("\\"))
      Sync[F].delay(println(s"OutputDir path shouldn't interfere with root directory," +
        s"please enter a path not starting with /"))
        .flatMap(_ => Console.make.readLine)
        .flatMap(name => pathShouldNotStartWithSlash(name))
    else Sync[F].delay(outputDir)
  } yield validOutputPath
}
