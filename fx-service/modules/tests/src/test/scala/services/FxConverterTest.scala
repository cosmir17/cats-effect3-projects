package services

import cats.effect._
import domain.convertor.{FxClientNetworkException, FxResponse, ResponseMalformedException}
import http.clients.FxClient
import org.mockito.cats.IdiomaticMockitoCats
import org.mockito.IdiomaticMockito
import org.typelevel.log4cats.testing.TestingLogger
import squants.market.{Currency, Money, defaultMoneyContext}
import weaver.SimpleIOSuite

object FxConverterTest extends SimpleIOSuite with IdiomaticMockito with IdiomaticMockitoCats {
  implicit val moneyContext = defaultMoneyContext

  test("Should convert for valid data") {
    implicit val logger = TestingLogger.impl()

    val fx = mock[FxClient[IO]]
    fx.query("GBP") returns IO { Map(Currency("USD").get -> 1.362250, Currency("EUR").get -> 1.164659,  Currency("CHF").get -> 1.248216) }
    val convertor = FxConverter.make[IO](fx)

    for {
        fxResponse <- convertor.execute(Currency("GBP").get, Currency("EUR").get, Money(20.0, Currency("GBP").get))
      } yield expect.same(FxResponse(1.164659, Money(23.29318, Currency("EUR").get), 20.0), fxResponse)
  }

  test("Should fail for invalid data, the downstream call response doesn't contain a required currency") {
    implicit val logger = TestingLogger.impl()

    val fx = mock[FxClient[IO]]
    fx.query("GBP") returns IO { Map(Currency("USD").get -> 1.362250, Currency("CHF").get -> 1.248216) }
    val convertor = FxConverter.make[IO](fx)

   convertor
     .execute(Currency("GBP").get, Currency("EUR").get, Money(20, Currency("GBP").get))
     .attempt
     .map{
       case Left(e) => expect.same(ResponseMalformedException("the response doesn't contain desired currency"), e)
       case _ => failure("unexpected error")
     }
  }

  test("Should fail for invalid data, the downstream call fails") {
    implicit val logger = TestingLogger.impl()

    val fx = mock[FxClient[IO]]
    fx.query("GBP") returns IO.raiseError(FxClientNetworkException("Network issue"))
    val convertor = FxConverter.make[IO](fx)

    convertor
      .execute(Currency("GBP").get, Currency("EUR").get, Money(20, Currency("GBP").get))
      .attempt
      .map{
        case Left(e) => expect.same(FxClientNetworkException("Network issue"), e)
        case _ => failure("unexpected error")
      }
  }
}