package shelfie

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.normalizedCache
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory
import java.io.File
import kotlinx.coroutines.runBlocking
import shelfie.graphql.ClubCardQuery

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
  @Suppress("UNUSED_VARIABLE") val outbox = File("outbox.txt")
  when (args.firstOrNull()) {
    "online" -> card(client, FetchPolicy.NetworkOnly, "network")
    "offline" -> card(client, FetchPolicy.CacheOnly, "cache")
    "review" -> {
      // TODO: send createReview; when the network is unreachable, queue it in outbox.txt instead of failing.
      println("TODO: review")
    }
    "sync" -> {
      // TODO: replay outbox.txt in order, keep what still fails, then show the card from the cache.
      println("TODO: sync")
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
