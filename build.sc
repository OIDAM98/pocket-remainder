import mill._, scalalib._

object app extends ScalaModule {
  def scalaVersion = "2.13.2"
  def ivyDeps =
    Agg(
      ivy"com.softwaremill.sttp.client::circe:2.2.9",
      ivy"io.circe::circe-core:0.13.0",
      ivy"io.circe::circe-generic:0.13.0",
      ivy"com.lihaoyi::os-lib:0.7.1"
    )
  object test extends Tests {
    def testFrameworks = Seq("utest.runner.Framework")

    def ivyDeps =
      Agg(
        ivy"com.lihaoyi::utest::0.7.4",
      )
  }
}
