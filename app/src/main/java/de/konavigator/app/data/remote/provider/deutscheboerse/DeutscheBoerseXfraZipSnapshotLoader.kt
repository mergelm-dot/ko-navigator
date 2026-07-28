package de.konavigator.app.data.remote.provider.deutscheboerse

import java.io.BufferedReader
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream

object DeutscheBoerseXfraZipSnapshotLoader {

    fun load(
        openCompressedInput: () -> InputStream,
        requestedProductIsins: Set<String>
    ): DeutscheBoerseXfraCsvLoadingResult {
        val requestedProductIsinsSnapshot = requestedProductIsins.toSet()
        if (requestedProductIsinsSnapshot.isEmpty()) {
            return DeutscheBoerseXfraCsvLoadingResult.Success(emptyList())
        }

        val compressedInput = try {
            openCompressedInput()
        } catch (_: IOException) {
            return sourceReadingFailure(lineNumber = 1L)
        }

        var ownedResource: Closeable = compressedInput
        var loadingResult: DeutscheBoerseXfraCsvLoadingResult? = null
        var runtimeFailure: RuntimeException? = null

        try {
            val zipInput = ZipInputStream(compressedInput)
            ownedResource = zipInput
            val regularEntryFound = try {
                findFirstRegularEntry(zipInput)
            } catch (_: IOException) {
                loadingResult = sourceReadingFailure(lineNumber = 1L)
                false
            }

            if (regularEntryFound) {
                val reader = BufferedReader(
                    InputStreamReader(zipInput, StandardCharsets.UTF_8)
                )
                ownedResource = reader
                loadingResult = DeutscheBoerseXfraCsvSnapshotLoader.load(
                    lines = reader.lineSequence(),
                    requestedProductIsins = requestedProductIsinsSnapshot
                )
            } else if (loadingResult == null) {
                loadingResult = sourceReadingFailure(lineNumber = 1L)
            }
        } catch (failure: IOException) {
            loadingResult = sourceReadingFailure(lineNumber = 1L)
        } catch (failure: RuntimeException) {
            runtimeFailure = failure
        }

        val closingFailure = try {
            ownedResource.close()
            null
        } catch (failure: IOException) {
            failure
        }

        runtimeFailure?.let { throw it }

        val completedResult = checkNotNull(loadingResult)
        return if (
            closingFailure != null &&
            completedResult is DeutscheBoerseXfraCsvLoadingResult.Success
        ) {
            sourceReadingFailure(lineNumber = null)
        } else {
            completedResult
        }
    }

    @Throws(IOException::class)
    private fun findFirstRegularEntry(
        zipInput: ZipInputStream
    ): Boolean {
        while (true) {
            val entry = zipInput.nextEntry ?: return false
            if (!entry.isDirectory) {
                return true
            }
            zipInput.closeEntry()
        }
    }

    private fun sourceReadingFailure(
        lineNumber: Long?
    ) = DeutscheBoerseXfraCsvLoadingResult.Failure(
        DeutscheBoerseXfraCsvLoadingError(
            code = DeutscheBoerseXfraCsvLoadingErrorCode.SOURCE_READING_FAILED,
            lineNumber = lineNumber
        )
    )
}
