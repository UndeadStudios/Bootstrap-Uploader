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
    private val extension: BootstrapPluginExtension,
    private val project : Project,
    private val uploadInfo : String,
    private val keys : String
) {

    fun init() {
        val saveLocation = File("${System.getProperty("user.home")}/.gradle/releaseClient/${project.name}/")
        if(!saveLocation.exists()) {
            saveLocation.mkdirs()
        }

        val loadingFromFile = uploadInfo.isEmpty()

        val bootstrapLocation = File("${project.buildDir}/bootstrap/${extension.releaseType.get()}/bootstrap.json")

        if(keys.isEmpty() || !File(saveLocation,"key-private.pem").exists()) {
            val message = when(loadingFromFile) {
                true -> "Keys not found Generating new keys at: $saveLocation"
                false -> "Keys not found Generating new keys:"
            }

            logger.error { message }
            Keys.generateKeys(saveLocation,loadingFromFile)
        }

        if(uploadInfo.isEmpty() || !File(saveLocation,"ftp.properties").exists()) {
            logger.error { "Upload info not found looking in [${if(loadingFromFile) "ftp.properties" else "args" }]" }
            exitProcess(0)
        }

        val loginInfo = when(loadingFromFile) {
            true -> File(saveLocation,"ftp.properties").toString()
            false -> uploadInfo
        }

        val defaultBootstrap = getDefaultBootstrap()

        val uploadManager = when(extension.uploadType.get()) {
            UploadType.FTP -> FtpUpload(loginInfo,extension.releaseType.get(), extension.passiveMode.get())
            UploadType.AWS -> AwsUpload(File(saveLocation,"aws.properties"))
        }

        val artifacts = getArtifacts().toMutableList()

        val externalLibs =  File("${project.buildDir}/bootstrap/${extension.releaseType.get()}/repo/").listFiles()

        uploadManager.connect()


        val progress = progress("Uploading", externalLibs.size + 2)

        externalLibs?.forEach {
            artifacts.add(
                Artifacts(
                    hash(it.readBytes()),
                    it.name,
                    "${extension.baseLink.get()}/client/${extension.releaseType.get()}/repo/${it.name}",
                    it.length()
                )
            )

            uploadManager.upload(it,progress)
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

        Keys.sha256(File(saveLocation,"key-private.pem"), bootstrapFiles[0], bootstrapFiles[1])

        bootstrapFiles.forEach {
            uploadManager.upload(it,progress)
            progress.step()
        }

        progress.close()


    }

    fun makeBootstrap() {
        val saveLocation = File("${System.getProperty("user.home")}/.gradle/releaseClient/${project.name}/")
        if(!saveLocation.exists()) {
            saveLocation.mkdirs()
        }

        val loadingFromFile = uploadInfo.isEmpty()

        val bootstrapLocation = File("${project.buildDir}/bootstrap/${extension.releaseType.get()}/bootstrap.json")

        if(!File(saveLocation,"key-private.pem").exists()) {
            val message = when(loadingFromFile) {
                true -> "Keys not found Generating new keys at: $saveLocation"
                false -> "Keys not found Generating new keys:"
            }

            logger.error { message }
            Keys.generateKeys(saveLocation,loadingFromFile)
        }


        val defaultBootstrap = getDefaultBootstrap()
        val artifacts = getArtifacts().toMutableList()

        defaultBootstrap.artifacts = artifacts.toTypedArray()
        defaultBootstrap.dependencyHashes = artifacts.associate { it.name to it.hash }

        bootstrapLocation.writeText(GsonBuilder().setPrettyPrinting().create().toJson(defaultBootstrap))

        val bootstrapFiles = listOf(
            bootstrapLocation,
            File("${project.buildDir}/bootstrap/${extension.releaseType.get()}/bootstrap.json.sha256")
        )

        Keys.sha256(File(saveLocation,"key-private.pem"), bootstrapFiles[0], bootstrapFiles[1])


    }

    private fun getDefaultBootstrap() : BootstrapManifest {
        val TEMPLATE = File("${project.projectDir.path}/bootstrap.template")
        if(!TEMPLATE.exists()) {
            logger.error { "bootstrap.template does not exist at {${TEMPLATE}}, using default one" }
            return Klaxon().parse<BootstrapManifest>(File("./bootstrap.template"))!!
        }
        return Klaxon().parse<BootstrapManifest>(TEMPLATE.readText())!!
    }

    fun getArtifacts(): List<Artifacts> {
        val artifacts = emptyList<Artifacts>().toMutableList()

        project.configurations.getAt("runtimeClasspath").resolvedConfiguration.resolvedArtifacts.forEach {

            var platform : MutableList<Platform>? = null

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

                    if (platform == null) {
                        platform = emptyList<Platform>().toMutableList()
                    }

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