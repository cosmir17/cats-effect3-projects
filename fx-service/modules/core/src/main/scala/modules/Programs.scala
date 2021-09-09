package modules

import cats.effect.Temporal
import org.typelevel.log4cats.Logger
import services.FxConverter

object Programs {
  def make[F[_]: Logger: Temporal](clients: HttpClients[F]): Programs[F] =
    new Programs[F](clients) {}
}

sealed abstract class Programs[F[_]: Logger: Temporal] (val clients: HttpClients[F]) {
  val fxConvertor: FxConverter[F] = FxConverter.make(clients.fxQuerier)
}
