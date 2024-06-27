package auth

import cats.data.{Kleisli, OptionT}
import cats.effect.{ExitCode, IO, IOApp}
import com.comcast.ip4s._
import org.http4s._
import org.http4s.server._
import org.http4s.implicits._
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.Authorization

object HttpAuthDemo extends IOApp {

  case class User(id: Long, name: String)

  val routes: HttpRoutes[IO] = {
    HttpRoutes.of {
      case GET -> Root / "welcome" / user =>
        Ok(s"Welcome $user")
    }
  }

  val basicAuthMethod = Kleisli.apply[IO, Request[IO], Either[String, User]] { req =>
    req.headers.get[Authorization] match {
      case Some(Authorization(BasicCredentials(creds))) =>
        IO(Right(User(1L, creds._1)))
      case Some(_) => IO(Left("No basic credentials"))
      case None => IO(Left("Unauthorized"))
    }
  }

  val onFailure: AuthedRoutes[String, IO] = Kleisli { (req: AuthedRequest[IO, String]) =>
    val a = Response[IO](status = Status.Unauthorized)
    OptionT.pure(a)
  }

  // middleware
  // e.g. parsing -> final response

  val userBasicAuthMiddleware: AuthMiddleware[IO, User] = AuthMiddleware(basicAuthMethod, onFailure)

  val authRoutes = AuthedRoutes.of[User, IO] {
    case GET -> Root / "welcome" as user => //localhost:8090/welcome/Sean
      Ok(s"Welcome, $user") //business logic
  }

  val server =
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8090")
      .withHttpApp(userBasicAuthMiddleware(authRoutes).orNotFound)
      .build

  override def run(args: List[String]): IO[ExitCode] = {
    server.useForever.as(ExitCode.Success)
  }

}
