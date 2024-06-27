package http.routes

import cats.Monad
import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.{Header, HttpRoutes}
import org.http4s.dsl.impl._
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.ci.CIString
import cats.implicits._
import io.circe.syntax._
import io.circe.generic.auto._
import org.http4s.circe._
import org.http4s.dsl._
import org.http4s.implicits._
import org.http4s.server.Router
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

import java.util.UUID

object SimpleHttp4sPractice extends IOApp.Simple {
  implicit val logging: LoggerFactory[IO] = Slf4jFactory.create[IO]

  type Student = String
  case class Instructor(firstName: String, lastName: String)
  case class Course(id: String, title: String, year: Int, students: List[Student], instructorName: String)

  object CourseRepository {

    private val catsEffectCourse = Course(
      "550e8400-e29b-41d4-a716-446655440000",
      "Scala Practice",
      2014,
      List("Tom", "Matt"),
      "Sean"
    )

    private val courses: Map[String, Course] = Map(catsEffectCourse.id -> catsEffectCourse)

    def findCoursesById(courseId: UUID): Option[Course] =
      courses.get(courseId.toString)

    def findCoursesByInstructor(name: String): List[Course] =
      courses.values.filter(_.instructorName == name).toList
  }

  //essential Rest endpoints
  // GET localhost:8080/courses?instructor=Martin%20Persky&year=2022
  // GET localhost:8080/courses/550e8400-e29b-41d4-a716-446655440000/students

  private object InstructorQueryParamMatcher extends QueryParamDecoderMatcher[String]("instructor")
  private object YearQueryParamMatcher extends OptionalValidatingQueryParamDecoderMatcher[Int]("year")

  private def courseRoutes[F[_]: Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "courses" :? InstructorQueryParamMatcher(instructor) +& YearQueryParamMatcher(maybeYear) =>
        val courses = CourseRepository.findCoursesByInstructor(instructor)
        maybeYear match {
          case Some(y) => y.fold(
            _ => BadRequest("Parameter 'year' is invalid"),
            year => Ok(courses.filter(_.year == year).asJson, Header.Raw(CIString("My-custom-header"), "my header value"))
        )
          case None => Ok(courses.asJson)
        }
      case GET -> Root / "courses" / UUIDVar(courseId) / "students" =>
        CourseRepository.findCoursesById(courseId).map(_.students) match {
        case Some(students) => Ok(students.asJson)
        case None => NotFound(s"No course with $courseId was found")
      }
    }
  }

  private def healthEndpoint[F[_]: Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "health" => Ok("All going great!")
    }
  }

  private def allRoutes[F[_]: Monad]: HttpRoutes[F] = courseRoutes[F] <+> healthEndpoint[F]

  private def routerWithPathPrefixes = Router(
    "/api" -> courseRoutes[IO],
    "/private" -> healthEndpoint[IO]
  ).orNotFound


  override def run: IO[Unit] =
    EmberServerBuilder
      .default[IO]
      .withHttpApp(routerWithPathPrefixes)
      .build
      .use(_ => IO.println("Server ready!") *> IO.never)
}