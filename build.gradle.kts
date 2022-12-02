import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
    id("com.gradle.plugin-publish") version "1.1.0"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}


gradlePlugin {
    website.set("https://github.com/johndoe/GradlePlugins")
    vcsUrl.set("https://github.com/johndoe/GradlePlugins")
    plugins {
        create("greetingsPlugin") {
            id = "io.github.johndoe.greeting"
            implementationClass = "io.github.johndoe.gradle.GreetingPlugin"
            displayName = "Gradle Greeting plugin"
            description = "Gradle plugin to say hello!"
            tags.set(listOf("search", "tags", "for", "your", "hello", "plugin"))
        }
        create("goodbyePlugin") {
            id = "io.github.johndoe.goodbye"
            implementationClass = "io.github.johndoe.gradle.GoodbyePlugin"
            displayName = "Gradle Goodbye plugin"
            description = "Gradle plugin to say goodbye!"
            tags.set(listOf("search", "tags", "for", "your", "goodbye", "plugin"))
        }
    }
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}