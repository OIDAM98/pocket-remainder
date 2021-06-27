package core.algebras

import core.model.errors.PocketError
import core.model.responses.PocketArticle

trait NotificationSender[F[_]] {
  def send(articles: List[PocketArticle]): F[Either[PocketError, String]]
}
