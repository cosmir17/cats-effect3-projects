import cats.effect._
import cats.effect.std.Console

import scala.language.postfixOps

object TextManipulator {
  def make[F[_] : Sync : MonadCancelThrow : Console](): TextManipulator[F] =
    new TextManipulator[F]() {}
}

class TextManipulator[F[_] : Sync : MonadCancelThrow : Console] private() {
  def divide(strLines: Resource[F, Iterator[String]], maxLines: Option[Int], maxBytes: Option[Int]): Resource[F, List[Iterator[String]]] =
    (maxLines, maxBytes) match {
      case (None, None) => strLines.map(lines => List(lines))
      case (Some(lineConfig), None) => processWithLineConfig(strLines, lineConfig)
      case (None, Some(byteConfig)) => processWithByteConfig(strLines, byteConfig)
      case (Some(lineConfig), Some(byteConfig)) => processWithBothConfigs(strLines, lineConfig, byteConfig)
    }

  //ToDo Refactoring: Need to make the following three methods generic to be one method.
  private def processWithBothConfigs(strLines: Resource[F, Iterator[String]], maxLines: Int, maxBytes: Int): Resource[F, List[Iterator[String]]] =
    strLines.map(_.foldLeft[List[Seq[String]]](List())((list, eachLine) =>
        if (list.isEmpty) List(Seq(eachLine))
        else if ((list.last.length > maxLines) || ( list.last.reduce(_ + _).getBytes("UTF-8").length > maxBytes)) list :+ Seq(eachLine)
        else {
          val currentGroup  = list.last
          list.init :+ (currentGroup :+ eachLine)
        }
      )).map(_.map(_.iterator))

  private def processWithByteConfig(strLines: Resource[F, Iterator[String]], maxBytes: Int): Resource[F, List[Iterator[String]]] =
    strLines.map(_.foldLeft[List[Seq[String]]](List())((list, eachLine) =>
      if (list.isEmpty) List(Seq(eachLine))
      else if ( list.last.reduce(_ + _).getBytes("UTF-8").length > maxBytes) list :+ Seq(eachLine)
      else {
        val currentGroup  = list.last
        list.init :+ (currentGroup :+ eachLine)
      }
    )).map(_.map(_.iterator))

  private def processWithLineConfig(strLines: Resource[F, Iterator[String]], lineConfig: Int): Resource[F, List[Iterator[String]]] =
    strLines.map(_.foldLeft[List[Seq[String]]](List())((list, eachLine) =>
      if (list.isEmpty) List(Seq(eachLine))
      else if (list.last.length > lineConfig) list :+ Seq(eachLine)
      else {
        val currentGroup  = list.last
        list.init :+ (currentGroup :+ eachLine)
      }
    )).map(_.map(_.iterator))

//  /**
//   * ToDo partraverse
//   * @param strLines
//   * @param maxLines
//   * @return
//   */
//  private def processWithLineConfig(strLines: Seq[String], maxLines: Int): F[Resource[F, List[Iterator[String]]]] = {
//    val noOfFullFiles = strLines.length / maxLines
//    val noOfFiles = if (strLines.length % maxLines > 0) noOfFullFiles + 1 else noOfFullFiles
//
//    val a = Range(0, noOfFiles).toList
//      .parTraverse(i => Async[F].delay(i * maxLines))
//  }

}


