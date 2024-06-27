package auth

import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.server._
import org.http4s.ember.server._
import com.comcast.ip4s._
import org.http4s.server.middleware.authentication.DigestAuth
import org.http4s.headers.Cookie
import cats.data._
import java.time.LocalDateTime
import java.util.Base64
import java.nio.charset.StandardCharsets
import scala.util._

object SessionAuth extends IOApp {
  case class User(id: Long, name: String)

  val today: String = LocalDateTime.now().toString()

  def setToken(user: String, date: String): String = Base64.getEncoder.encodeToString(s"${user}:{$today}".getBytes(StandardCharsets.UTF_8))

  def getUser(token: String): Try[String] = Try(new String(Base64.getDecoder.decode(token)).split(":")(0))

  val funcPass: String => IO[Option[(User, String)]] = (user_val: String) =>
    user_val match {
      case "username" => IO(Some(User(1, "username"), "password"))
      case _ => IO(None)
    }

  val middleware: AuthMiddleware[IO, User] = DigestAuth[IO, User]("http://localhost:8080/welcome", funcPass)

  val authedRoutes: AuthedRoutes[User, IO] =
    AuthedRoutes.of {
      case GET -> Root / "welcome" as user =>
        Ok(s"Welcome, ${user.name}").map(_.addCookie(ResponseCookie("sessioncookie", setToken(user.name, today), maxAge = Some(86400))))
    }

  val digestService: HttpRoutes[IO] =
    middleware(authedRoutes)


  val cookieAccessRoutes = HttpRoutes.of[IO] {
    case GET -> Root / "statement" / user =>
      Ok(s"Welcome back $user, Financial statement processing...")
    case GET -> Root / "logout" =>
      Ok("Logging out...").map(_.removeCookie("sessioncookie"))
  }

  def cookieCheckerService(service: HttpRoutes[IO]): HttpRoutes[IO] = Kleisli { req =>
    val authHeader: Option[Cookie] = req.headers.get[Cookie]
    OptionT.liftF(authHeader match {
      case Some(cookie) =>
        cookie.values.toList.find { x => x.name == "sessioncookie"} match {
          case Some(token) => getUser(token.content) match {
            case Success(user) => service.orNotFound.run((req.withPathInfo(Uri.Path.fromString(s"/statement/$user"))))
            case Failure(_) => Ok("Invalid token")
          }
          case None => Ok("No token")
        }
      case None => Ok("No cookies")
    })
  }


  val serviceRouter =
    Router(
      "/login" -> digestService,
      "/" -> cookieCheckerService(cookieAccessRoutes)
    )

  val server = EmberServerBuilder
    .default[IO]
    .withHost(ipv4"0.0.0.0")
    .withPort(port"8080")
    .withHttpApp(serviceRouter.orNotFound)
    .build

  override def run(args: List[String]): IO[ExitCode] = {
    server.use(_ => IO.never).as(ExitCode.Success)
  }

}