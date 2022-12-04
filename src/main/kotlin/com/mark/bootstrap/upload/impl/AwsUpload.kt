package com.mark.bootstrap.upload.impl

import com.mark.bootstrap.upload.Uploader
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.File
import java.util.*

class AwsUpload(
    val props : File
) : Uploader() {

    init {
        if (!props.exists()) {
            error("Props: ${props.path} Is Missing")
        }
    }

    private val client = FTPClient()

    override var connected: Boolean = false

    override fun baseDirectory(): String {
        return client.printWorkingDirectory()
    }

    override fun upload(file: File) {
        TODO("Not yet implemented")
    }

    override fun connect() {
        val properties = Properties()
        properties.load(props.inputStream())
        client.connect(properties.getProperty("host"))
        val login = client.login(properties.getProperty("username"), properties.getProperty("password"))
        if (login) {
            println("FTP Logged in")
            connected = true
        } else {
            error("Error Logging into FTP")
        }
        client.setFileType(FTP.BINARY_FILE_TYPE)
    }

    override fun connected() = client.isConnected

}