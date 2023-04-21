plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
    kotlin("jvm") version "1.8.20"
    kotlin("plugin.serialization") version "1.8.20"
}

group = "dev.mr3n"
version = "1.0-SNAPSHOT"

repositories {
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    mavenCentral()
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.8-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("io.ktor:ktor-client-core:2.2.4")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.2.4")
    implementation("io.ktor:ktor-client-cio:2.2.4")
    implementation(project(":shared"))
}

tasks.named("build") {
    dependsOn("shadowJar")
}

kotlin {
    jvmToolchain(8)
}