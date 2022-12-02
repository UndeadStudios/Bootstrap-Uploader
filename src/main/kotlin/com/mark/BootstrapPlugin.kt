package com.mark

import com.mark.utils.Keys
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import java.io.File

class BootstrapPlugin : Plugin<Project> {
    override fun apply(project: Project) : Unit = with(project) {
        val extension = project.extensions.create<BootstrapPluginExtension>("releaseSettings")

        val bootstrapDependencies by configurations.creating {
            isCanBeConsumed = false
            isCanBeResolved = true
            isTransitive = false
        }

        project.task("releaseClient") {
            this.group = "client update"

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
                BootstrapTask(
                    extension,
                    project
                ).init()

            }


        }

        project.task("generateKeys") {
            this.group = "client update"
            doLast {
                val saveLocations = File("${System.getProperty("user.home")}/.gradle/releaseClient/${project.name}/")
                Keys.generateKeys(saveLocations)
            }
        }

    }
}