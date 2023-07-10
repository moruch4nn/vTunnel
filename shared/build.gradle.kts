plugins {
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "dev.mr3n"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("com.auth0:java-jwt:4.4.0")
}

tasks.named("build") {
    dependsOn("shadowJar")
}

kotlin {
    jvmToolchain(8)
    sourceSets.all {
        languageSettings {
            languageVersion = "2.0"
        }
    }
}