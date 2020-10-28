package core.model.json

import core.model.requests._
import core.model.credentials._

import io.circe._
import io.circe.generic.semiauto._
import sttp.client.circe._
import core.model.responses.PocketArticle
import core.model.responses.PocketItem

object encoders {
  implicit val requestTokenEncoder: Encoder[RequestToken]        = deriveEncoder
  implicit val requestAccessEncoder: Encoder[RequestAccessToken] = deriveEncoder
  implicit val pocketRequestEncoder: Encoder[PocketRequest]      = deriveEncoder
  implicit val pocketCredentialsEncoder: Encoder[PocketCredentials] =
    deriveEncoder
  implicit val pocketArticle: Encoder[PocketArticle]            = deriveEncoder
  implicit val pocketItem: Encoder[PocketItem]                  = deriveEncoder
  implicit val mailCredentialsEncoder: Encoder[MailCredentials] = deriveEncoder

}
