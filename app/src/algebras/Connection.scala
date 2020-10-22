package algebras

import model.errors.PocketError
import model.responses.ConsumerKey
import model.responses.PocketAuth
import model.credentials.PocketCredentials
import model.responses.PocketItems

trait Connection[F[_]] {
  def getRequestToken: F[Either[PocketError, ConsumerKey]]
  def getAccessToken(code: String): F[Either[PocketError, PocketAuth]]
  def generateToken: F[Either[PocketError, PocketCredentials]]
  def getArticles(
      credentials: PocketCredentials,
      n: Int
  ): F[Either[PocketError, PocketItems]]
}
