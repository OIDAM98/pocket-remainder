package core.algebras

import core.model.errors.PocketError

trait Credentials[F[_], A, B <: A] {
  def readCredentials: F[Either[PocketError, A]]
  def saveCredentials(saveCredentials: B): F[Either[PocketError, B]]
}
