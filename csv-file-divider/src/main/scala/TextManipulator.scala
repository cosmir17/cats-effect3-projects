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
      case (ml, mb) => processWithConfigs(strLines, ml, mb)
    }

  private def processWithConfigs(strLines: Resource[F, Iterator[String]], maxLines: Option[Int], maxBytes: Option[Int]): Resource[F, List[Iterator[String]]] =
    strLines.map(_.foldLeft[List[Seq[String]]](List())((list, eachLine) =>
        if (list.isEmpty) List(Seq(eachLine))
        else if (maxLines.exists(_ < list.last.length) || maxBytes.exists(_ < list.last.reduce(_ + _).getBytes("UTF-8").length)) list :+ Seq(eachLine)
        else {
          val currentGroup  = list.last
          list.init :+ (currentGroup :+ eachLine)
        }
      )).map(_.map(_.iterator))
}


