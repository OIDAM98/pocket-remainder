package core.model
import credentials._
import cron4s.expr.CronExpr
import cron4s.Cron

object configuration {

  final case class AppConfig(to_send: String, count: Int, size: Int)

  sealed trait NotificationCredentials
  final case class MailCredentials(email: String, password: String) extends NotificationCredentials {
    override def toString: String = s"MailCredentials($email, SECRET)"
  }
  final case class TelegramCredentials(token: String, chatId: Long)
      extends NotificationCredentials {
    override def toString: String = s"TelegramCredentials(SECRET, $chatId)"
  }

  final case class GlobalConfig(
      app_config: AppConfig,
      notification_config: NotificationCredentials,
      pocket: PocketCredentials,
      schedules: List[CronExpr]
  )

}
