package interpreters.senders

import cats.effect.{Async, ConcurrentEffect}
import core.algebras.NotificationSender
import cats.implicits._
import core.model.configuration.TelegramCredentials
import core.model.responses
import core.model.errors.{PocketError, UnexpectedError}
import core.utilities.pocket
import canoe.api._
import canoe.syntax._
import canoe.methods.messages.SendMessage
import canoe.models.ParseMode.Markdown

final class TelegramSender[F[_]: Async: ConcurrentEffect] private (
    credentials: TelegramCredentials
) extends NotificationSender[F] {
  private val client = TelegramClient.global[F](credentials.token)

  private def sendMessage[F[_]: TelegramClient](text: String) =
    SendMessage(credentials.chatId, text, parseMode = Some(Markdown)).call

  def send(articles: List[responses.PocketArticle]): F[Either[PocketError, String]] =
    client
      .use { implicit cli =>
        sendMessage(pocket.createMarkdownBody(articles))
      }
      .map(msg =>
        s"Successfully sent message to ${credentials.chatId} with msg:\n ${msg.messageId}".asRight
          .orElse(UnexpectedError("Something went wrong sending the email!").asLeft)
      )
}

object TelegramSender {
  def apply[F[_]: Async: ConcurrentEffect](
      credentials: TelegramCredentials
  ): F[TelegramSender[F]] = Async[F].delay(new TelegramSender(credentials))
}
