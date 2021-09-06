package services

import cats.MonadThrow
import cats.syntax.all._
import domain.convertor.{FxResponse, ResponseMalformedException}
import http.clients.FxClient
import org.typelevel.log4cats.Logger
import squants.market.{Currency, CurrencyExchangeRate, Money}

trait FxConverter[F[_]] {
  def execute (fromCurrency: Currency, toCurrency: Currency, money: Money): F[FxResponse]
}
object FxConverter {
  def make[F[_]: Logger: MonadThrow](fx: FxClient[F]): FxConverter[F] =
    (fromCurrency: Currency, toCurrency: Currency, money: Money) => for {
      currencies <- fx.query(fromCurrency.name)
      cFound = currencies.find(_._1 == toCurrency)
      cFoundRefined <- MonadThrow[F].fromOption(cFound, ResponseMalformedException("the response doesn't contain desired currency"))
      fx = CurrencyExchangeRate(fromCurrency(1), toCurrency(cFoundRefined._2))
      toAmount = fx.convert(money)
    } yield FxResponse(fx.rate, toAmount, money.amount)

}

