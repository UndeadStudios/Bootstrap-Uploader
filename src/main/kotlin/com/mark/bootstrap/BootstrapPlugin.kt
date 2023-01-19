package com.mark.bootstrap

import com.mark.bootstrap.utils.Keys
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.*
import java.io.File

class BootstrapPlugin : Plugin<Project> {

    @Input @Optional
    val key : String = ""
    @Input @Optional
    val uploadInfo : String = ""

    override fun apply(project: Project) : Unit = with(project) {

        this.group = "client update"

        val extension = project.extensions.create<BootstrapPluginExtension>("releaseSettings")

        val bootstrapDependencies by configurations.creating {
            isCanBeConsumed = false
            isCanBeResolved = true
            isTransitive = false
        }

        project.task("releaseClient") {
            dependsOn(bootstrapDependencies)
            dependsOn("jar")

            doLast {
                copy {
                    from(bootstrapDependencies)
                    into("${buildDir}/bootstrap/${extension.releaseType.get()}/")
                }
                copy {
                    from("${buildDir}/repo/.", "${buildDir}/libs/",)
                    into("${buildDir}/bootstrap/${extension.releaseType.get()}/repo/")
                }
                BootstrapTask(extension, project,uploadInfo,key).init()
            }

        }

        project.task("generateBootstrap") {
            dependsOn(bootstrapDependencies)
            dependsOn("jar")

            doLast {
                copy {
                    from(bootstrapDependencies)
                    into("${buildDir}/bootstrap/${extension.releaseType.get()}/")
                }
                copy {
                    from("${buildDir}/repo/.", "${buildDir}/libs/",)
                    into("${buildDir}/bootstrap/${extension.releaseType.get()}/repo/")
                }
                BootstrapTask(extension, project,uploadInfo,key).makeBootstrap()
            }
        }

        project.task("generateKeys") {
            doLast {
                val loadingFromFile = uploadInfo.isEmpty()
                val saveLocations = File("${System.getProperty("user.home")}/.gradle/releaseClient/${project.name}/")
                Keys.generateKeys(saveLocations,loadingFromFile)
            }
        }

    }
}