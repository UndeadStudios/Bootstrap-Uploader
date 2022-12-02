package com.mark.upload.impl

import com.mark.upload.Uploader
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.File
import java.util.*

class FtpUpload(
    val props : File,
    val type : String
) : Uploader() {

    init {
        if (!props.exists()) {
            error("Props: ${props.path} Is Missing")
        }
    }

    val client = FTPClient()

    override var connected: Boolean = false

    override fun baseDirectory(): String {
        return client.printWorkingDirectory()
    }

    override fun upload(file: File) {
        client.storeFile("/client/${type}/repo/${file.name}", file.inputStream())
    }

    override fun connect() {
        val properties = Properties()
        properties.load(props.inputStream())
        client.connect(properties.getProperty("host"))
        client.setFileType(FTP.BINARY_FILE_TYPE)
        val login = client.login(properties.getProperty("username"), properties.getProperty("password"))
        if (login) {
            println("FTP Logged in")
            connected = true
            client.makeDirectory("/client")
            client.makeDirectory("/client/${type}")
            client.makeDirectory("/client/${type}/repo/")
        } else {
            error("Error Logging into FTP")
        }
    }

    override fun connected() = client.isConnected

}