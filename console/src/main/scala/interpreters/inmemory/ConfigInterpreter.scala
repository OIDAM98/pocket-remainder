package interpreters.inmemory

import cats.effect.Sync
import core.algebras.Credentials
import core.model.configuration._
import core.model.errors.{PocketError, UnexpectedError}
import pureconfig.{ConfigFieldMapping, ConfigSource, ConfigWriter, KebabCase}
import cats.implicits._
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.{FieldCoproductHint, ProductHint}
import pureconfig.generic.auto._
import pureconfig.module.cron4s.cronExprConfigConvert

import scala.util.Try

final class ConfigInterpreter[F[_]: Sync] private (
    filename: Option[String]
) extends Credentials[F, GlobalConfig, GlobalConfig] {

  implicit def hint[A]: ProductHint[A] = ProductHint[A](ConfigFieldMapping(KebabCase, KebabCase))
  implicit val notificationConfHint: FieldCoproductHint[NotificationCredentials] =
    new FieldCoproductHint[NotificationCredentials]("type") {
      override def fieldValue(name: String): String = name.dropRight("Credentials".length)
    }

  private val file                   = filename.getOrElse("console/src/main/resources/application.conf")
  private val fromResources: Boolean = filename.isEmpty
  private val filePath               = os.Path(file, base = os.pwd)

  def readCredentials: F[Either[PocketError, GlobalConfig]] =
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
    ConfigSource.default.load[GlobalConfig]

  private val readFromFilename =
    ConfigSource.file(filePath.toNIO).load[GlobalConfig]

  def saveCredentials(saveCredentials: GlobalConfig): F[Either[PocketError, GlobalConfig]] = {
    Sync[F]
      .delay(
        Try(
          os.write.over(
            os.pwd / file,
            ConfigWriter[GlobalConfig].to(saveCredentials).render()
          )
        )
      )
      .map(_.toEither.map(_ => saveCredentials).leftMap(e => UnexpectedError(e.getMessage)))
  }
}

object ConfigInterpreter {
  def apply[F[_]: Sync](
      filename: Option[String]
  ): F[ConfigInterpreter[F]] =
    Sync[F].pure(new ConfigInterpreter[F](filename))
}
