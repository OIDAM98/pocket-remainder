package console

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import cats.data.EitherT
import cats.effect.{ContextShift, ExitCode, IO, IOApp}
import cats.implicits._
import core.model.errors.{
  ExpiredToken,
  NoAccessTokenFound,
  NoConsumerCodeFound,
  NoFileFound,
  PocketError,
  UnexpectedError
}
import core.model.responses
import core.utilities.{constants, pocket}
import interpreters.PocketService
import interpreters.http.SttpConnection
import interpreters.inmemory.{MailFileInterpreter, PocketFileInterpreter}
import interpreters.mailers.CourierMail
import cron4s.Cron
import fs2.Stream
import eu.timepit.fs2cron.schedule

import scala.concurrent.ExecutionContext

object Main extends IOApp {

  val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
  def getCurrentTime: String         = dateFormatter.format(LocalDateTime.now)

  def generateArticles(
      pocketFile: String,
      mailFile: String,
      toEmail: String,
      count: String,
      size: String
  ): IO[Either[PocketError, (String, List[responses.PocketArticle])]] =
    (for {
      pocketFile  <- EitherT.right(PocketFileInterpreter[IO](pocketFile))
      mailFile    <- EitherT.right(MailFileInterpreter[IO](mailFile))
      pocketCred  <- EitherT(pocketFile.readCredentials)
      mailCred    <- EitherT(mailFile.readCredentials)
      http        <- EitherT.right(SttpConnection[IO](pocketCred.consumer_key, pocketFile))
      service     <- EitherT.right(PocketService[IO](count.toInt, size.toInt, http))
      mailer      <- EitherT.right(CourierMail[IO](mailCred))
      randomItems <- EitherT(service.getRandomArticles(pocketCred))
      randomList = randomItems.map(_.toDomain)
      message <- EitherT(
        mailer
          .send(toEmail, randomList)
          .handleErrorWith(err =>
            IO(UnexpectedError(s"[ERROR - ${getCurrentTime()}]: \n" + err.getMessage).asLeft)
          )
      )
    } yield (message, randomList)).value

  def drainToConsole(
      articles: IO[Either[PocketError, (String, List[responses.PocketArticle])]]
  ): IO[Unit] =
    articles.flatMap {
      case Right((msg, lst)) =>
        IO(println(s"[${getCurrentTime()}] $msg")) >> pocket.printArticles[IO](lst) >> IO(
          println("===================\n")
        )
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

  implicit val ctxShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  override def run(args: List[String]): IO[ExitCode] = {
    if (args.isEmpty || args.length != 5)
      IO.raiseError(new IllegalAccessException(constants.messages.INIT_PRINT)).as(ExitCode(2))
    else {
      val pocketFile :: mailFile :: toEmail :: count :: size :: Nil = args
      //val timeToWait                                                = " 0 0 11 ? * SUN * "
      val timeToWait = "0 */2 * ? * *"
      Cron.parse(timeToWait) match {
        case Left(err) => IO(println(err.getMessage)).map(_ => ExitCode.Error)
        case Right(cronTask) =>
          for {
            _ <- schedule(
              List(
                cronTask -> Stream.eval(
                  drainToConsole(generateArticles(pocketFile, mailFile, toEmail, count, size))
                )
              )
            ).compile.drain
          } yield ExitCode.Success
      }

    }

  }
}
