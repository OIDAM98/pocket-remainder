package interpreters.inmemory

import cats.implicits._
import cats.effect.Sync
import core.algebras.Credentials
import core.model.credentials.MailCredentials
import core.model.errors.{NoFileFound, PocketError, UnexpectedError}
import io.circe.parser._
import core.model.json.decoders._
import core.model.json.encoders._
import io.circe.syntax.EncoderOps

import scala.util.{Failure, Success, Try}

final class MailFileInterpreter[F[_]: Sync] private (filename: String)
    extends Credentials[F, MailCredentials, MailCredentials] {
  def readCredentials: F[Either[PocketError, MailCredentials]] =
    Sync[F].delay(Try(os.read(os.pwd / filename))).map {
      case Success(value) =>
        decode[MailCredentials](value).leftMap(e => UnexpectedError(e.getMessage))
      case Failure(_) => Either.left(NoFileFound)
    }

  def saveCredentials(
      saveCredentials: MailCredentials
  ): F[Either[PocketError, MailCredentials]] =
    Sync[F]
      .delay(
        Try(
          os.write.over(
            os.pwd / filename,
            saveCredentials.asJson.spaces2
          )
        )
      )
      .map(_.toEither.map(_ => saveCredentials).leftMap(e => UnexpectedError(e.getMessage)))
}

object MailFileInterpreter {
  def apply[F[_]: Sync](filename: String): F[MailFileInterpreter[F]] =
    Sync[F].pure(
      new MailFileInterpreter[F](filename)
    )
}
