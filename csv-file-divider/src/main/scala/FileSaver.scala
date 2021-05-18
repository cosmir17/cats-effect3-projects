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

  def saveAsFiles(textGroups: List[Seq[String]], outputDir: String): F[Unit] =
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
      textFileNames    <- Sync[F].delay(textGroups.indices.map(i => validOutputDir + "/" + prefixedFileName + "_"+ i + "_.csv"))
      textFiles        <- Sync[F].delay(textFileNames.map(name => new File(name)))
      resources        <- Sync[F].delay(textFiles.map(textGroupsToResource).toList)
      lineSeparator    <- Sync[F].delay(System.getProperty("line.separator"))

      //ToDo partraverse
      _                <- resources.zipWithIndex.traverse(pair => pair._1.use(ostream => Sync[F].delay(
                            textGroups(pair._2).foreach(textLine => {
                              ostream.write(textLine.getBytes("UTF-8"))
                              ostream.write(lineSeparator.getBytes("UTF-8"))
                            }))
                              *> Sync[F].delay(println( textFileNames(pair._2) + " file created"))))
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
