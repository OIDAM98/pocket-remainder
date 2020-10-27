package core.algebras

import core.model.responses.PocketArticle

trait MailService[F[_]] {
  def send(articles: List[PocketArticle])
}
