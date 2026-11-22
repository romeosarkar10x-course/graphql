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

@KeyDirective(fields = FieldSet("id"))
data class User(val id: ID, val name: String, @ShareableDirective val handle: String, val city: String?)

val USERS = listOf(
    User(ID("u1"), "Asha Rao", "asha", "Bengaluru"), User(ID("u2"), "Ben Okafor", "benok", "Lagos"),
).associateBy { it.id.value }

/** Pin the federation version: graphql-kotlin 11 links v2.13 by default, newer than some composers know. */
@LinkDirective(url = "https://specs.apollo.dev/federation/v2.9", `as` = "", `import` = [LinkImport("@key", ""), LinkImport("@shareable", "")])
class AccountsSchema

class Query {
    fun user(id: ID): User? = USERS[id.value]
    fun users(): List<User> = USERS.values.toList()
}

class UserResolver : FederatedTypeSuspendResolver<User> {
    override val typeName = "User"
    override suspend fun resolve(environment: DataFetchingEnvironment, representation: Map<String, Any>): User? =
        USERS[representation["id"].toString()]
}

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
