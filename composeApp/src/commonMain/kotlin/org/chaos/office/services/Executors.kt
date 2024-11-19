package org.chaos.office.services

import java.io.File
import java.nio.file.Files
import java.util.*
import java.util.concurrent.TimeUnit

object JqExecutor {
    private val jqExecutable: File by lazy {
        val osName = System.getProperty("os.name").lowercase(Locale.getDefault())
        val jqResourcePath = when {
            osName.contains("win") -> "/bin/jq-win/jq.exe"
            osName.contains("mac") -> "/bin/jq-mac/jq"
            else -> "/bin/jq-linux/jq"
        }

        val tempDir = Files.createTempDirectory("jq-temp").toFile()
        tempDir.deleteOnExit()

        val jqFile = File(tempDir, "jq")
        jqFile.outputStream().use { output ->
            JqExecutor::class.java.getResourceAsStream(jqResourcePath)?.use { input ->
                input.copyTo(output)
            } ?: throw RuntimeException("Failed to find jq executable")
        }
        jqFile.setExecutable(true)
        jqFile
    }

    fun execute(input: String, query: String = "."): String {
        val tempFile = File.createTempFile("jq_input", ".json")
        tempFile.writeText(input)

        val process = ProcessBuilder(jqExecutable.absolutePath, query, tempFile.absolutePath)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        process.waitFor(5, TimeUnit.SECONDS)

        val output = process.inputStream.bufferedReader().readText()
        val error = process.errorStream.bufferedReader().readText()

        tempFile.delete()

        return error.ifEmpty { output }
    }
}