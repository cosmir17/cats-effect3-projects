name := "csv-file-divider"

version := "0.1"

scalaVersion := "2.13.5"

scalacOptions ++= List("-Ymacro-annotations", "-Yrangepos", "-Wconf:cat=unused:info")

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect"         % "3.1.1",
  "org.typelevel" %% "cats-effect-kernel"  % "3.1.1",
  "org.typelevel" %% "cats-effect-std"     % "3.1.1",
  "org.typelevel" %% "cats-core"           % "2.6.1",
  "org.typelevel" %% "cats-laws"           % "2.6.1",

  "com.monovore"  %% "decline-effect"      % "2.0.0",
  "com.disneystreaming"        %% "weaver-cats"       % "0.7.2",
  "com.disneystreaming"        %% "weaver-discipline" % "0.7.2",
  "com.disneystreaming"        %% "weaver-scalacheck" % "0.7.2",
  "org.typelevel"              %% "log4cats-noop"     % "2.1.0",
  "com.github.julien-truffaut" %% "monocle-law"       % "3.0.0-M5",
)

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
addCompilerPlugin("org.typelevel" % "kind-projector" % "0.11.3"  cross CrossVersion.full)