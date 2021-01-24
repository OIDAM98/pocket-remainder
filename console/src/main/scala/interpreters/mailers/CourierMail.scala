package interpreters.mailers

import cats.effect.Async
import core.algebras.MailService
import cats.implicits._
import core.model.credentials.MailCredentials
import core.model.responses
import courier._
import Defaults._
import core.model.errors.{PocketError, UnexpectedError}
import core.utilities.pocket

import scala.util.{Failure, Success}

final class CourierMail[F[_]: Async] private (credentials: MailCredentials) extends MailService[F] {
  private val mailer: F[Mailer] = Async[F].delay(
    Mailer("smtp.gmail.com", 465)
      .auth(true)
      .as(credentials.email, credentials.password)
      .ssl(true)()
  )
  def send(
      email: String,
      articles: List[responses.PocketArticle]
  ): F[Either[PocketError, String]] = {
    for {
      mail <- mailer
      envelope =
        Envelope
          .from(credentials.email.addr)
          .to(email.addr)
          .subject("Pocket Remainder for the week")
          .content(
            Multipart()
              .text("These are the articles for the week:\n\n")
              .html(pocket.createEmailBody(articles))
          )
      response <- Async[F].async { cb: (Either[Throwable, String] => Unit) =>
        mail(envelope).onComplete {
          case Success(_)         => cb(Right(s"Successfully sent message to $email!"))
          case Failure(exception) => cb(Left(exception))
        }
      }
    } yield response.asRight.orElse(UnexpectedError("Something went wrong sending the email!").asLeft)
  }

}

object CourierMail {
  def apply[F[_]: Async](credentials: MailCredentials): F[CourierMail[F]] =
    Async[F].delay(new CourierMail[F](credentials))
}
