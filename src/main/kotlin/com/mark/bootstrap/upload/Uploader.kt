package com.mark.bootstrap.upload

import me.tongfei.progressbar.ProgressBar
import java.io.File

abstract class Uploader {

    abstract var connected : Boolean

    abstract fun upload(file : File, bar : ProgressBar)

    abstract fun baseDirectory() : String

    abstract fun connect()

    abstract fun connected() : Boolean

}