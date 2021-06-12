package interpreters.http

import cats.Applicative
import cats.data.EitherT
import cats.effect.{Concurrent, ContextShift, Sync}
import cats.implicits._
import core.algebras.{Connection, Credentials}
import core.model.credentials.{PocketCredentials, PocketUseData}
import core.model.errors.{PocketError, UnexpectedError}
import core.model.json.decoders._
import core.model.json.encoders._
import core.model.requests.{PocketRequest, RequestAccessToken, RequestToken}
import core.model.responses.{ConsumerKey, PocketAuth, PocketItems}
import core.utilities.constants._
import io.circe._
import sttp.client._
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.asynchttpclient.cats.AsyncHttpClientCatsBackend
import sttp.client.circe._
import sttp.model.Uri

import scala.util.Try

final class SttpConnection[F[_]: Sync] private (
    consumerKey: String
)(implicit val backend: SttpBackend[F, Nothing, WebSocketHandler])
    extends Connection[F] {

  private type ErrorWrapped[A] = EitherT[F, PocketError, A]

  def generateToken: F[Either[PocketError, PocketCredentials]] = {
    (for {
      code        <- EitherT(getRequestToken)
      _           <- EitherT(waitForAuth(code))
      token       <- EitherT(getAccessToken(code.code))
      credentials <- PocketCredentials(consumerKey, token.access_token).pure[ErrorWrapped]
    } yield credentials).value

  }

  def getRequestToken: F[Either[PocketError, ConsumerKey]] =
    generateRequest[RequestToken, ConsumerKey](
      RequestToken(consumerKey, REDIRECT_URI),
      AUTH_ROUTE
    ).send.map(_.body.leftMap(e => UnexpectedError(e.getMessage)))

  import core.utilities.constants.REDIRECT_URI
  import scala.io.StdIn.readLine

  def getAccessToken(
      code: String
  ): F[Either[PocketError, PocketAuth]] =
    generateRequest[RequestAccessToken, PocketAuth](
      RequestAccessToken(consumerKey, code),
      AUTH_CODE_ROUTE
    ).send.map(_.body.leftMap(e => UnexpectedError(e.getMessage)))

  def waitForAuth(
      code: ConsumerKey
  ): F[Either[PocketError, ConsumerKey]] =
    Sync[F].delay {
      Try {
        val redirectURL =
          s"https://getpocket.com/auth/authorize?request_token=${code.code}&redirect_uri=$REDIRECT_URI"

        println("Go to")
        println(s"\t $redirectURL")
        println("to authenticate the application.")

        readLine("Press any letter after the app has been authorized")
        code
      }.toEither.leftMap(e => UnexpectedError(e.getMessage))
    }

  def getArticles(
      credentials: PocketCredentials,
      n: Int
  ): F[Either[PocketError, PocketItems]] =
    generateRequest[PocketRequest, PocketItems](
      credentials.toRequest(n),
      ARTICLES_ROUTE
    ).send.map(_.body.left.map(e => UnexpectedError(e.getMessage)))

  private def generateRequest[A, B: Decoder](body: A, path: Uri)(implicit
      view: A => BasicRequestBody
  ) =
    jsonRequest
      .post(path)
      .body(body)
      .response(asJson[B])
}

object SttpConnection {
  def apply[F[_]: Concurrent](
      consumerKey: String
  )(implicit cs: ContextShift[F]): F[SttpConnection[F]] = {

    AsyncHttpClientCatsBackend[F]().flatMap {
      implicit backend: SttpBackend[F, Nothing, WebSocketHandler] =>
        Sync[F].pure(new SttpConnection[F](consumerKey))
    }
  }
}
