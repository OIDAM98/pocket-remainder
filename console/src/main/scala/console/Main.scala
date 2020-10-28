package console

import cats.data.EitherT
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import core.interpreters.PocketService
import core.interpreters.http.SttpConnection
import core.interpreters.inmemory.{MailFileInterpreter, PocketFileInterpreter}
import core.interpreters.mailers.CourierMail
import core.model.errors.{
  ExpiredToken,
  NoAccessTokenFound,
  NoConsumerCodeFound,
  NoFileFound,
  PocketError,
  UnexpectedError
}
import core.utilities.{constants, pocket}

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    if (args.isEmpty || args.length != 5)
      IO.raiseError(new IllegalAccessException(constants.messages.INIT_PRINT)).as(ExitCode(2))
    else {
      val pocketFile :: mailFile :: toEmail :: count :: size :: Nil = args

      val IOArticles = (for {
        pocketFile  <- EitherT.right(PocketFileInterpreter[IO](pocketFile))
        mailFile    <- EitherT.right(MailFileInterpreter[IO](mailFile))
        pocketCred  <- EitherT(pocketFile.readCredentials)
        mailCred    <- EitherT(mailFile.readCredentials)
        http        <- EitherT.right(SttpConnection[IO](pocketCred.consumer_key, pocketFile))
        service     <- EitherT.right(PocketService[IO](count.toInt, size.toInt, http))
        mailer      <- EitherT.right(CourierMail[IO](mailCred))
        randomItems <- EitherT(service.getRandomArticles(pocketCred))
        randomList  = randomItems.map(_.toDomain)
        message     <- EitherT(mailer.send(toEmail, randomList))
      } yield (message, randomList)).value

      IOArticles
        .flatMap {
          case Right((msg, lst)) => IO(println(msg)) >> pocket.printArticles[IO](lst)
          case Left(value: PocketError) =>
            IO {
              value match {
                case NoFileFound              => println(constants.messages.NO_FILE_FOUND)
                case NoConsumerCodeFound      => println(constants.messages.NO_CONSUMER_KEY)
                case NoAccessTokenFound       => println(constants.messages.NO_TOKEN_FOUND)
                case ExpiredToken             => println(constants.messages.EXPIRED_TOKEN)
                case UnexpectedError(message) => println(message)
              }
            }
        }
        .map(_ => ExitCode.Success)

    }

  }
}
