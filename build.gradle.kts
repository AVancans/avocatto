val kotlin_version: String by project
val logback_version: String by project
val ktor_version: String by project

plugins {
    kotlin("jvm") version "2.1.10"
    id("io.ktor.plugin") version "3.1.0"
}

group = "com.avocatto"
version = "1.0-SNAPSHOT"

application {
    mainClass = "com.avocatto.Main"

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-websockets")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-gson")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-cio")

    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("ch.qos.logback:logback-classic:1.2.11")

    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-websockets:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")

    implementation("com.google.firebase:firebase-admin:9.4.3")
    implementation("io.reactivex.rxjava3:rxjava:3.1.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-rx3:1.10.1")

    testImplementation(kotlin("test"))
}

configurations.all {
    resolutionStrategy {
        force("org.slf4j:slf4j-api:1.7.36")
    }
}

tasks.test {
    useJUnitPlatform()
}