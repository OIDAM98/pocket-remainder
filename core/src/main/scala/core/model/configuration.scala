package core.model
import credentials._
import cron4s.expr.CronExpr

object configuration {

  final case class AppConfig(to_send: String, count: Int, size: Int)

  sealed trait NotificationCredentials
  final case class MailCredentials(email: String, password: String) extends NotificationCredentials
  final case class TelegramCredentials(smt: String)                 extends NotificationCredentials

  final case class GlobalConfig(
      app_config: AppConfig,
      notification_config: NotificationCredentials,
      pocket: PocketCredentials,
      schedule: CronExpr
  )

}
