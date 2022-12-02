package com.mark.upload

import java.io.File

abstract class Uploader {

    abstract var connected : Boolean

    abstract fun upload(file : File)

    abstract fun baseDirectory() : String

    abstract fun connect()

    abstract fun connected() : Boolean

}