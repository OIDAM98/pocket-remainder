package core.utilities

import java.time.format.DateTimeFormatter

import cats.effect.Sync
import core.model.responses.{PocketArticle, PocketItem}

object pocket {
  def fetchRandomArticles(
      list: List[PocketItem],
      max: Int
  ): List[PocketItem] = {
    import scala.collection.mutable

    val used     = mutable.Set.empty[Int]
    val articles = mutable.ListBuffer.empty[PocketItem]
    val random   = scala.util.Random
    val length   = list.length
    var counter  = 0

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

  import io.circe.syntax._
  import core.model.json.encoders.pocketArticle

  def printArticles[F[_]: Sync](articles: List[PocketArticle]): F[Unit] =
    Sync[F].delay {
      val jsons = articles.map(_.asJson.spaces2)
      val list  = jsons.mkString("\n")
      println(list)
    }

  import scalatags.Text.all._

  private val FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  // Return largest size of string found within articles
  private def getLargest(articles: List[PocketArticle]): Int =
    articles.foldLeft(0) {
      case (cmax, article) =>
        (article.pocket_url.length max cmax) max (article.title.length max cmax)
    }

  private def toHTMLFormat(article: PocketArticle, l: Int) =
    div(
      p(b("\tTitle: "), i(article.title)),
      p(b("\tURL: "), article.pocket_url),
      p(b("\tTime Added: "), article.time_added.format(FORMATTER)),
      p(b("\tWord Count: "), article.word_count),
      p("-" * l)
    )

  def createEmailBody(articles: List[PocketArticle]): String = {
    val largest = getLargest(articles)
    html(
      body(
        articles.map(a => toHTMLFormat(a, largest))
      )
    ).render
  }

  private def toMarkdownFormat(article: PocketArticle) =
    article match {
      case PocketArticle(_, _, title, _, _, time_added, word_count, pocket_url) =>
        raw"""
             |*$title*
             |_Read here:_ $pocket_url
             |*Time Added:* ${time_added.format(FORMATTER)}
             |*Word Count:* $word_count""".stripMargin
    }

  def createMarkdownBody(articles: List[PocketArticle]): String =
    articles.map(toMarkdownFormat).mkString("\n")


}
