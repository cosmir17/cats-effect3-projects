package modules

import cats.effect.Temporal
import services.FxConverter

object Programs {
  def make[F[_]: Temporal](clients: HttpClients[F]): Programs[F] =
    new Programs[F](clients) {}
}

sealed abstract class Programs[F[_]: Temporal] (val clients: HttpClients[F]) {
  val fxConvertor: FxConverter[F] = FxConverter.make(clients.fxQuerier)
}
