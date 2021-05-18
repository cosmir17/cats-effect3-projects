import cats.effect.{Resource, _}
import cats.effect.std.Console
import cats.syntax.all._

import java.io._
import scala.io.BufferedSource
import scala.language.postfixOps

object CSVReader {
  def make[F[_] : Sync : MonadCancelThrow : Console](): CSVReader[F] =
    new CSVReader[F]() {}
}

class CSVReader[F[_] : Sync : MonadCancelThrow : Console] private () {

  def parseFile(inputFileName: String): F[Seq[String]] =
    for {
      name <- validateFileName(inputFileName)
      csvResource = csvToResource(name)
      strLines <- csvResource.use(i => Sync[F].delay((for (line <- i.getLines()) yield line).toSeq))
    } yield strLines

  private def validateFileName(csvFileName: String): F[String] =
    for {
      file <- Sync[F].delay(new File(".").listFiles.filter(_.isFile).filter(_.getName == csvFileName).toList)
      newName <- if (file.isEmpty) Sync[F].delay(println(s"$csvFileName does not exist, enter a valid name "))
        .flatMap(_ => Console.make.readLine)
        .flatMap(name => validateFileName(name))
      else Sync[F].delay(println(s"$csvFileName is found")).map(_ => csvFileName)
    } yield newName

  private def csvToResource(validFileName: String): Resource[F, BufferedSource] =
    Resource.make {
      Sync[F].delay(io.Source.fromFile(validFileName) )                           // build
    } { bufferedSource =>
      Sync[F].delay((bufferedSource.close())).handleErrorWith(_ => Sync[F].unit)  // release
    }
}
