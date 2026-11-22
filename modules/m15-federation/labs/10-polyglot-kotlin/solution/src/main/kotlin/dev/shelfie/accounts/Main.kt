package dev.shelfie.accounts

import com.expediagroup.graphql.generator.TopLevelObject
import com.expediagroup.graphql.generator.federation.FederatedSchemaGeneratorConfig
import com.expediagroup.graphql.generator.federation.FederatedSchemaGeneratorHooks
import com.expediagroup.graphql.generator.federation.directives.FieldSet
import com.expediagroup.graphql.generator.federation.directives.KeyDirective
import com.expediagroup.graphql.generator.federation.directives.LinkDirective
import com.expediagroup.graphql.generator.federation.directives.LinkImport
import com.expediagroup.graphql.generator.federation.directives.ShareableDirective
import com.expediagroup.graphql.generator.federation.execution.FederatedTypeSuspendResolver
import com.expediagroup.graphql.generator.federation.toFederatedSchema
import com.expediagroup.graphql.generator.scalars.ID
import com.sun.net.httpserver.HttpServer
import graphql.ExecutionInput
import graphql.GraphQL
import graphql.schema.DataFetchingEnvironment
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import tools.jackson.databind.json.JsonMapper

/** A Shelfie reader. The accounts team owns who people are; other subgraphs point at users by id. */
@KeyDirective(fields = FieldSet("id"))
data class User(val id: ID, val name: String, @ShareableDirective val handle: String, val city: String?)

val USERS: Map<String, User> = listOf(
    "u1" to listOf("Asha Rao", "asha", "Bengaluru"), "u2" to listOf("Ben Okafor", "benok", "Lagos"),
    "u3" to listOf("Chloe Martin", "chloem", "Lyon"), "u4" to listOf("Dev Patel", "devp", "Leicester"),
    "u5" to listOf("Elena Petrova", "elenap", "Sofia"), "u6" to listOf("Farid Haddad", "farid", "Beirut"),
    "u7" to listOf("Grace Kim", "gracek", "Seoul"), "u8" to listOf("Hugo Silva", "hugos", "Porto"),
    "u9" to listOf("Ines Duarte", "inesd", "Lisbon"), "u10" to listOf("Jonas Berg", "jonasb", "Oslo"),
    "u11" to listOf("Kemi Adeyemi", "kemia", "Accra"), "u12" to listOf("Liu Wei", "liuw", "Chengdu"),
).associate { (id, p) -> id to User(ID(id), p[0], p[1], p[2]) }

class Query {
    /** The signed-in reader: the user named by the X-Shelfie-User header, null without one. */
    fun me(env: DataFetchingEnvironment): User? = env.graphQlContext.get<String>("userId")?.let { USERS[it] }
    fun user(id: ID): User? = USERS[id.value]
    fun users(): List<User> = USERS.values.toList()
}

/** _entities for User: one call per representation; the data is in memory, so that costs nothing. */
class UserResolver : FederatedTypeSuspendResolver<User> {
    override val typeName = "User"
    override suspend fun resolve(environment: DataFetchingEnvironment, representation: Map<String, Any>): User? =
        USERS[representation["id"].toString()]
}

/**
 * Pin the federation version. graphql-kotlin 11 links federation v2.13 by default; the course's composer
 * (The Guild's, 0.27) knows v2.0 to v2.9 and refuses anything newer, as would every other subgraph's tooling
 * that has not caught up. A polyglot graph moves at the speed of its slowest tool.
 */
@LinkDirective(url = "https://specs.apollo.dev/federation/v2.9", `as` = "", `import` = [LinkImport("@key", ""), LinkImport("@shareable", "")])
class AccountsSchema

fun main(args: Array<String>) {
    val port = args.firstOrNull()?.toInt() ?: 4001
    val config = FederatedSchemaGeneratorConfig(
        supportedPackages = listOf("dev.shelfie.accounts"),
        hooks = FederatedSchemaGeneratorHooks(listOf(UserResolver())),
    )
    val schema = toFederatedSchema(config, queries = listOf(TopLevelObject(Query())), schemaObject = TopLevelObject(AccountsSchema()))
    val graphQL = GraphQL.newGraphQL(schema).build()
    val json = JsonMapper.builder().build()
    val server = HttpServer.create(InetSocketAddress(port), 64)
    server.executor = Executors.newVirtualThreadPerTaskExecutor()
    server.createContext("/graphql") { ex ->
        @Suppress("UNCHECKED_CAST")
        val body = json.readValue(ex.requestBody.readAllBytes(), Map::class.java) as Map<String, Any?>
        val user = ex.requestHeaders.getFirst("X-Shelfie-User")?.takeIf { it in USERS }
        @Suppress("UNCHECKED_CAST")
        val input = ExecutionInput.newExecutionInput(body["query"] as String)
            .operationName(body["operationName"] as String?)
            .variables((body["variables"] as Map<String, Any?>?) ?: emptyMap())
            .graphQLContext(if (user != null) mapOf("userId" to user) else emptyMap())
            .build()
        val bytes = json.writeValueAsBytes(graphQL.execute(input).toSpecification())
        ex.responseHeaders.set("Content-Type", "application/json")
        ex.sendResponseHeaders(200, bytes.size.toLong())
        ex.responseBody.use { it.write(bytes) }
    }
    server.start()
    println("accounts (Kotlin, graphql-kotlin) on $port")
}
