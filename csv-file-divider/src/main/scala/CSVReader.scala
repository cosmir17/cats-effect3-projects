import cats.effect.{Resource, _}
import cats.effect.std.Console
import cats.syntax.all._

import java.io._
import scala.io.BufferedSource
import scala.language.postfixOps

object CSVReader {
  def make[F[_] : Async : MonadCancelThrow : Console](): CSVReader[F] =
    new CSVReader[F]() {}
}

class CSVReader[F[_] : Async : MonadCancelThrow : Console] private () {

  def parseFile(inputFileName: String): F[Resource[F, Iterator[String]]] =
    for {
      name <- validateFileName(inputFileName)
      csvResource = csvToResource(name)
    } yield csvResource.evalMap(i => Sync[F].delay(i.getLines()))

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
      Sync[F].blocking(io.Source.fromFile(validFileName) )                           // build
    } { bufferedSource =>
      Sync[F].blocking(bufferedSource.close()).handleErrorWith(_ => Sync[F].unit)  // release
    }
}
