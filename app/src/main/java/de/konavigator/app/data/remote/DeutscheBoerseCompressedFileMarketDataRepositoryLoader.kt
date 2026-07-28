package de.konavigator.app.data.remote

import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeutscheBoerseCompressedFileMarketDataRepositoryLoader(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun load(
        dxscGzipFile: File,
        xfraZipFile: File,
        requestedProductIsins: Set<String>
    ): DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult {
        val requestedProductIsinsSnapshot = requestedProductIsins.toSet()
        return withContext(ioDispatcher) {
            DeutscheBoerseCompressedFileMarketDataRepositoryFactory.create(
                dxscGzipFile = dxscGzipFile,
                xfraZipFile = xfraZipFile,
                requestedProductIsins = requestedProductIsinsSnapshot
            )
        }
    }
}
