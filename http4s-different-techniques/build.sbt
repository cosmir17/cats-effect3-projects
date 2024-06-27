ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.14"

lazy val root = (project in file("."))
  .settings(
    name := "http4s-different-techniques"
  )

val JwtHttp4sVersion = "1.2.2"
val JwtScalaVersion = "9.4.6"

val jwtHttp4s =       "dev.profunktor"          %% "http4s-jwt-auth"     % JwtHttp4sVersion
val jwtScala =        "com.github.jwt-scala"    %% "jwt-core"            % JwtScalaVersion
val jwtCirce =        "com.github.jwt-scala"    %% "jwt-circe"           % JwtScalaVersion

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-blaze-server" % "0.23.16",
  "org.http4s" %% "http4s-ember-server" % "0.23.27",
  "org.http4s" %% "http4s-ember-client" % "0.23.27",
  "org.http4s" %% "http4s-circe"        % "0.23.27",
  "org.http4s" %% "http4s-dsl"          % "0.23.27",
  "io.circe"   %% "circe-generic"       % "0.14.7",
  "ch.qos.logback" % "logback-classic"  % "1.5.6",
  "org.typelevel" %% "log4cats-slf4j"   % "2.7.0",
  "org.typelevel" %% "log4cats-core"    % "2.7.0",

  jwtHttp4s,
  jwtScala,
  jwtCirce
)
