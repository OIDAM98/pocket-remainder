package console

import cats.data.EitherT
import cats.effect.{ExitCode, IO, IOApp}
import core.interpreters.PocketService
import core.interpreters.http.SttpConnection
import core.interpreters.inmemory.PocketFileInterpreter
import core.model.errors.{ExpiredToken, NoAccessTokenFound, NoConsumerCodeFound, NoFileFound, PocketError, UnexpectedError}
import core.utilities.{constants, pocket}

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    if (args.isEmpty || args.length > 3)
      IO.raiseError(new IllegalAccessException(constants.messages.INIT_PRINT)).as(ExitCode(2))
    else {
      val filename = args.head
      val count    = args(1).toInt
      val size     = args(2).toInt

      val articles = (for {
        files       <- EitherT.right(PocketFileInterpreter[IO](filename))
        credentials <- EitherT(files.readCredentials)
        http <- EitherT.right(SttpConnection[IO](credentials.consumer_key, files))
        service    <- EitherT.right(PocketService[IO](count, size, http))
        randomList <- EitherT(service.getRandomArticles(credentials))
      } yield randomList.map(_.toDomain)).value

      articles
        .flatMap {
          case Right(lst) => pocket.printArticles[IO](lst)
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
