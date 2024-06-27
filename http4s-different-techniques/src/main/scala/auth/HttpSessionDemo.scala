package auth

import cats.data.{Kleisli, OptionT}
import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.comcast.ip4s._
import org.http4s._
import org.http4s.server._
import org.http4s.implicits._
import org.http4s.dsl.io._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.Authorization
import org.http4s.headers.Cookie
import org.http4s.server.middleware.authentication.DigestAuth
import org.http4s.server.middleware.authentication.DigestAuth.Md5HashedAuthStore

import java.nio.charset.StandardCharsets
import java.time.LocalTime
import java.util.Base64
import scala.util.Try

object HttpSessionDemo extends IOApp.Simple {
  case class User(id: Long, name: String)

  def today: String = LocalTime.now().toString
  def setToken(user: String, date: String) = Base64.getEncoder.encodeToString(s"$user:$date".getBytes(StandardCharsets.UTF_8))

  def getUser(token: String): Option[String] = Try(new String(Base64.getDecoder.decode(token)).split(":")(0)).toOption

  // login endpoint
  val authRoutes = AuthedRoutes.of[User, IO] {
    case GET -> Root / "welcome" as user => //localhost:8090/welcome/Sean
      Ok(s"Welcome, $user").map(_.addCookie(
        ResponseCookie("sessioncookie", setToken(user.name, today), maxAge = Some(24 * 3600)))
      ) //business logic
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
  val middlewareResource = Resource.eval(middleware)

  val cookieAccessRoutes = HttpRoutes.of[IO] {
    case GET -> Root / "statement" / user =>
      Ok("here is your financial statement!")
    case GET -> Root / "logout" =>
      Ok("Logging out").map(_.removeCookie("sessioncookie"))
  }

  def checkSessionCookie(cookie: Cookie): Option[RequestCookie] =
    cookie.values.toList.find(_.name == "sessioncookie")

  def modifyPath(user: String): Path =
    Uri.Path.unsafeFromString(s"/statement/$user")

  // prove that the user has a cookie
  def cookieCheckerApp(app: HttpRoutes[IO]): HttpRoutes[IO] = Kleisli { req =>
    val authHeader: Option[Cookie] = req.headers.get[Cookie]
    OptionT.liftF(
      authHeader.fold(Ok("No cookies")) { cookie =>
        checkSessionCookie(cookie).fold(Ok("No token")) { token =>
          getUser(token.content).fold(Ok("Invalid Token")) { user =>
            app.orNotFound.run(req.withPathInfo(modifyPath(user)))
          }
        }
      }
    )

  }

  val routerResource = middlewareResource.map { mw =>
    Router(
      "/login" -> mw(authRoutes), //login endpoint (unauthed)
      "/" -> cookieCheckerApp(cookieAccessRoutes) //authed endpoints
    )
  }

  val serverResource = for {
    router <- routerResource
    server <- EmberServerBuilder.default[IO].withHost(ipv4"0.0.0.0").withPort(port"8090")
    .withHttpApp(router.orNotFound)
    .build
  } yield server

  override def run: IO[Unit] =
    serverResource.useForever.as(ExitCode.Success)
}


// sk@computer-hello ~ % curl -v localhost:8090/login/welcome --digest -u sean:my_password
//*   Trying [::1]:8090...
//* Connected to localhost (::1) port 8090
//* Server auth using Digest with user 'sean'
//> GET /login/welcome HTTP/1.1
//> Host: localhost:8090
//> User-Agent: curl/8.4.0
//> Accept: */*
//>
//< HTTP/1.1 401 Unauthorized
//< Date: Wed, 12 Jun 2024 03:27:31 GMT
//< Connection: keep-alive
//< WWW-Authenticate: Digest realm="http://localhost:8090",qop="auth",nonce="88fc535041634edb00aa739f62a0b14f7502d99f"
//< Content-Length: 0
//<
//* Connection #0 to host localhost left intact
//* Issue another request to this URL: 'http://localhost:8090/login/welcome'
//* Found bundle for host: 0x6000039bc150 [serially]
//* Can not multiplex, even if we wanted to
//* Re-using existing connection with host localhost
//* Server auth using Digest with user 'sean'
//> GET /login/welcome HTTP/1.1
//> Host: localhost:8090
//> Authorization: Digest username="sean", realm="http://localhost:8090", nonce="88fc535041634edb00aa739f62a0b14f7502d99f", uri="/login/welcome", cnonce="MDBkZWViNTlhN2ZiYzY1MGI4NWVlMzA0ZGQzOThkYzU=", nc=00000001, qop=auth, response="e887fcc21b8459e42d9226a669d88d3d"
//> User-Agent: curl/8.4.0
//> Accept: */*
//>
//< HTTP/1.1 200 OK
//< Date: Wed, 12 Jun 2024 03:27:31 GMT
//< Connection: keep-alive
//< Content-Type: text/plain; charset=UTF-8
//< Content-Length: 21
//< Set-Cookie: sessioncookie=U2VhbjowNDoyNzozMS43MjY5NTE=; Max-Age=86400
//<
//* Connection #0 to host localhost left intact
//Welcome, User(1,Sean)%
//
//
//
//
//
//
//
// sk@computer-hello ~ %
// curl -v --cookie 'sessioncookie=U2VhbjowNDoyNzozMS43MjY5NTE=' http://localhost:8090/statement/sean;

//response =>
//*   Trying [::1]:8090...
//* Connected to localhost (::1) port 8090
//> GET /statement/sean HTTP/1.1
//> Host: localhost:8090
//> User-Agent: curl/8.4.0
//> Accept: */*
//> Cookie: sessioncookie=U2VhbjowNDoyNzozMS43MjY5NTE=
//>
//< HTTP/1.1 200 OK
//< Date: Wed, 12 Jun 2024 03:28:10 GMT
//< Connection: keep-alive
//< Content-Type: text/plain; charset=UTF-8
//< Content-Length: 33
//<
//* Connection #0 to host localhost left intact
//here is your financial statement!%                                                                                                                                                                                                                        sk@computer-hello ~ %