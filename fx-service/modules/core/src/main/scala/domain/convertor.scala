package domain

import derevo.cats._
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import domain.AppExceptionHandler.AppException
import io.circe.{Decoder, DecodingFailure, Encoder, Json, KeyDecoder}
import squants.market.{Currency, Money, MoneyContext, defaultMoneyContext}
import cats.{Eq, MonadThrow, Show}

import scala.util._

object convertor {
  implicit val currencyEq: Eq[Currency] = Eq.and(Eq.and(Eq.by(_.code), Eq.by(_.symbol)), Eq.by(_.name))

  implicit val moneyContext: MoneyContext = defaultMoneyContext

  implicit val cKeyDecoder: KeyDecoder[Currency] = new KeyDecoder[Currency] {
    override def apply(key: String): Option[Currency] = Currency(key).toOption
  }

  implicit val eq: Eq[Money] = Eq.fromUniversalEquals
  implicit val moneyEncoder: Encoder[Money] = Encoder[Money] { (m: Money) =>
    Json.fromBigDecimal(m.amount)
  }

  implicit val currencyEncoder: Encoder[Currency] = Encoder[Currency] { (c: Currency) =>
    Json.fromString(c.code)
  }

  implicit val currencyDecoder: Decoder[Currency] = Decoder.instance { c =>
    c.as[String].flatMap { s =>
      Currency(s) match {
        case Success(curr) => Right(curr)
        case Failure(_) => Left(DecodingFailure(s"Unsupported currency: $s", c.history))
      }
    }
  }

  @derive(decoder, show)
  case class FxReq(fromCurrency: String, toCurrency: String, amount: BigDecimal) {
    def refine[F[_]: MonadThrow]: F[(Currency, Currency, Money)] =
      MonadThrow[F].fromTry(
        for {
        f <- Currency(fromCurrency)
        t <- Currency(toCurrency)
        c = f(amount)
      } yield (f, t, c))
  }
  //  convertor.execute
  //  POST /api/convert
  //  Body:
//      {
//        "fromCurrency": "GBP",
//        "toCurrency" : "EUR",
//        "amount" : 102.6
//      }

  @derive(decoder)
  case class CurrencyResponse(currencies: Map[Currency, BigDecimal])
  //http://943r6.mocklab.io/exchange-rates/GBP
//  {
//    "USD": 1.362250,
//    "EUR": 1.164659,
//    "CHF": 1.248216,
//    "CNY": 8.857225
//  }

  implicit val showMoney: Show[Money] = Show.fromToString
  @derive(encoder, show)
  case class FxResponse(exchange: BigDecimal, amount: Money, original: BigDecimal)
//  {
//    "exchange" : 1.11,
//    "amount" : 113.886,
//    "original" : 102.6
//  }

  abstract class FxClientException(msg: String) extends AppException(msg)

  @derive(eqv, show)
  case class BaseCurrencyNotFound(msg: String) extends FxClientException("Supplied base currency is not acceptable, try a different currency")

  @derive(eqv, show)
  case class ResponseMalformedException(msg: String) extends FxClientException("The response format is not expected. Can't decode the message")

  @derive(eqv, show)
  case class FxClientNetworkException(msg: String) extends FxClientException(msg)
}
