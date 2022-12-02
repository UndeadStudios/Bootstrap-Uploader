package com.mark

import org.gradle.api.provider.Property

interface BootstrapPluginExtension {
    val uploadType: Property<UploadType>
    val releaseType: Property<String>
    val baseLink: Property<String>
}