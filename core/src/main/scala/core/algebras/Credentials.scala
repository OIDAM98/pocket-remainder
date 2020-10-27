package core.algebras

import core.model.errors.PocketError
import core.model.credentials.{CredentialsType, PocketUseData}

trait Credentials[F[_], A, B <: CredentialsType] {
  def readCredentials: F[Either[PocketError, A]]
  def saveCredentials(saveCredentials: B): F[Either[PocketError, B]]
}
