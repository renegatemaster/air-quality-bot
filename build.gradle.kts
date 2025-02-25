plugins {
    kotlin("jvm") version "2.1.10"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.renegatemaster.airqualitybot"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.1.0")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.0")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("org.seleniumhq.selenium:selenium-java:4.17.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.renegatemaster.airqualitybot.MainKt")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveFileName.set("app.jar")
    mergeServiceFiles()
    manifest {
        attributes["Main-Class"] = "com.renegatemaster.airqualitybot.MainKt"
    }
}