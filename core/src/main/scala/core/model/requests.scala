package model

object requests {
  case class RequestToken(consumer_key: String, redirect_uri: String)
  case class RequestAccessToken(consumer_key: String, code: String)
  case class PocketRequest(
      consumer_key: String,
      access_token: String,
      count: Int = 50,
      detailType: String = "simple",
      sort: String = "oldest"
  )
}
