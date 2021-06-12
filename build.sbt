ThisBuild / scalaVersion := "2.13.6"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.odealva"
ThisBuild / organizationName := "odealva"

val CirceVersion      = "0.13.0"
val PureConfigVersion = "0.16.0"
val Fs2CronVersion    = "0.2.2"
val SttpVersion       = "2.2.9"
val CatsVersion       = "2.2.0"
val CatsEffectVersion = "2.2.0"
val ScalatagsVersion  = "0.9.4"
val CourierVersion    = "2.0.0"
val OsLibVersion      = "0.7.8"

lazy val root = (project in file("."))
  .settings(
    name := "pocket-reminder"
  )
  .aggregate(core, console)

lazy val core = (project in file("core"))
  .settings(
    name := "pocket-library",
    libraryDependencies ++= Seq(
      "io.circe"                     %% "circe-core"                     % CirceVersion,
      "io.circe"                     %% "circe-generic"                  % CirceVersion,
      "io.circe"                     %% "circe-parser"                   % CirceVersion,
      "org.typelevel"                %% "cats-core"                      % CatsVersion,
      "org.typelevel"                %% "cats-effect"                    % CatsEffectVersion,
      "com.softwaremill.sttp.client" %% "core"                           % SttpVersion,
      "com.softwaremill.sttp.client" %% "circe"                          % SttpVersion,
      "com.softwaremill.sttp.client" %% "cats"                           % SttpVersion,
      "com.softwaremill.sttp.client" %% "async-http-client-backend-cats" % SttpVersion,
      "com.lihaoyi"                  %% "os-lib"                         % OsLibVersion,
      "com.github.daddykotex"        %% "courier"                        % CourierVersion,
      "com.lihaoyi"                  %% "scalatags"                      % ScalatagsVersion,
      "eu.timepit"                   %% "fs2-cron-core"                  % Fs2CronVersion,
      "com.github.pureconfig"        %% "pureconfig"                     % PureConfigVersion,
      "com.github.pureconfig"        %% "pureconfig-cron4s"              % PureConfigVersion
    )
  )

lazy val console = (project in file("console"))
  .settings(
    name := "pocket-console-client"
  )
  .dependsOn(core)

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
