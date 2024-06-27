import cats.Monad
import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.{HttpApp, HttpRoutes, ParseFailure, QueryParamDecoder}
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.{OptionalQueryParamDecoderMatcher, OptionalValidatingQueryParamDecoderMatcher, QueryParamDecoderMatcher}
import cats.implicits._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.blaze.server.BlazeServerBuilder
import org.typelevel.log4cats.slf4j.Slf4jFactory
import java.time.Year
import scala.collection.mutable
import scala.util.Try
import org.http4s.circe._
import org.http4s.server.Router
import org.typelevel.log4cats.{Logger, LoggerFactory}
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util.UUID

object Http4sServer extends IOApp {
  implicit val logging: LoggerFactory[IO] = Slf4jFactory.create[IO]

  type Actor = String
  case class Movie(id: String, title: String, year: Int, actors: List[Actor], director: String)
  case class Director(firstName: String, lastName: String) {
    override def toString: String = s"$firstName $lastName"
  }

  case class DirectorDetails(firstName: String, lastName: String, genre: String)

  val terminatorTwo: Movie = Movie(
    "550e8400-e29b-41d4-a716-446655440000",
    "Terminator 2",
    1985,
    List("Arnold Sch", "Linda"),
    "James Cameron"
  )

  val movies: Map[String, Movie] = Map(terminatorTwo.id -> terminatorTwo)

  private def findMovieById(movieId: UUID) =
    movies.get(movieId.toString)

  private def findMoviesByDirector(director: String): List[Movie] =
    movies.values.filter(_.director == director).toList

  implicit val yearQueryParamDecoder: QueryParamDecoder[Year] =
    QueryParamDecoder[Int].emap { yearInt =>
      Try(Year.of(yearInt)).toEither.leftMap { e =>
        ParseFailure(e.getMessage, e.getMessage)
      }
    }

  private object DirectorQueryParamMatcher extends QueryParamDecoderMatcher[String]("director")
  private object YearQueryParamMatcher extends OptionalValidatingQueryParamDecoderMatcher[Year]("year")

  // GET /movies?director=Zack%20Snyder&year=2021
  def movieRoutes[F[_]: Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "movies" :? DirectorQueryParamMatcher(director) +& YearQueryParamMatcher(maybeYear) =>
        val moviesByDirector = findMoviesByDirector(director)
        maybeYear match {
          case Some(validatedYear) => validatedYear.fold(_ => BadRequest("The year is badly formatted"), year => {
            val moviesByDirectorAndYear = moviesByDirector.filter(_.year == year.getValue)
            Ok(moviesByDirectorAndYear.asJson)
          })
          case None => Ok(moviesByDirector.asJson)
        }
      case GET -> Root / "movies" / UUIDVar(movieId) / "actors" =>
        findMovieById(movieId).map(_.actors) match {
          case Some(actors) => Ok(actors.asJson)
          case None => NotFound(s"No movie with id $movieId")
        }
    }
  }

  object DirectorPath {
    def unapply(str: String): Option[Director] = {
      Try {
        val tokens = str.split(" ")
        Director(tokens(0), tokens(1))
      }.toOption
    }
  }

  val directorDetailsDB: mutable.Map[Director, DirectorDetails] =
    mutable.Map(Director("James", "Cameron") -> DirectorDetails("James", "Cameron", "many"))

  def directorRoutes[F[_]: Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "directors" / DirectorPath(director) =>
        directorDetailsDB.get(director) match {
          case Some(dirDetails) => Ok(dirDetails.asJson)
          case _ => NotFound(s"No director '$director' found")
        }
    }
  }

  def allRoutes[F[_]: Monad]: HttpRoutes[F] =
    movieRoutes[F] <+> directorRoutes[F] //cats.syntax.semigroupK

  def allRoutesComplete[F[_]: Monad]: HttpApp[F] =
    allRoutes[F].orNotFound

  override def run(args: List[String]): IO[ExitCode] = {
    val apis = Router(
      "/api" -> movieRoutes[IO],
      "/api/admin" -> directorRoutes[IO]
    ).orNotFound

    BlazeServerBuilder[IO]
      .bindHttp(8090, "localhost")
      .withHttpApp(allRoutesComplete) // alternatively 'apis'
      .resource
      .useForever
      .as(ExitCode.Success)

  }
}

