package shelfie

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.normalizedCache
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.apollo.exception.ApolloNetworkException
import java.io.File
import kotlinx.coroutines.runBlocking
import shelfie.graphql.AddReviewMutation
import shelfie.graphql.ClubCardQuery
import shelfie.graphql.type.CreateReviewInput

/**
 * Lab 12.7: a phone-like client on the JVM. Each command is a fresh process (an app restart); the cache lives
 * in shelfie.db and queued mutations in outbox.txt, so both survive it.
 *   online | offline | review <bookId> <rating> <body> | sync
 */
fun main(args: Array<String>) = runBlocking {
  val url = System.getenv("SHELFIE_GRAPHQL") ?: "http://toxiproxy:8669/graphql"
  val client = ApolloClient.Builder()
    .serverUrl(url)
    .addHttpHeader("X-Shelfie-User", "u11")
    .normalizedCache(SqlNormalizedCacheFactory("jdbc:sqlite:shelfie.db"))
    .build()
  val outbox = File("outbox.txt")
  when (args.firstOrNull()) {
    "online" -> card(client, FetchPolicy.NetworkOnly, "network")
    "offline" -> card(client, FetchPolicy.CacheOnly, "cache")
    "review" -> {
      val (bookId, rating, body) = Triple(args[1], args[2].toInt(), args.drop(3).joinToString(" "))
      val r = client.mutation(AddReviewMutation(CreateReviewInput(bookId, rating, body))).execute()
      if (r.exception is ApolloNetworkException) {
        outbox.appendText("$bookId\t$rating\t$body\n")
        println("offline: review for $bookId queued (${outbox.readLines().size} in the outbox)")
      } else {
        println("sent: " + (r.data?.createReview?.review?.id ?: r.data?.createReview?.userErrors?.firstOrNull()?.code ?: r.exception))
      }
    }
    "sync" -> {
      val queued = if (outbox.exists()) outbox.readLines().filter { it.isNotBlank() } else emptyList()
      val left = mutableListOf<String>()
      for (line in queued) {
        val (bookId, rating, body) = line.split("\t", limit = 3)
        val r = client.mutation(AddReviewMutation(CreateReviewInput(bookId, rating.toInt(), body))).execute()
        val payload = r.data?.createReview
        when {
          r.exception != null -> { left += line; println("still offline: $bookId stays queued") }
          payload?.review != null -> println("synced: review ${payload.review.id} for $bookId, average now ${payload.review.book.averageRating}")
          // A replay after a lost response (lab 12.8): the server already has it.
          payload?.userErrors?.firstOrNull()?.code?.rawValue == "DUPLICATE" -> println("synced: $bookId was already on the server")
          else -> println("rejected: $bookId ${payload?.userErrors?.firstOrNull()?.message}")
        }
      }
      outbox.writeText(left.joinToString("") { "$it\n" })
      card(client, FetchPolicy.CacheOnly, "cache, after sync")
    }
    else -> println("usage: online | offline | review <bookId> <rating> <body> | sync")
  }
  client.close()
}

private suspend fun card(client: ApolloClient, policy: FetchPolicy, source: String) {
  val r = client.query(ClubCardQuery("c1")).fetchPolicy(policy).execute()
  val club = r.data?.club
  if (club == null) {
    println("[$source] no club card: ${r.exception?.message ?: "not cached"}")
    return
  }
  val book = club.currentBook
  println("[$source] ${club.name} (${club.memberCount} members), reading ${book?.title}: ${book?.averageRating}")
}
