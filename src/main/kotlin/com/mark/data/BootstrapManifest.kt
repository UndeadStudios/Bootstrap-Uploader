package com.mark.data

import com.mark.data.Artifacts

data class BootstrapManifest(
    val launcherArguments : Array<String> = emptyArray(),
    val launcherJvm11Arguments : Array<String> = emptyArray(),
    val launcherJvm11WindowsArguments : Array<String> = emptyArray(),
    val launcherJvm17Arguments : Array<String> = emptyArray(),
    val launcherJvm17MacArguments : Array<String> = emptyArray(),
    val launcherJvm17WindowsArguments : Array<String> = emptyArray(),
    val clientJvmArguments : Array<String> = emptyArray(),
    val clientJvm9Arguments : Array<String> = emptyArray(),
    val clientJvm17MacArguments : Array<String> = emptyArray(),
    val clientJvm17Arguments : Array<String> = emptyArray(),
    var artifacts : Array<Artifacts> = emptyArray(),
    var dependencyHashes : Map<String,String> = emptyMap<String, String>().toMutableMap()
)