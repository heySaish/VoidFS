package com.voidkernel.voidfs.data.shell

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

data class ShellResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
) {
    val isSuccess: Boolean get() = exitCode == 0
}

object RootShellExecutor {

    suspend fun runCommand(command: String): ShellResult = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder("su").start()
            val os = DataOutputStream(process.outputStream)
            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
            val stderrReader = BufferedReader(InputStreamReader(process.errorStream))

            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()

            val stdoutBuilder = StringBuilder()
            var line: String?
            while (stdoutReader.readLine().also { line = it } != null) {
                stdoutBuilder.append(line).append("\n")
            }

            val stderrBuilder = StringBuilder()
            while (stderrReader.readLine().also { line = it } != null) {
                stderrBuilder.append(line).append("\n")
            }

            val exitCode = process.waitFor()

            ShellResult(
                exitCode = exitCode,
                stdout = stdoutBuilder.toString().trim(),
                stderr = stderrBuilder.toString().trim()
            )
        } catch (e: Exception) {
            ShellResult(
                exitCode = -1,
                stdout = "",
                stderr = e.localizedMessage ?: "Unknown shell error"
            )
        }
    }
}
