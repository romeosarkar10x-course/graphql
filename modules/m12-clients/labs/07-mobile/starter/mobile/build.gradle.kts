plugins {
  kotlin("jvm") version "2.4.10"
  id("com.apollographql.apollo") version "5.2.0"
  application
}

dependencies {
  implementation("com.apollographql.apollo:apollo-runtime:5.2.0")
  // The normalised cache, persisted in SQLite: what a phone keeps across app restarts and airplane mode
  implementation("com.apollographql.apollo:apollo-normalized-cache:5.2.0")
  implementation("com.apollographql.apollo:apollo-normalized-cache-sqlite:5.2.0")
}

kotlin {
  jvmToolchain(21)
}

application {
  mainClass.set("shelfie.MainKt")
}

// Generates Kotlin models for every .graphql operation in src/main/graphql, checked against schema.graphqls.
apollo {
  service("shelfie") {
    packageName.set("shelfie.graphql")
  }
}
