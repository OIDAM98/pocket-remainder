package interpreters.inmemory

import scala.util.Try
import scala.util.Success
import scala.util.Failure
import model.credentials.PocketCredentials

import model.json.decoders._
import io.circe.parser._
import model.errors._
import model.credentials.PocketKey
import algebras.Credentials
import cats._

final class FileInterpreter[F[_]: Applicative] private (val filename: String)
    extends Credentials[F] {
  def readCredentials: F[Either[PocketError, PocketCredentials]] = {
    val file = Try(os.read(os.pwd / filename))
    Applicative[F].pure {
      file match {
        case Success(value) =>
          decode[PocketCredentials](value).left.map { e =>
            val message = e.getMessage
            if (message.contains("consumer_key"))
              NoConsumerCodeFound
            else if (message.contains("access_token"))
              NoAccessTokenFound
            else UnexpectedError(message)
          }

        case Failure(_) => Left(NoFileFound)
      }
    }
  }

  def readConsumerKey: F[Either[PocketError, PocketKey]] = {
    val file = Try(os.read(os.pwd / filename))

    Applicative[F].pure {

      file match {
        case Success(value) =>
          decode[PocketKey](value).left.map { e =>
            val message = e.getMessage
            if (message.contains("consumer_key"))
              NoConsumerCodeFound
            else UnexpectedError(message)
          }

        case Failure(_) => Left(NoFileFound)
      }
    }
  }

  import model.json.encoders._
  import io.circe.syntax._

  def saveCredentials(
      credentials: PocketCredentials
  ): F[Either[PocketError, PocketCredentials]] =
    Applicative[F].pure {
      Try(
        os.write.over(
          os.pwd / filename,
          credentials.asJson.spaces2
        )
      ).toEither
        .map(_ => credentials)
        .left
        .map(e => UnexpectedError(e.getMessage))
    }

}

object FileInterpreter {
  def make[F[_]: Applicative](filename: String) =
    new FileInterpreter[F](filename)
}
