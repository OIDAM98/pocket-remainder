package utilities

import constants._

import sttp.client._
import io.circe._
import sttp.client.circe._
import sttp.model.Uri
import errors.PocketError
import model.responses.ConsumerKey
import model.requests.RequestToken

import model.json.decoders._
import model.json.encoders._

import errors.UnexpectedError
import model.requests.RequestAccessToken
import model.responses.PocketAuth
import model.credentials.PocketCredentials
import model.requests.PocketRequest
import model.responses.PocketItems

object http {

  implicit val backend = sttp.client.HttpURLConnectionBackend()

  private def getRequestToken(
      consumerKey: String
  ): Either[PocketError, ConsumerKey] = {
    generateRequest[RequestToken, ConsumerKey](
      RequestToken(consumerKey, REDIRECT_URI),
      AUTH_ROUTE
    ).send.body.left.map(e => UnexpectedError(e.getMessage()))
  }

  private def getAccessToken(
      consumerKey: String,
      code: String
  ): Either[PocketError, PocketAuth] =
    generateRequest[RequestAccessToken, PocketAuth](
      RequestAccessToken(consumerKey, code),
      AUTH_CODE_ROUTE
    ).send.body.left.map(e => UnexpectedError(e.getMessage()))

  private def generateRequest[A, B: Decoder](body: A, path: Uri)(implicit
      view: A => BasicRequestBody
  ) =
    jsonRequest
      .post(path)
      .body(body)
      .response(asJson[B])

  def generateToken(
      filename: String,
      consumerKey: String
  ): Either[PocketError, PocketCredentials] =
    for {
      code <- getRequestToken(consumerKey)
      _ <- pocket.waitForAuth(code)
      token <- getAccessToken(consumerKey, code.code)
      credentials = PocketCredentials(consumerKey, token.access_token)
      _ <- files.saveCredentials(filename, credentials)
    } yield credentials

  def getArticles(
      credentials: PocketCredentials,
      n: Int
  ): Either[PocketError, PocketItems] =
    generateRequest[PocketRequest, PocketItems](
      credentials.toRequest(n),
      ARTICLES_ROUTE
    ).send.body.left.map(e => UnexpectedError(e.getMessage()))

}
