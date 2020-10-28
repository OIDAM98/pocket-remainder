package core.algebras

import core.model.errors.PocketError
import core.model.responses.PocketArticle

trait MailService[F[_]] {
  def send(email: String, articles: List[PocketArticle]): F[Either[PocketError, String]]
}
