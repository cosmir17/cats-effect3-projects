import sbt._

object Dependencies {

  object V {
    val cats          = "2.10.0"
    val catsEffect    = "3.5.4"
    val catsRetry     = "3.1.3"
    val circe         = "0.14.7"
    val ciris         = "3.6.0"
    val javaxCrypto   = "1.0.1"
    val fs2           = "3.10.2"
    val http4s        = "1.0.0-M41"
    val log4cats      = "2.7.0"
    val monocle       = "3.0.0-M6"
    val squants       = "1.8.3"
    val iron          = "2.5.0"

    val logback          = "1.5.6"
    val declineEffect    = "2.4.1"
    val apacheCodec      = "1.17.0"

    val weaver        = "0.8.4"
    val specs2        = "5.5.1"
    val wiremock      = "3.0.1"
    val apacheCommon  = "2.16.1"
    val scodecCats    = "1.2.0"
  }

  object Libraries {
    def circe(artifact: String): ModuleID  = "io.circe"   %% s"circe-$artifact"  % V.circe
    def ciris(artifact: String): ModuleID  = "is.cir"     %% artifact            % V.ciris
    def http4s(artifact: String): ModuleID = "org.http4s" %% s"http4s-$artifact" % V.http4s
    def iron(artifact: String): ModuleID   = "io.github.iltotore" %% s"$artifact"  % V.iron

    val cats       = "org.typelevel"    %% "cats-core"   % V.cats
    val catsEffect = "org.typelevel"    %% "cats-effect" % V.catsEffect
    val catsRetry  = "com.github.cb372" %% "cats-retry"  % V.catsRetry
    val squants    = "org.typelevel"    %% "squants"     % V.squants
    val fs2        = "co.fs2"           %% "fs2-core"    % V.fs2

    val circeCore    = circe("core")
    val circeGeneric = circe("generic")
    val circeParser  = circe("parser")
    val circeRefined = circe("refined")

    val cirisCore    = ciris("ciris")
    val cirisEnum    = ciris("ciris-enumeratum")
    val cirisRefined = ciris("ciris-refined")

    val http4sDsl    = http4s("dsl")
    val http4sServer = http4s("ember-server")
    val http4sClient = http4s("ember-client")
    val http4sCirce  = http4s("circe")

    val monocleCore = "com.github.julien-truffaut" %% "monocle-core" % V.monocle

    val ironScalaCheck    = iron("iron-scalacheck")
    val ironCirce         = iron("iron-circe")
    val ironCats          = iron("iron-cats")
    val coreIron          = iron("iron")

    val log4cats        = "org.typelevel" %% "log4cats-slf4j" % V.log4cats
    val log4catsTesting = "org.typelevel" %% "log4cats-testing" % V.log4cats

    val javaxCrypto = "javax.xml.crypto" % "jsr105-api" % V.javaxCrypto

    val declineEffect = "com.monovore" %% "decline-effect" % V.declineEffect
    val apacheCodec = "commons-codec" % "commons-codec" % V.apacheCodec

    // Runtime
    val logback  = "ch.qos.logback" % "logback-classic" % V.logback

    // Test
    val catsLaws         = "org.typelevel"              %% "cats-laws"         % V.cats
    val log4catsNoOp     = "org.typelevel"              %% "log4cats-noop"     % V.log4cats
    val monocleLaw       = "com.github.julien-truffaut" %% "monocle-law"       % V.monocle
    val weaverCats       = "com.disneystreaming"        %% "weaver-cats"       % V.weaver
    val weaverDiscipline = "com.disneystreaming"        %% "weaver-discipline" % V.weaver
    val weaverScalaCheck = "com.disneystreaming"        %% "weaver-scalacheck" % V.weaver
    val specs2           = "org.specs2"                 %% "specs2-core"       % V.specs2
    val wiremock         = "com.github.tomakehurst"     %  "wiremock"          % V.wiremock
    val apacheCommon     = "commons-io"                 %  "commons-io"        % V.apacheCommon
    val scodecCats       = "org.scodec"                 %% "scodec-cats"       % V.scodecCats
  }

}
