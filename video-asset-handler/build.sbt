import Dependencies._

ThisBuild / scalaVersion := "3.4.2"
ThisBuild / version := "0.0.2"
ThisBuild / organization := "com.seank"
ThisBuild / organizationName := "seank"

lazy val root = (project in file("."))
  .settings(name := "video-asset-handler")
  .aggregate(core, tests)

lazy val testLibraryDependencies = Seq(
  Libraries.catsLaws % Test,
  Libraries.log4catsNoOp % Test,
  Libraries.log4catsTesting % Test,
  Libraries.monocleLaw % Test,
  Libraries.weaverCats % Test,
  Libraries.weaverDiscipline % Test,
  Libraries.weaverScalaCheck % Test,
  Libraries.wiremock % Test,
  Libraries.specs2 % Test,
  Libraries.apacheCommon % Test,
  Libraries.scodecCats % Test
)

// integration tests
lazy val it = (project in file("modules/tests/src/it"))
  .settings(commonSettings: _*)
  .settings(
    name := "video-handler-integration-test-suite",
    libraryDependencies ++= testLibraryDependencies
  )
  .dependsOn(core)

lazy val tests = (project in file("modules/tests/src/test"))
  .settings(
    name := "video-handler-test-suite",
    libraryDependencies ++= testLibraryDependencies
  )
  .dependsOn(core)

def dockerSettings(name: String) = List(
  Docker / packageName := s"trading-$name",
  dockerBaseImage      := "jdk17-curl:latest",
  dockerExposedPorts ++= List(8080),
  makeBatScripts     := Nil,
  dockerUpdateLatest := true
)

val commonSettings = List(
  scalafmtOnCompile := false,
  testFrameworks += new TestFramework("weaver.framework.CatsEffect"),
  libraryDependencies ++= List(
    Libraries.cats,
    Libraries.catsEffect,
    Libraries.circeCore,
    Libraries.circeGeneric,
    Libraries.circeParser,
    Libraries.circeRefined,
    Libraries.cirisCore,
    Libraries.cirisEnum,
    Libraries.cirisRefined,
    Libraries.fs2,
    Libraries.http4sDsl,
    Libraries.http4sServer,
    Libraries.http4sClient,
    Libraries.http4sCirce,
    Libraries.javaxCrypto,
    Libraries.log4cats,
    Libraries.logback % Runtime,
    Libraries.monocleCore,
    Libraries.coreIron,
    Libraries.ironCats,
    Libraries.ironCirce,
    Libraries.ironScalaCheck,
    Libraries.squants,
    Libraries.declineEffect,
    Libraries.apacheCodec
  )
)

lazy val core = (project in file("modules/core"))
  .enablePlugins(DockerPlugin)
  .settings(dockerSettings("core"))
  .enablePlugins(AshScriptPlugin)
  .settings(commonSettings)

addCommandAlias("runLinter", ";scalafixAll --rules OrganizeImports")