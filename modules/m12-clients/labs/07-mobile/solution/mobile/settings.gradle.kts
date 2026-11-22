// Lab 12.7. Gradle, not Maven: Apollo Kotlin's code generator ships only as a Gradle plugin (the one Maven
// plugin, com.github.aoudiamoncef:apollo-client-maven-plugin 7.1.0, targets Apollo 3.8.2 from 2023).
pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
}
dependencyResolutionManagement {
  repositories {
    mavenCentral()
  }
}
rootProject.name = "shelfie-mobile"
