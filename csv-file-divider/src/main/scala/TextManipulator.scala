import cats.effect._
import cats.effect.std.Console

import scala.language.postfixOps

object TextManipulator {
  def make[F[_] : Sync : MonadCancelThrow : Console](): TextManipulator[F] =
    new TextManipulator[F]() {}
}

class TextManipulator[F[_] : Sync : MonadCancelThrow : Console] private() {
  def divide(strLines: Seq[String], maxLines: Option[Int], maxBytes: Option[Int]): F[List[Seq[String]]] =
    (maxLines, maxBytes) match {
      case (None, None) => Sync[F].delay(List(strLines))
      case (Some(lineConfig), None) => Sync[F].delay(processWithLineConfig(strLines, lineConfig))
      case (None, Some(byteConfig)) => Sync[F].delay(processWithByteConfig(strLines, byteConfig))
      case (Some(lineConfig), Some(byteConfig)) => Sync[F].delay(processWithBothConfigs(strLines, lineConfig, byteConfig))
    }

  //ToDo Refactoring: Need to make the following three methods generic to be one method.
  private def processWithBothConfigs(strLines: Seq[String], maxLines: Int, maxBytes: Int): List[Seq[String]] =
    strLines.foldLeft[List[Seq[String]]](List())((list, eachLine) =>
        if (list.isEmpty) List(Seq(eachLine))
        else if ((list.last.length > maxLines) || ( list.last.reduce(_ + _).getBytes("UTF-8").length > maxBytes)) list :+ Seq(eachLine)
        else {
          val currentGroup  = list.last
          list.init :+ (currentGroup :+ eachLine)
        }
      )

  private def processWithByteConfig(strLines: Seq[String], maxBytes: Int): List[Seq[String]] =
    strLines.foldLeft[List[Seq[String]]](List())((list, eachLine) =>
      if (list.isEmpty) List(Seq(eachLine))
      else if ( list.last.reduce(_ + _).getBytes("UTF-8").length > maxBytes) list :+ Seq(eachLine)
      else {
        val currentGroup  = list.last
        list.init :+ (currentGroup :+ eachLine)
      }
    )

  private def processWithLineConfig(strLines: Seq[String], lineConfig: Int): List[Seq[String]] =
    strLines.foldLeft[List[Seq[String]]](List())((list, eachLine) =>
      if (list.isEmpty) List(Seq(eachLine))
      else if (list.last.length > lineConfig) list :+ Seq(eachLine)
      else {
        val currentGroup  = list.last
        list.init :+ (currentGroup :+ eachLine)
      }
    )

//  /**
//   * ToDo partraverse
//   * @param strLines
//   * @param maxLines
//   * @return
//   */
//  private def processWithLineConfig(strLines: Seq[String], maxLines: Int): F[List[Seq[String]]] = {
//    val noOfFullFiles = strLines.length / maxLines
//    val noOfFiles = if (strLines.length % maxLines > 0) noOfFullFiles + 1 else noOfFullFiles
//
//    val a = Range(0, noOfFiles).toList
//      .parTraverse(i => Async[F].delay(i * maxLines))
//  }

}


