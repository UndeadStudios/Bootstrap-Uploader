plugins {
    kotlin("jvm") version "1.7.20"
    id("com.gradle.plugin-publish") version "1.1.0"
    `kotlin-dsl`
}

group = "com.mark"
version = "1.1"

repositories {
    mavenCentral()
}

gradlePlugin {

    plugins {
        create("ReleaseClientBootstrap") {
            id = "com.mark"
            implementationClass = "com.mark.BootstrapPlugin"
            displayName = "Bootstrap Updater"
            description = "Release your Applications using Signed Bootstraps"
            version = "1.1"
        }
    }
}

pluginBundle {
    website = "https://github.com/Mark7625/bootstrap-release"
    vcsUrl = "https://github.com/Mark7625/bootstrap-release"
    tags = listOf("application", "update", "bootstrap")
}

dependencies {

    implementation(gradleApi())
    implementation("software.amazon.awssdk:s3:2.17.207")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation("com.beust:klaxon:5.5")
    implementation("commons-net:commons-net:3.8.0")
    implementation("io.github.microutils:kotlin-logging:1.12.5")
    implementation("org.slf4j:slf4j-simple:1.7.29")
    implementation("me.tongfei:progressbar:0.9.2")
    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation("com.google.code.gson:gson:2.10")
    // https://mvnrepository.com/artifact/joda-time/joda-time
    implementation("joda-time:joda-time:2.12.2")


}
