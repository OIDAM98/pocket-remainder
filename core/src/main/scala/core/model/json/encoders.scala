package model.json

import model.requests._
import model.credentials._

import io.circe._
import io.circe.generic.semiauto._
import sttp.client.circe._
import model.responses.PocketArticle
import model.responses.PocketItem

object encoders {
  implicit val requesTokenEncoder: Encoder[RequestToken] = deriveEncoder
  implicit val requestAccessEncoder: Encoder[RequestAccessToken] = deriveEncoder
  implicit val pocketRequestEncoder: Encoder[PocketRequest] = deriveEncoder
  implicit val pocketCredentialsEncoder: Encoder[PocketCredentials] =
    deriveEncoder
  implicit val pockerArticle: Encoder[PocketArticle] = deriveEncoder
  implicit val pocketItem: Encoder[PocketItem] = deriveEncoder

}
