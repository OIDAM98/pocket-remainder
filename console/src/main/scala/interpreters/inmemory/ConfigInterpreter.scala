package interpreters.inmemory

import cats.effect.Sync
import core.algebras.Credentials
import core.model.credentials._
import core.model.errors.{PocketError, UnexpectedError}
import pureconfig.{ConfigFieldMapping, ConfigSource, ConfigWriter, KebabCase}
import cats.implicits._
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._
import pureconfig.module.cron4s.cronExprConfigConvert

import scala.util.Try

final class AppMailInterpreter[F[_]: Sync] private (
    filename: String
) extends Credentials[F, AppMailConf, AppMailConf] {

  implicit val hint                  = ProductHint[PocketCredentials](ConfigFieldMapping(KebabCase, KebabCase))
  private val fromResources: Boolean = if (filename == "") true else false
  private val filePath = os.Path(filename, base = os.pwd )

  def readCredentials: F[Either[PocketError, AppMailConf]] =
    Sync[F].delay {
      val configRead =
        if (fromResources)
          readFromResources
        else
          readFromFilename

      configRead
        .leftMap { (e: ConfigReaderFailures) =>
          println(e.prettyPrint(2))
          UnexpectedError(e.prettyPrint(2))
        }
    }

  private val readFromResources =
    ConfigSource.default.load[AppMailConf]

  private val readFromFilename =
    ConfigSource.file(filePath.toNIO).load[AppMailConf]

  def saveCredentials(saveCredentials: AppMailConf): F[Either[PocketError, AppMailConf]] = {
    Sync[F]
      .delay(
        Try(
          os.write.over(
            os.pwd / filename,
            ConfigWriter[AppMailConf].to(saveCredentials).render()
          )
        )
      )
      .map(_.toEither.map(_ => saveCredentials).leftMap(e => UnexpectedError(e.getMessage)))
  }
}

object AppMailInterpreter {
  def apply[F[_]: Sync](
      filename: String = ""
  ): F[AppMailInterpreter[F]] =
    Sync[F].pure(new AppMailInterpreter[F](filename))
}
