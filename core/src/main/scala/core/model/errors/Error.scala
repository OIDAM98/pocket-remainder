package core.model.errors

sealed trait PocketError
case object NoFileFound extends PocketError
case object NoConsumerCodeFound extends PocketError
case object NoAccessTokenFound extends PocketError
case object ExpiredToken extends PocketError
case class UnexpectedError(message: String) extends PocketError
