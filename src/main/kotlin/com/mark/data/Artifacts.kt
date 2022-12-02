package com.mark.data

data class Artifacts(
    val hash : String = "",
    val name : String = "",
    val path : String = "",
    val size : Long = -1,
    val platform : List<Platform>? = null
)
