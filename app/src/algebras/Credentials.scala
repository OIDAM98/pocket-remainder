package algebras

import model.errors.PocketError
import model.credentials.PocketCredentials
import model.credentials.PocketKey

trait Credentials[F[_]] {
  def readCredentials: F[Either[PocketError, PocketCredentials]]
  def readConsumerKey: F[Either[PocketError, PocketKey]]
  def saveCredentials(saveCredentials: PocketCredentials): F[Either[PocketError, PocketCredentials]]
}
