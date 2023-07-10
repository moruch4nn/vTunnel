plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0"
}

group = "dev.mr3n"
version = "1.0-SNAPSHOT"

repositories {
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://jitpack.io")
    mavenCentral()
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.8-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("com.github.moruch4nn:PaperAllInOne:852cbd13c0")
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
    sourceSets.all {
        languageSettings {
            languageVersion = "2.0"
        }
    }
}