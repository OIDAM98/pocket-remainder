package model

import java.time.LocalDateTime
import java.time.Instant
import java.time.ZoneId

object responses {
  private def generatePocketUrl(id: String) =
    s"https://app.getpocket.com/read/$id"

  case class ConsumerKey(code: String)
  case class PocketAuth(access_token: String, username: String)
  case class PocketItem(
      item_id: String,
      resolved_id: String,
      given_url: String,
      given_title: String,
      favorite: Int,
      status: Int,
      time_added: String,
      word_count: String,
      is_article: Int,
      has_image: Int,
      has_video: Int
  ) {
    def toDomain: PocketArticle = {
      val date = LocalDateTime.ofInstant(
        Instant.ofEpochSecond(time_added.toLong),
        ZoneId.systemDefault()
      )
      val url = (is_article, has_image, has_video) match {
        case (1, _, _) => generatePocketUrl(resolved_id)
        case (0, 2, _) => generatePocketUrl(resolved_id)
        case (0, _, 2) => generatePocketUrl(resolved_id)
        case _         => given_url
      }

      PocketArticle(
        item_id,
        given_url,
        given_title,
        favorite,
        status,
        date,
        word_count.toInt,
        url
      )
    }
  }
  case class PocketItems(
      status: Int,
      complete: Int,
      list: Map[String, PocketItem]
  )
  case class PocketArticle(
      id: String,
      url: String,
      title: String,
      favorite: Int,
      status: Int,
      time_added: LocalDateTime,
      word_count: Int,
      pocket_url: String
  )
}
