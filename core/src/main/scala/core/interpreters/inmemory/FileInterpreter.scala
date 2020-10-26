package core.interpreters.inmemory

import scala.util.Try
import scala.util.Success
import scala.util.Failure
import core.model.credentials.{PocketCredentials, PocketKey, PocketUseData}
import core.model.json.decoders._
import io.circe.parser._
import core.model.errors._
import core.algebras.Credentials
import cats._
import cats.implicits._
import cats.effect.Sync

final class FileInterpreter[F[_]: Sync] private (val filename: String) extends Credentials[F] {
  def readCredentials: F[Either[PocketError, PocketUseData]] =
    Sync[F]
      .delay(Try(os.read(os.pwd / filename)))
      .map {
        case Success(value) =>
          decode[PocketCredentials](value).leftMap { e =>
            val message = e.getMessage
            if (message.contains("consumer_key"))
              NoConsumerCodeFound
            else if (message.contains("access_token"))
              NoAccessTokenFound
            else UnexpectedError(message)
          }
        case Failure(_) => Either.left(NoFileFound)
      }
      .flatMap {
        case Left(value) =>
          value match {
            case NoAccessTokenFound => readConsumerKey
            case e                  => Sync[F].pure(Either.left(e))
          }
        case a => Sync[F].pure(a)
      }

  private def readConsumerKey: F[Either[PocketError, PocketUseData]] =
    Sync[F]
      .delay(Try(os.read(os.pwd / filename)))
      .map {
        case Success(value) =>
          decode[PocketKey](value).left.map { e =>
            val message = e.getMessage
            if (message.contains("consumer_key"))
              NoConsumerCodeFound
            else UnexpectedError(message)
          }

        case Failure(_) => Left(NoFileFound)
      }

  import core.model.json.encoders._
  import io.circe.syntax._

  def saveCredentials(
      credentials: PocketCredentials
  ): F[Either[PocketError, PocketCredentials]] =
    Sync[F]
      .delay(
        Try(
          os.write.over(
            os.pwd / filename,
            credentials.asJson.spaces2
          )
        )
      )
      .map(_.toEither.map(_ => credentials).leftMap(e => UnexpectedError(e.getMessage)))

}

object FileInterpreter {
  def apply[F[_]: Sync](filename: String): F[FileInterpreter[F]] =
    Applicative[F].pure(new FileInterpreter[F](filename))
}
