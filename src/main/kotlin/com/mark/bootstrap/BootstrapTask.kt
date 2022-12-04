package com.mark.bootstrap

import com.beust.klaxon.Klaxon
import com.google.gson.GsonBuilder
import com.mark.bootstrap.data.Artifacts
import com.mark.bootstrap.data.BootstrapManifest
import com.mark.bootstrap.data.Platform
import com.mark.bootstrap.upload.impl.FtpUpload
import com.mark.bootstrap.upload.impl.AwsUpload
import com.mark.bootstrap.utils.Keys
import me.tongfei.progressbar.ProgressBar
import me.tongfei.progressbar.ProgressBarStyle
import mu.KotlinLogging
import java.io.File
import java.security.MessageDigest
import kotlin.system.exitProcess
import org.gradle.api.Project
import java.time.Duration
import java.time.temporal.ChronoUnit

private val logger = KotlinLogging.logger {}

class BootstrapTask(
    val extension: BootstrapPluginExtension,
    val project : Project
) {

    fun init() {
        val saveLocations = File("${System.getProperty("user.home")}/.gradle/releaseClient/${project.name}/")
        val bootstrapLocation = File("${project.buildDir}/bootstrap/${extension.releaseType.get()}/bootstrap.json")

        if(!File(saveLocations,"key-private.pem").exists()) {
            logger.error { "Keys not found Generating new keys at: $saveLocations" }
            Keys.generateKeys(saveLocations)
        }

        val defaultBootstrap = getDefaultBootstrap()

        val upload = when(extension.uploadType.get()) {
            UploadType.FTP -> FtpUpload(File(saveLocations,"ftp.properties"),extension.releaseType.get())
            UploadType.AWS -> AwsUpload(File(saveLocations,"aws.properties"))
            else -> null
        } ?: error("Upload Type Null")

        upload.connect()

        val artifacts = getArtifacts().toMutableList()

        val externalLibs =  File("${project.buildDir}/bootstrap/${extension.releaseType.get()}/repo/").listFiles()

        val progress = progress("Uploading", externalLibs.size + 2)

        externalLibs.forEach {
            artifacts.add(
                Artifacts(
                hash(it.readBytes()),
                it.name,
                "${extension.baseLink.get()}/client/${extension.releaseType.get()}/repo/${it.name}",
                it.length()
            )
            )

            upload.upload(it)
            progress.extraMessage = it.name
            progress.step()
        }

        defaultBootstrap.artifacts = artifacts.toTypedArray()
        defaultBootstrap.dependencyHashes = artifacts.associate { it.name to it.hash }

        bootstrapLocation.writeText(GsonBuilder().setPrettyPrinting().create().toJson(defaultBootstrap))

        val bootstrapFiles = listOf(
            bootstrapLocation,
            File("${project.buildDir}/bootstrap/${extension.releaseType.get()}/bootstrap.json.sha256")
        )

        Keys.sha256(File(saveLocations,"key-private.pem"), bootstrapFiles[0], bootstrapFiles[1])

        bootstrapFiles.forEach {
            upload.upload(it)
            progress.extraMessage = it.name
            progress.step()
        }

        progress.close()


    }

    private fun getDefaultBootstrap() : BootstrapManifest {
        val TEMPLATE = File("${project.projectDir.path}/bootstrap.template")
        if(!TEMPLATE.exists()) {
            error(
                "bootstrap.template does not exist at {${TEMPLATE}}," +
                "Please add the file ${System.lineSeparator()}For more Info please look at the guide"
            )
        }
        return Klaxon().parse<BootstrapManifest>(TEMPLATE.readText())!!
    }

    fun getArtifacts(): List<Artifacts> {
        val artifacts = emptyList<Artifacts>().toMutableList()

        project.configurations.getAt("runtimeClasspath").resolvedConfiguration.resolvedArtifacts.forEach {

            val platform = emptyList<Platform>().toMutableList()

            if (it.file.name.contains("injection-annotations")) {
                return@forEach
            }


            val module = it.moduleVersion.id.toString()
            val splat = module.split(":")
            val name = splat[1]
            val group = splat[0]
            val version = splat[2]
            var path = ""

            if (it.file.name.contains("runelite-client") ||
                it.file.name.contains("http-api") ||
                it.file.name.contains("runescape-api") ||
                it.file.name.contains("runelite-api") ||
                it.file.name.contains("runelite-jshell")) {
                path = "https://github.com/open-osrs/hosting/raw/master/${extension.releaseType.get()}/${it.file.name}"
            } else if (it.file.name.contains("injection-annotations")) {
                path = "https://github.com/open-osrs/hosting/raw/master/" + group.replace(".", "/") + "/${name}/$version/${it.file.name}"
            } else if (!group.contains("runelite")) {
                path = "https://repo.maven.apache.org/maven2/" + group.replace(".", "/") + "/${name}/$version/${name}-$version"
                if (it.classifier != null && it.classifier != "no_aop") {
                    path += "-${it.classifier}"
                }
                path += ".jar"
            } else if (
                it.file.name.contains("trident") ||
                it.file.name.contains("discord") ||
                it.file.name.contains("substance") ||
                it.file.name.contains("gluegen") ||
                it.file.name.contains("jogl") ||
                it.file.name.contains("rlawt") ||
                it.file.name.contains("jocl")
            ) {
                path = "https://repo.runelite.net/"
                path += "${group.replace(".", "/")}/${name}/$version/${name}-$version"

                if (it.classifier != null) {
                    path += "-${it.classifier}"

                    if (it.classifier!!.contains("linux")) {
                        platform.add(Platform(null,"linux"))
                    } else if (it.classifier!!.contains("windows")) {

                        val arch = if (it.classifier!!.contains("amd64")) {
                            "amd64"
                        } else {
                            "x86"
                        }
                        platform.add(Platform(arch,"windows"))
                    } else if (it.classifier!!.contains("macos")) {

                        val arch = if (it.classifier!!.contains("x64")) {
                            "x86_64"
                        } else if (it.classifier!!.contains("arm64")) {
                            "aarch64"
                        } else {
                            null
                        }
                        platform.add(Platform(arch,"macos"))
                    }
                }
                path += ".jar"
            } else {
                println("ERROR: " + it.file.name + " has no download path!")
                exitProcess(-1)
            }

            val filePath = it.file.absolutePath
            val artifactFile = File(filePath)

            artifacts.add(
                Artifacts(
                hash(artifactFile.readBytes()),
                it.file.name,
                path,
                artifactFile.length(),
                platform
            )
            )

        }

        return artifacts
    }

    private fun hash(file: ByteArray): String {
        return MessageDigest.getInstance("SHA-256").digest(file).fold("") { str, it -> str + "%02x".format(it) }
    }

    private fun progress(task : String, amt : Int) : ProgressBar {
        return ProgressBar(
            task,
            amt.toLong(),
            1,
            System.err,
            ProgressBarStyle.ASCII,
            "",
            1,
            false,
            null,
            ChronoUnit.SECONDS,
            0L,
            Duration.ZERO
        )
    }

}