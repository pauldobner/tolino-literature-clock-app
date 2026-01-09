package com.example.literatureclock

import java.io.DataOutputStream
import java.io.IOException

object ShellUtils {
    fun hasRootAccess(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output.contains("uid=0")
        } catch (e: Exception) {
            false
        }
    }

    // Returns the output or error message
    fun executeRootCommand(command: String): String {
        var os: DataOutputStream? = null
        try {
            val process = Runtime.getRuntime().exec("su")
            os = DataOutputStream(process.outputStream)
            
            // Redirect stderr to stdout to capture errors
            os.writeBytes("$command 2>&1\n")
            os.writeBytes("exit\n")
            os.flush()
            
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            return output
        } catch (e: Exception) {
            e.printStackTrace()
            return "Exception: ${e.message}"
        } finally {
            try {
                os?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
