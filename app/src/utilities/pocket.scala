package utilities

import model.responses.PocketItem
import model.responses.ConsumerKey
import errors.PocketError
import scala.util.Try
import errors.UnexpectedError

object pocket {
  def fetchRandomArticles(list: List[PocketItem], max: Int): List[PocketItem] = {
    import scala.collection.mutable

    val used = mutable.Set.empty[Int]
    val articles = mutable.ListBuffer.empty[PocketItem]
    val random = scala.util.Random
    val length = list.length
    var counter = 0

    while (counter < max) {
      val i = random.nextInt(length)
      if (!used.contains(i)) {
        used += i
        articles += list(i)
        counter = counter + 1
      }
    }
    articles.toList
  }

  import constants.REDIRECT_URI
  import scala.io.StdIn.readLine
  def waitForAuth(code: ConsumerKey): Either[PocketError, Unit] =
    Try {
      val redirectURL =
        s"https://getpocket.com/auth/authorize?request_token=${code.code}&redirect_uri=${REDIRECT_URI}"
      
      println("Go to")
      println(s"\t $redirectURL")
      println("to authenticate the application.")

      readLine("Press any letter after the app has been authorized")
      ()
    }.toEither.left.map(e => UnexpectedError(e.getMessage()))
}
