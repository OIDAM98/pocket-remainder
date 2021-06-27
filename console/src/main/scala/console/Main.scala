package console

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import cats.data.EitherT
import cats.effect.{ContextShift, ExitCode, IO, IOApp}
import cats.implicits._
import core.algebras.NotificationSender
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
import interpreters.senders.{CourierMailSender, TelegramSender}
import fs2.Stream
import eu.timepit.fs2cron.schedule

import scala.concurrent.ExecutionContext

object Main extends IOApp {

  val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

  def getCurrentTime(): String = dateFormatter.format(LocalDateTime.now)

  def generateArticles(conf: AppConfig, pocket: PocketCredentials)(
      notifSender: IO[NotificationSender[IO]]
  ): IO[Either[PocketError, (String, List[responses.PocketArticle])]] =
    (for {
      http        <- EitherT.right(SttpConnection[IO](pocket.consumer_key))
      service     <- EitherT.right(PocketService[IO](conf.count, conf.size, http))
      sender      <- EitherT.right(notifSender)
      randomItems <- EitherT(service.getRandomArticles(pocket))
      randomList = randomItems.map(_.toDomain)
      message <- EitherT(
        sender
          .send(randomList)
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

  def sendArticles(
      conf: AppConfig,
      credentials: NotificationCredentials,
      pocket: PocketCredentials
  ): IO[Unit] = {
    val send =
      generateArticles(
        conf,
        pocket
      ) _

    val sender =
      credentials match {
        case mail: MailCredentials =>
          CourierMailSender[IO](mail, conf.to_send)
        case telegram: TelegramCredentials =>
          TelegramSender[IO](telegram)
      }

    drainToConsole(send(sender))
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
            sendArticles(
              config.app_config,
              config.notification_config,
              config.pocket
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
