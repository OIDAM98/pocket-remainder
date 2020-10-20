import model.responses._
import utilities._
import model.credentials.PocketCredentials
import errors.PocketError

class PocketAPI(filename: String, max: Int, size: Int) {

  def withConsumerKey(consumerkey: String): Either[PocketError, List[PocketItem]] = {
    for {
      credentials <- http.generateToken(filename, consumerkey)
      randomArticles <- withCredentials(credentials)
    } yield randomArticles
  }

  def withCredentials(credentials: PocketCredentials): Either[PocketError, List[PocketItem]] =
    for {
      articlesResponse <- http.getArticles(credentials, size)
      articles = articlesResponse.list.values.toList
      randomArticles = pocket.fetchRandomArticles(articles, max)
    } yield randomArticles
}
