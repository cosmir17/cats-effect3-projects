package auth

import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.comcast.ip4s._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server._
import org.http4s.server.middleware.authentication.DigestAuth
import org.http4s.server.middleware.authentication.DigestAuth.Md5HashedAuthStore

object HttpDigestDemo extends IOApp {

  case class User(id: Long, name: String)

  val routes: HttpRoutes[IO] = {
    HttpRoutes.of {
      case GET -> Root / "welcome" / user =>
        Ok(s"Welcome $user")
    }
  }

  val searchFunc: String => IO[Option[(User, String)]] = { //query my db for my user
    case "sean" =>
      for {
        user <- IO.pure(User(1L, "Sean"))
        hash <- Md5HashedAuthStore.precomputeHash[IO]("sean", "http://localhost:8090", "my_password")
      } yield Some(user, hash)

    case _ => IO.pure(None) // user can't be found
  }

  val authStore = Md5HashedAuthStore(searchFunc)
  val middleware: IO[AuthMiddleware[IO, User]] = DigestAuth.applyF[IO, User]("http://localhost:8090", authStore)

  val authRoutes = AuthedRoutes.of[User, IO] {
    case GET -> Root / "welcome" as user => //localhost:8090/welcome/Sean
      Ok(s"Welcome, $user") //business logic
  }

  val middlewareResource = Resource.eval(middleware)

  val serverResource = for {
    mw <- middlewareResource
    sv <- EmberServerBuilder.default[IO].withHost(ipv4"0.0.0.0").withPort(port"8090").withHttpApp(mw(authRoutes).orNotFound).build
  } yield sv


  override def run(args: List[String]): IO[ExitCode] = {
    serverResource.useForever.as(ExitCode.Success)
  }

}
