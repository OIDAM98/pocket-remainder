package core.algebras

import core.model.errors.PocketError
import core.model.credentials.{PocketCredentials, PocketUseData}

trait Credentials[F[_]] {
  def readCredentials: F[Either[PocketError, PocketUseData]]
  def saveCredentials(saveCredentials: PocketCredentials): F[Either[PocketError, PocketCredentials]]
}
