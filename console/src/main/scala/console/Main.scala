package console

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
import core.utilities.{constants, pocket => utils}
import interpreters.PocketService
import interpreters.http.SttpConnection
import interpreters.inmemory.ConfigInterpreter
import interpreters.senders.{CourierMailSender, TelegramSender}
import fs2.Stream
import eu.timepit.fs2cron.schedule

import scala.concurrent.ExecutionContext

object Main extends IOApp {

  private def drainToConsole(
      articles: IO[Either[PocketError, (String, List[responses.PocketArticle])]]
  ): IO[Unit] =
    articles.flatMap {
      case Right((msg, lst)) =>
        utils.logMsg[IO](msg) *>
          utils.printArticles[IO](lst)

      case Left(value: PocketError) =>
        value match {
          case NoFileFound              => utils.logErr[IO](constants.messages.NO_FILE_FOUND)
          case NoConsumerCodeFound      => utils.logErr[IO](constants.messages.NO_CONSUMER_KEY)
          case NoAccessTokenFound       => utils.logErr[IO](constants.messages.NO_TOKEN_FOUND)
          case ExpiredToken             => utils.logErr[IO](constants.messages.EXPIRED_TOKEN)
          case UnexpectedError(message) => utils.logErr[IO](s"Unexpected Error - $message")
        }

    }

  def generateArticles(
      pocket: PocketCredentials,
      service: PocketService[IO],
      sender: NotificationSender[IO]
  ): IO[Either[PocketError, (String, List[responses.PocketArticle])]] =
    (for {
      randomItems <- EitherT {
        service.getRandomArticles(pocket).flatTap {
          case Right(value) => utils.logMsg[IO](raw"""Attempting to send the following articles:
                 |${value.map(_.toString).mkString("\n")}
                 |""".stripMargin)
          case Left(value)  => utils.logErr[IO](value.getMessage)
        }
      }
      randomList = randomItems.map(_.toDomain)
      message <- EitherT(
        sender
          .send(randomList)
          .handleErrorWith { err =>
            IO(UnexpectedError(raw"""
                   |[ERROR - ${utils.getCurrentTime()}]:
                   |${err.getMessage}
                   |${err.getStackTrace.mkString("\n")}
                   |""".stripMargin).asLeft)
          }
      )
    } yield (message, randomList)).value

  def sendArticles(
      pocket: PocketCredentials,
      service: PocketService[IO],
      sender: NotificationSender[IO]
  ): IO[Unit] =
    drainToConsole(generateArticles(pocket, service, sender))

  implicit val ctxShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  override def run(args: List[String]): IO[ExitCode] = {

    val confFile = sys.env.get("CONFIG_PATH") <+> args.headOption
    val streamEt = (for {
      confReader <- EitherT.right[PocketError](ConfigInterpreter[IO](confFile))
      config     <- EitherT(confReader.readCredentials)
      http       <- EitherT.right[PocketError](SttpConnection[IO](config.pocket.consumer_key))
      service <- EitherT.right[PocketError](
        PocketService[IO](config.app_config.count, config.app_config.size, http)
      )
      sender <- EitherT.right[PocketError] {
        config.notification_config match {
          case mail: MailCredentials =>
            CourierMailSender[IO](mail, config.app_config.to_send)
          case telegram: TelegramCredentials =>
            TelegramSender[IO](telegram)
        }
      }
      stream = schedule(
        config.schedules.map { s =>
          s -> Stream
            .eval(
              sendArticles(
                config.pocket,
                service,
                sender
              )
            )
        }
      )

    } yield stream).value

    streamEt.flatMap {
      case Left(err)     => utils.logErr[IO](err.toString) *> IO(ExitCode.Error)
      case Right(stream) => stream.compile.drain *> IO(ExitCode.Success)
    }

  }
}
