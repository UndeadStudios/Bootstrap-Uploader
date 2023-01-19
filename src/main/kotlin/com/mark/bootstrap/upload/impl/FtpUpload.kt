package com.mark.bootstrap.upload.impl

import com.mark.bootstrap.upload.Uploader
import me.tongfei.progressbar.ProgressBar
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.File
import java.io.StringReader
import java.util.*

class FtpUpload(
    val props : String,
    val type : String,
    val passiveMode : Boolean
) : Uploader() {

    val client = FTPClient()

    override var connected: Boolean = false

    override fun baseDirectory(): String {
        return client.printWorkingDirectory()
    }

    override fun upload(file: File, bar : ProgressBar) {

        val fileType = when(file.name.endsWith(".jar")) {
            true -> "Jar"
            false -> "Json"
        }

        bar.extraMessage = "Uploading: ${file.name}"
        println("Uploading: ${file.name} (${type}) / (${"/client/${type}/repo/${file.name}"})")

        try {
            if(passiveMode) {
                client.enterLocalPassiveMode()
            }
            if (fileType == "Jar") {
                client.storeFile("/client/${type}/repo/${file.name}", file.inputStream())
            } else {
                client.storeFile("/client/${type}/${file.name}", file.inputStream())
            }
        }catch (ex: Exception) {
            ex.printStackTrace();
        }

    }

    override fun connect() {
        val properties = Properties()
        properties.load(StringReader(props))
        client.connect(properties.getProperty("host"))
        client.setFileType(FTP.BINARY_FILE_TYPE)
        val login = client.login(properties.getProperty("username"), properties.getProperty("password"))
        if (login) {
            println("FTP Logged in")
            client.makeDirectory("/client")
            client.makeDirectory("/client/${type}")
            client.makeDirectory("/client/${type}/repo/")
            connected = true
        } else {
            error("Error Logging into FTP")
        }
    }

    override fun connected() = client.isConnected

}