package core.model

import core.model.requests.PocketRequest
import cron4s.expr.CronExpr

object credentials {
  sealed trait CredentialsType
  sealed abstract class PocketUseData(val consumer_key: String)
  final case class PocketKey(override val consumer_key: String) extends PocketUseData(consumer_key)
  final case class PocketCredentials(override val consumer_key: String, access_token: String)
      extends PocketUseData(consumer_key)
      with CredentialsType {
    def toRequest(count: Int): PocketRequest =
      PocketRequest(this.consumer_key, this.access_token, count = count)
  }
  final case class MailCredentials(email: String, password: String) extends CredentialsType
  final case class AppMailConf(mail: MailCredentials, pocket: PocketUseData, schedule: CronExpr) extends CredentialsType
}
