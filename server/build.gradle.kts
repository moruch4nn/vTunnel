plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
    kotlin("jvm") version "1.8.20"
    kotlin("plugin.serialization") version "1.8.20"
}

group = "dev.mr3n"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.2.4")
    implementation("io.ktor:ktor-server-core:2.2.4")
    implementation("io.ktor:ktor-server-websockets:2.2.4")
    implementation("io.ktor:ktor-server-netty:2.2.4")
    implementation("com.velocitypowered:velocity-api:3.1.1")
    implementation("com.auth0:java-jwt:4.4.0")
    implementation(project(":shared"))
}

tasks.named("build") {
    dependsOn("shadowJar")
}

kotlin {
    jvmToolchain(8)
}