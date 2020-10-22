package model

import model.requests.PocketRequest

object credentials {
  case class PocketKey(consumer_key: String)
  case class PocketCredentials(consumer_key: String, access_token: String) {
    def toRequest(count: Int): PocketRequest = PocketRequest(this.consumer_key, this.access_token, count = count)
  }
}
