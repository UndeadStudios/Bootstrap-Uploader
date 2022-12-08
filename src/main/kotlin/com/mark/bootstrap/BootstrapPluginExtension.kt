package com.mark.bootstrap

import org.gradle.api.provider.Property

interface BootstrapPluginExtension {
    val uploadType: Property<UploadType>
    val releaseType: Property<String>
    val baseLink: Property<String>
    val passiveMode: Property<Boolean>
}