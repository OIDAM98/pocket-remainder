package interpreters

import utilities.constants._
import sttp.client._
import io.circe._
import sttp.client.circe._
import sttp.model.Uri
import model.errors.PocketError
import model.responses.ConsumerKey
import model.responses.PocketItems
import model.responses.PocketAuth
import model.requests.RequestToken
import model.json.decoders._
import model.json.encoders._
import model.errors.UnexpectedError
import model.requests.RequestAccessToken
import model.credentials.PocketCredentials
import model.requests.PocketRequest
import cats.Applicative
import algebras.{Connection, Credentials}
import cats.effect.Sync
import cats.data.EitherT
import cats.implicits._

import scala.util.Try

final class SttpConnection[F[_]: Sync] private (
    consumerKey: String,
    files: Credentials[F]
)(implicit backend: HttpURLConnectionBackend)
    extends Connection[F] {

  private type ErrorWrapped[A] = EitherT[F, PocketError, A]

  def generateToken: F[Either[PocketError, PocketCredentials]] = {
    (for {
      code        <- EitherT(getRequestToken)
      _           <- EitherT(waitForAuth(code))
      token       <- EitherT(getAccessToken(code.code))
      credentials <- PocketCredentials(consumerKey, token.access_token).pure[ErrorWrapped]
      cred        <- EitherT(files.saveCredentials(credentials))
    } yield cred).value

  }

  def getRequestToken: F[Either[PocketError, ConsumerKey]] =
    Sync[F].pure {
      generateRequest[RequestToken, ConsumerKey](
        RequestToken(consumerKey, REDIRECT_URI),
        AUTH_ROUTE
      ).send.body.left.map(e => UnexpectedError(e.getMessage))
    }

  import utilities.constants.REDIRECT_URI

  import scala.io.StdIn.readLine

  def getAccessToken(
      code: String
  ): F[Either[PocketError, PocketAuth]] =
    Sync[F].pure {
      generateRequest[RequestAccessToken, PocketAuth](
        RequestAccessToken(consumerKey, code),
        AUTH_CODE_ROUTE
      ).send.body.left.map(e => UnexpectedError(e.getMessage))
    }

  def waitForAuth(
      code: ConsumerKey
  ): F[Either[PocketError, ConsumerKey]] =
    Sync[F].pure {
      Try {
        val redirectURL =
          s"https://getpocket.com/auth/authorize?request_token=${code.code}&redirect_uri=$REDIRECT_URI"

        println("Go to")
        println(s"\t $redirectURL")
        println("to authenticate the application.")

        readLine("Press any letter after the app has been authorized")
        code
      }.toEither.left.map(e => UnexpectedError(e.getMessage))
    }

  def getArticles(
      credentials: PocketCredentials,
      n: Int
  ): F[Either[PocketError, PocketItems]] =
    Applicative[F].pure {
      generateRequest[PocketRequest, PocketItems](
        credentials.toRequest(n),
        ARTICLES_ROUTE
      ).send.body.left.map(e => UnexpectedError(e.getMessage))
    }

  private def generateRequest[A, B: Decoder](body: A, path: Uri)(implicit
      view: A => BasicRequestBody
  ) =
    jsonRequest
      .post(path)
      .body(body)
      .response(asJson[B])
}

object SttpConnection {
  def make[F[_]: Sync](consumerKey: String, files: Credentials[F]) =
    Sync[F].delay(HttpURLConnectionBackend).map { implicit backend =>
      new SttpConnection[F](consumerKey, files)
    }
}
