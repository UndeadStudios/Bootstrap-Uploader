plugins {
    kotlin("jvm") version "1.7.20"
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
}

group = "com.mark.bootstrap"
version = "1.1"

repositories {
    mavenCentral()
}

gradlePlugin {

    plugins {
        create("ReleaseClientBootstrap") {
            id = "com.mark.bootstrap.Bootstrap-Uploader"
            implementationClass = "com.mark.BootstrapPlugin"
        }
    }

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
