import utilities._
import errors.NoConsumerCodeFound
import errors.UnexpectedError
import errors.NoAccessTokenFound
import errors.NoFileFound
import errors.PocketError
import model.responses.PocketItem

import model.json.encoders._
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import errors.ExpiredToken

object Main extends App {
  if (args.length == 0 || args.length > 3) {
    println(constants.messages.INIT_PRINT)
    sys.exit()
  }

  val filename = args(0)
  val count = args(1).toInt
  val size = args(2).toInt

  val pocket = new PocketAPI(filename, count, size)

  val credentials = files.readCredentials(filename)
  val articles: Either[PocketError, List[PocketItem]] = credentials match {
    case Right(c) => pocket.withCredentials(c)
    case Left(error) =>
      error match {
        case NoAccessTokenFound | ExpiredToken =>
          for {
            consumerKey <- files.readConsumerKey(filename)
            list <- pocket.withConsumerKey(consumerKey.consumer_key)
          } yield list
        case _ => Left(error)
      }
  }

  val processed = articles.map(_.map(_.toDomain))

  processed match {
    case Right(xs) => xs.foreach(x => println(x.asJson.spaces2))
    case Left(error) => error match {
      case ExpiredToken => println(constants.messages.EXPIRED_TOKEN); sys.exit
      case NoFileFound => println(constants.messages.NO_FILE_FOUND); sys.exit
      case NoConsumerCodeFound => println(constants.messages.NO_CONSUMER_KEY); sys.exit
      case NoAccessTokenFound => println(constants.messages.NO_TOKEN_FOUND); sys.exit
      case UnexpectedError(message) => println(s"Unexpected error:\n$message"); sys.exit
    } 
  }
}
