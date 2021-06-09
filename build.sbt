import Dependencies._

ThisBuild / scalaVersion := "2.13.3"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.odealva"
ThisBuild / organizationName := "odealva"

val CirceVersion      = "0.13.0"
val PureConfigVersion = "0.14.1"

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
      "org.typelevel"                %% "cats-core"                      % "2.2.0",
      "org.typelevel"                %% "cats-effect"                    % "2.2.0",
      "com.softwaremill.sttp.client" %% "core"                           % "2.2.9",
      "com.softwaremill.sttp.client" %% "circe"                          % "2.2.9",
      "com.softwaremill.sttp.client" %% "cats"                           % "2.2.9",
      "com.softwaremill.sttp.client" %% "async-http-client-backend-cats" % "2.2.9",
      "com.lihaoyi"                  %% "os-lib"                         % "0.7.1",
      "com.github.daddykotex"        %% "courier"                        % "2.0.0",
      "com.lihaoyi"                  %% "scalatags"                      % "0.9.2",
      "eu.timepit"                   %% "fs2-cron-core"                  % "0.2.2"
    ),
    libraryDependencies += scalaTest % Test
  )

lazy val console = (project in file("console"))
  .settings(
    name := "pocket-console-client",
    libraryDependencies ++= Seq(
      "com.github.pureconfig" %% "pureconfig"             % PureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-cats-effect" % PureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-cron4s"      % PureConfigVersion
    )
  )
  .dependsOn(core)

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
