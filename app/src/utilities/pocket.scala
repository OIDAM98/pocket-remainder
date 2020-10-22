package utilities

import model.responses.PocketItem
import model.responses.ConsumerKey
import errors.PocketError
import scala.util.Try
import errors.UnexpectedError
import cats.Applicative

object pocket {
  def fetchRandomArticles(
      list: List[PocketItem],
      max: Int
  ): List[PocketItem] = {
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
}
