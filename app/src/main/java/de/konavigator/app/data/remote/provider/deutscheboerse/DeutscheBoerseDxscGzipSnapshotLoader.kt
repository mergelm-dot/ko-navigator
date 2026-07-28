package de.konavigator.app.data.remote.provider.deutscheboerse

import java.io.BufferedReader
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

object DeutscheBoerseDxscGzipSnapshotLoader {

    fun load(
        openCompressedInput: () -> InputStream,
        requestedProductIsins: Set<String>
    ): DeutscheBoerseDxscNdjsonLoadingResult {
        val requestedProductIsinsSnapshot = requestedProductIsins.toSet()
        if (requestedProductIsinsSnapshot.isEmpty()) {
            return DeutscheBoerseDxscNdjsonLoadingResult.Success(emptyList())
        }

        val compressedInput = try {
            openCompressedInput()
        } catch (_: IOException) {
            return sourceReadingFailure(lineNumber = 1L)
        }

        var ownedResource: Closeable = compressedInput
        var loadingResult: DeutscheBoerseDxscNdjsonLoadingResult? = null
        var runtimeFailure: RuntimeException? = null

        try {
            val gzipInput = try {
                GZIPInputStream(compressedInput)
            } catch (_: IOException) {
                loadingResult = sourceReadingFailure(lineNumber = 1L)
                null
            }

            if (gzipInput != null) {
                ownedResource = gzipInput
                val reader = BufferedReader(
                    InputStreamReader(gzipInput, StandardCharsets.UTF_8)
                )
                ownedResource = reader
                loadingResult = DeutscheBoerseDxscNdjsonSnapshotLoader.load(
                    lines = reader.lineSequence(),
                    requestedProductIsins = requestedProductIsinsSnapshot
                )
            }
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
            completedResult is DeutscheBoerseDxscNdjsonLoadingResult.Success
        ) {
            sourceReadingFailure(lineNumber = null)
        } else {
            completedResult
        }
    }

    private fun sourceReadingFailure(
        lineNumber: Long?
    ) = DeutscheBoerseDxscNdjsonLoadingResult.Failure(
        DeutscheBoerseDxscNdjsonLoadingError(
            code = DeutscheBoerseDxscNdjsonLoadingErrorCode.SOURCE_READING_FAILED,
            lineNumber = lineNumber
        )
    )
}
