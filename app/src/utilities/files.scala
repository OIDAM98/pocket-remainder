package utilities

import scala.util.Try
import scala.util.Success
import scala.util.Failure
import model.credentials.PocketCredentials

import model.json.decoders._
import io.circe._
import io.circe.parser._
import errors.PocketError
import errors.NoFileFound
import errors.NoAccessTokenFound
import errors.UnexpectedError
import errors.NoConsumerCodeFound
import model.credentials.PocketKey

object files {
  def readCredentials(
      filename: String
  ): Either[PocketError, PocketCredentials] = {
    val file = Try(os.read(os.pwd / filename))
    file match {
      case Success(value) =>
        decode[PocketCredentials](value).left.map { e =>
          val message = e.getMessage()
          if (message.contains("consumer_key"))
            NoConsumerCodeFound
          else if (message.contains("access_token"))
            NoAccessTokenFound
          else UnexpectedError(message)
        }

      case Failure(exception) => Left(NoFileFound)
    }
  }

  def readConsumerKey(filename: String): Either[PocketError, PocketKey] = {
    val file = Try(os.read(os.pwd / filename))
    file match {
      case Success(value) =>
        decode[PocketKey](value).left.map { e =>
          val message = e.getMessage()
          if (message.contains("consumer_key"))
            NoConsumerCodeFound
          else UnexpectedError(message)
        }

      case Failure(exception) => Left(NoFileFound)
    }
  }

  import model.json.encoders._
  import io.circe.syntax._

  def saveCredentials(
      filename: String,
      credentials: PocketCredentials
  ): Either[PocketError, Unit] =
    Try(
      os.write.over(os.pwd / filename, credentials.asJson.spaces2)
    ).toEither.left.map(e => UnexpectedError(e.getMessage()))

}
