package core.interpreters

import cats.data.EitherT
import cats.effect.Sync
import core.algebras.Connection
import core.model.credentials.{PocketCredentials, PocketKey, PocketUseData}
import core.model.errors.PocketError
import core.model.responses._
import core.utilities._

final class PocketService[F[_]: Sync] private (max: Int, size: Int, http: Connection[F]) {

  def getRandomArticles(login: PocketUseData): F[Either[PocketError, List[PocketItem]]] = {
    login match {
      case _: PocketKey            => withConsumerKey
      case cred: PocketCredentials => withCredentials(cred).value
    }
  }

  private def withConsumerKey: F[Either[PocketError, List[PocketItem]]] = {
    (for {
      credentials    <- EitherT(http.generateToken)
      randomArticles <- withCredentials(credentials)
    } yield randomArticles).value
  }

  private def withCredentials(
      credentials: PocketCredentials
  ): EitherT[F, PocketError, List[PocketItem]] =
    for {
      articlesResponse <- EitherT(http.getArticles(credentials, max))
      articles       = articlesResponse.list.values.toList
      randomArticles = pocket.fetchRandomArticles(articles, size)
    } yield randomArticles
}

object PocketService {
  def apply[F[_]: Sync](max: Int, size: Int, http: Connection[F]): F[PocketService[F]] =
    Sync[F].pure(new PocketService[F](max, size, http))
}
