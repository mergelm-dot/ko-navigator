package de.konavigator.app.data.remote

import de.konavigator.app.application.repository.KnockoutProductMarketDataRepository
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseCompressedFileSnapshotProviderFactory
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseSnapshotProviderCreationError
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseSnapshotProviderCreationResult
import java.io.File

sealed interface DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult {

    data class Success(
        val repository: KnockoutProductMarketDataRepository
    ) : DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult

    data class Failure(
        val error: DeutscheBoerseSnapshotProviderCreationError
    ) : DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult
}

object DeutscheBoerseCompressedFileMarketDataRepositoryFactory {

    fun create(
        dxscGzipFile: File,
        xfraZipFile: File,
        requestedProductIsins: Set<String>
    ): DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult {
        val requestedProductIsinsSnapshot = requestedProductIsins.toSet()
        return when (
            val creationResult =
                DeutscheBoerseCompressedFileSnapshotProviderFactory.create(
                    dxscGzipFile = dxscGzipFile,
                    xfraZipFile = xfraZipFile,
                    requestedProductIsins = requestedProductIsinsSnapshot
                )
        ) {
            is DeutscheBoerseSnapshotProviderCreationResult.Success ->
                DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult.Success(
                    repository = RemoteKnockoutProductMarketDataRepository(
                        provider = creationResult.provider
                    )
                )

            is DeutscheBoerseSnapshotProviderCreationResult.Failure ->
                DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult.Failure(
                    error = creationResult.error
                )
        }
    }
}
