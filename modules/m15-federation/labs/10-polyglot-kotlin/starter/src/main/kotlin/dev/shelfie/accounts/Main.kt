package dev.shelfie.accounts

import com.expediagroup.graphql.generator.SchemaGeneratorConfig
import com.expediagroup.graphql.generator.TopLevelObject
import com.expediagroup.graphql.generator.scalars.ID
import com.expediagroup.graphql.generator.toSchema
import com.sun.net.httpserver.HttpServer
import graphql.ExecutionInput
import graphql.GraphQL
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import tools.jackson.databind.json.JsonMapper

// Lab 15.10: today this is a plain GraphQL server in Kotlin. Make it Shelfie's accounts subgraph: the same API as
// the Java one (contract/subgraphs/.../accounts/accounts.graphqls), federated, composing with the others.

data class User(val id: ID, val name: String, val handle: String, val city: String?)

val USERS: Map<String, User> = listOf(
    "u1" to listOf("Asha Rao", "asha", "Bengaluru"), "u2" to listOf("Ben Okafor", "benok", "Lagos"),
    "u3" to listOf("Chloe Martin", "chloem", "Lyon"), "u4" to listOf("Dev Patel", "devp", "Leicester"),
    "u5" to listOf("Elena Petrova", "elenap", "Sofia"), "u6" to listOf("Farid Haddad", "farid", "Beirut"),
    "u7" to listOf("Grace Kim", "gracek", "Seoul"), "u8" to listOf("Hugo Silva", "hugos", "Porto"),
    "u9" to listOf("Ines Duarte", "inesd", "Lisbon"), "u10" to listOf("Jonas Berg", "jonasb", "Oslo"),
    "u11" to listOf("Kemi Adeyemi", "kemia", "Accra"), "u12" to listOf("Liu Wei", "liuw", "Chengdu"),
).associate { (id, p) -> id to User(ID(id), p[0], p[1], p[2]) }

class Query {
    fun user(id: ID): User? = USERS[id.value]
    fun users(): List<User> = USERS.values.toList()
}

fun main(args: Array<String>) {
    val port = args.firstOrNull()?.toInt() ?: 4001
    val schema = toSchema(SchemaGeneratorConfig(supportedPackages = listOf("dev.shelfie.accounts")), listOf(TopLevelObject(Query())))
    val graphQL = GraphQL.newGraphQL(schema).build()
    val json = JsonMapper.builder().build()
    val server = HttpServer.create(InetSocketAddress(port), 64)
    server.executor = Executors.newVirtualThreadPerTaskExecutor()
    server.createContext("/graphql") { ex ->
        @Suppress("UNCHECKED_CAST")
        val body = json.readValue(ex.requestBody.readAllBytes(), Map::class.java) as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val input = ExecutionInput.newExecutionInput(body["query"] as String)
            .variables((body["variables"] as Map<String, Any?>?) ?: emptyMap()).build()
        val bytes = json.writeValueAsBytes(graphQL.execute(input).toSpecification())
        ex.responseHeaders.set("Content-Type", "application/json")
        ex.sendResponseHeaders(200, bytes.size.toLong())
        ex.responseBody.use { it.write(bytes) }
    }
    server.start()
    println("accounts (Kotlin) on $port")
}
