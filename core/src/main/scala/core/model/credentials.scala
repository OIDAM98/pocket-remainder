package core.model

import core.model.requests.PocketRequest

object credentials {

  sealed trait PocketUseData {
    val consumer_key: String
  }
  final case class PocketKey(override val consumer_key: String) extends PocketUseData
  final case class PocketCredentials(override val consumer_key: String, access_token: String)
      extends PocketUseData {
    override def toString: String = s"PocketCredentials(SECRET, SECRET)"
    def toRequest(count: Int): PocketRequest =
      PocketRequest(this.consumer_key, this.access_token, count = count)
  }
}
