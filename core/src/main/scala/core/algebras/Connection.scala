package core.algebras

import core.model.errors.PocketError
import core.model.responses.ConsumerKey
import core.model.responses.PocketAuth
import core.model.credentials.PocketCredentials
import core.model.responses.PocketItems

trait Connection[F[_]] {
  def getRequestToken: F[Either[PocketError, ConsumerKey]]
  def getAccessToken(code: String): F[Either[PocketError, PocketAuth]]
  def generateToken: F[Either[PocketError, PocketCredentials]]
  def getArticles(
      credentials: PocketCredentials,
      n: Int
  ): F[Either[PocketError, PocketItems]]
}
