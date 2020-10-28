package core.model.json

import core.model.responses._
import core.model.credentials._
import io.circe._
import io.circe.generic.semiauto._
import sttp.client.circe._

object decoders {
  implicit val keyDecoder: Decoder[ConsumerKey]             = deriveDecoder
  implicit val pocketAuthDecoder: Decoder[PocketAuth]       = deriveDecoder
  implicit val pocketArticleDecoder: Decoder[PocketArticle] = deriveDecoder
  implicit val pocketItemDecoder: Decoder[PocketItem]       = deriveDecoder
  implicit val pocketItemsDecoder: Decoder[PocketItems]     = deriveDecoder
  implicit val pocketCredentialsDecoder: Decoder[PocketCredentials] =
    deriveDecoder
  implicit val consumerDecoder: Decoder[PocketKey]              = deriveDecoder
  implicit val mailCredentialsDecoder: Decoder[MailCredentials] = deriveDecoder

}
