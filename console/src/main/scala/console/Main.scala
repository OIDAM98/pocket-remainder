package console

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import cats.data.EitherT
import cats.effect.{ContextShift, ExitCode, IO, IOApp}
import cats.implicits._
import core.model.configuration._
import core.model.credentials.PocketCredentials
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
import interpreters.inmemory.ConfigInterpreter
import interpreters.mailers.CourierMail
import fs2.Stream
import eu.timepit.fs2cron.schedule

import scala.concurrent.ExecutionContext

object Main extends IOApp {

  val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

  def getCurrentTime(): String = dateFormatter.format(LocalDateTime.now)

  def generateArticles(
      conf: AppConfig,
      mail: MailCredentials,
      pocket: PocketCredentials
  ): IO[Either[PocketError, (String, List[responses.PocketArticle])]] =
    (for {
      http        <- EitherT.right(SttpConnection[IO](pocket.consumer_key))
      service     <- EitherT.right(PocketService[IO](conf.count, conf.size, http))
      mailer      <- EitherT.right(CourierMail[IO](mail))
      randomItems <- EitherT(service.getRandomArticles(pocket))
      randomList = randomItems.map(_.toDomain)
      message <- EitherT(
        mailer
          .send(conf.to_send, randomList)
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

    val confFile = args.headOption
    //val timeToWait                                                = " 0 0 11 ? * SUN * "
    val streamEt = (for {
      confReader <- EitherT.right(ConfigInterpreter[IO](confFile))
      config     <- EitherT(confReader.readCredentials)
      stream = schedule(
        List(
          config.schedule -> Stream.eval(
            drainToConsole(
              generateArticles(
                config.app_config,
                config.notification_config.asInstanceOf[MailCredentials],
                config.pocket
              )
            )
          )
        )
      )

    } yield stream).value

    streamEt.flatMap {
      case Left(err)     => IO.delay(println(s"[${getCurrentTime()}]: $err")) *> IO(ExitCode.Error)
      case Right(stream) => stream.compile.drain *> IO(ExitCode.Success)
    }

  }
}
