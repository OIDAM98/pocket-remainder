package core.algebras

import core.model.errors.PocketError
import core.model.credentials.{PocketCredentials, PocketUseData}

trait Credentials[F[_], A] {
  def readCredentials: F[Either[PocketError, A]]
  def saveCredentials(saveCredentials: PocketCredentials): F[Either[PocketError, PocketCredentials]]
}
