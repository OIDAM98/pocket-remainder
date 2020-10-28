package core.utilities

import sttp.client._

object constants {
  val AUTH_ROUTE = uri"https://getpocket.com/v3/oauth/request"
  val AUTH_CODE_ROUTE = uri"https://getpocket.com/v3/oauth/authorize"
  val ARTICLES_ROUTE = uri"https://getpocket.com/v3/get"

  val REDIRECT_URI = "contentreminder://response"

  object messages {
    val INIT_PRINT: String = """
    |This application must be called in the following format:
    |      $app credFile mailFile max count
    |
    |credFile = json file containing Pocket consumer key and access code.
    |mailFile = json file containing Gmail email and password to send mail from.
    |max = total number of articles to fetch from Pocket.
    |count = number of random articles to obtain.
    """.stripMargin

    val EXPIRED_TOKEN: String = """The token that was being used has already expired.
    |Restart the application to obtain a new token.
    """.stripMargin

    val NO_TOKEN_FOUND: String = """No token was found inside the JSON file.
    |Restart the application to obtain a new token.
    """.stripMargin

    val NO_FILE_FOUND = "No JSON file found with the specified name."

    val NO_CONSUMER_KEY =
      "No consumer key was found inside the specified JSON file."

  }

  val jsonRequest: RequestT[Empty, Either[String, String], Nothing] = basicRequest.header("X-Accept", "application/json")
}
