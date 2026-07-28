package de.konavigator.app.data.remote.provider.deutscheboerse

import java.io.InputStream

enum class DeutscheBoerseSnapshotProviderCreationErrorCode {
    XFRA_LOADING_FAILED,
    DXSC_LOADING_FAILED
}

data class DeutscheBoerseSnapshotProviderCreationError(
    val code: DeutscheBoerseSnapshotProviderCreationErrorCode,
    val xfraLoadingError: DeutscheBoerseXfraCsvLoadingError? = null,
    val dxscLoadingError: DeutscheBoerseDxscNdjsonLoadingError? = null
)

sealed interface DeutscheBoerseSnapshotProviderCreationResult {

    data class Success(
        val provider: DeutscheBoerseSnapshotKnockoutProductMarketDataProvider
    ) : DeutscheBoerseSnapshotProviderCreationResult

    data class Failure(
        val error: DeutscheBoerseSnapshotProviderCreationError
    ) : DeutscheBoerseSnapshotProviderCreationResult
}

object DeutscheBoerseSnapshotProviderFactory {

    fun createFromCompressedSources(
        openDxscCompressedInput: () -> InputStream,
        openXfraCompressedInput: () -> InputStream,
        requestedProductIsins: Set<String>
    ): DeutscheBoerseSnapshotProviderCreationResult {
        val requestedProductIsinsSnapshot = requestedProductIsins.toSet()
        if (requestedProductIsinsSnapshot.isEmpty()) {
            return success(
                dxscRecords = emptyList(),
                xfraRecords = emptyList()
            )
        }

        val xfraRecords = when (
            val xfraResult = DeutscheBoerseXfraZipSnapshotLoader.load(
                openCompressedInput = openXfraCompressedInput,
                requestedProductIsins = requestedProductIsinsSnapshot
            )
        ) {
            is DeutscheBoerseXfraCsvLoadingResult.Failure ->
                return DeutscheBoerseSnapshotProviderCreationResult.Failure(
                    DeutscheBoerseSnapshotProviderCreationError(
                        code =
                            DeutscheBoerseSnapshotProviderCreationErrorCode
                                .XFRA_LOADING_FAILED,
                        xfraLoadingError = xfraResult.error
                    )
                )

            is DeutscheBoerseXfraCsvLoadingResult.Success -> xfraResult.records
        }

        val dxscRecords = when (
            val dxscResult = DeutscheBoerseDxscGzipSnapshotLoader.load(
                openCompressedInput = openDxscCompressedInput,
                requestedProductIsins = requestedProductIsinsSnapshot
            )
        ) {
            is DeutscheBoerseDxscNdjsonLoadingResult.Failure ->
                return DeutscheBoerseSnapshotProviderCreationResult.Failure(
                    DeutscheBoerseSnapshotProviderCreationError(
                        code =
                            DeutscheBoerseSnapshotProviderCreationErrorCode
                                .DXSC_LOADING_FAILED,
                        dxscLoadingError = dxscResult.error
                    )
                )

            is DeutscheBoerseDxscNdjsonLoadingResult.Success -> dxscResult.records
        }

        return success(
            dxscRecords = dxscRecords,
            xfraRecords = xfraRecords
        )
    }

    fun create(
        dxscLines: Sequence<String>,
        xfraLines: Sequence<String>,
        requestedProductIsins: Set<String>
    ): DeutscheBoerseSnapshotProviderCreationResult {
        val requestedProductIsinsSnapshot = requestedProductIsins.toSet()
        if (requestedProductIsinsSnapshot.isEmpty()) {
            return success(
                dxscRecords = emptyList(),
                xfraRecords = emptyList()
            )
        }

        val xfraRecords = when (
            val xfraResult = DeutscheBoerseXfraCsvSnapshotLoader.load(
                lines = xfraLines,
                requestedProductIsins = requestedProductIsinsSnapshot
            )
        ) {
            is DeutscheBoerseXfraCsvLoadingResult.Failure ->
                return DeutscheBoerseSnapshotProviderCreationResult.Failure(
                    DeutscheBoerseSnapshotProviderCreationError(
                        code =
                            DeutscheBoerseSnapshotProviderCreationErrorCode
                                .XFRA_LOADING_FAILED,
                        xfraLoadingError = xfraResult.error
                    )
                )

            is DeutscheBoerseXfraCsvLoadingResult.Success -> xfraResult.records
        }

        val dxscRecords = when (
            val dxscResult = DeutscheBoerseDxscNdjsonSnapshotLoader.load(
                lines = dxscLines,
                requestedProductIsins = requestedProductIsinsSnapshot
            )
        ) {
            is DeutscheBoerseDxscNdjsonLoadingResult.Failure ->
                return DeutscheBoerseSnapshotProviderCreationResult.Failure(
                    DeutscheBoerseSnapshotProviderCreationError(
                        code =
                            DeutscheBoerseSnapshotProviderCreationErrorCode
                                .DXSC_LOADING_FAILED,
                        dxscLoadingError = dxscResult.error
                    )
                )

            is DeutscheBoerseDxscNdjsonLoadingResult.Success -> dxscResult.records
        }

        return success(
            dxscRecords = dxscRecords,
            xfraRecords = xfraRecords
        )
    }

    private fun success(
        dxscRecords: List<DeutscheBoerseDxscPretradeRecord>,
        xfraRecords: List<DeutscheBoerseXfraTradableInstrumentRecord>
    ) = DeutscheBoerseSnapshotProviderCreationResult.Success(
        DeutscheBoerseSnapshotKnockoutProductMarketDataProvider(
            dxscRecords = dxscRecords,
            xfraRecords = xfraRecords
        )
    )
}
