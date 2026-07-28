package de.konavigator.app.data.remote.provider.deutscheboerse

import de.konavigator.app.data.remote.dto.KnockoutProductMarketDataDto
import de.konavigator.app.data.remote.provider.KnockoutProductMarketDataProvider
import de.konavigator.app.data.remote.provider.ProviderResult

/** Provides mapped market data from immutable snapshots of parsed Deutsche Boerse records. */
class DeutscheBoerseSnapshotKnockoutProductMarketDataProvider(
    dxscRecords: Iterable<DeutscheBoerseDxscPretradeRecord>,
    xfraRecords: Iterable<DeutscheBoerseXfraTradableInstrumentRecord>
) : KnockoutProductMarketDataProvider {

    private val dxscRecordsSnapshot = dxscRecords.toList()
    private val xfraRecordsSnapshot = xfraRecords.toList()

    override suspend fun findByProductIsin(
        productIsin: String
    ): ProviderResult<KnockoutProductMarketDataDto> {
        val matchingXfraRecords = xfraRecordsSnapshot.filter { record ->
            record.isin == productIsin
        }
        val xfraRecord = when (matchingXfraRecords.size) {
            0 -> return ProviderResult.NotFound
            1 -> matchingXfraRecords.single()
            else -> return ProviderResult.DataAccessFailure
        }

        val dxscRecord = when (
            val selection = DeutscheBoerseDxscLatestRecordSelector.select(
                records = dxscRecordsSnapshot,
                productIsin = productIsin
            )
        ) {
            DeutscheBoerseDxscLatestRecordSelectionResult.NotFound ->
                return ProviderResult.NotFound

            is DeutscheBoerseDxscLatestRecordSelectionResult.Failure ->
                return ProviderResult.DataAccessFailure

            is DeutscheBoerseDxscLatestRecordSelectionResult.Success ->
                selection.record
        }

        return when (
            val mapping = DeutscheBoerseKnockoutProductMarketDataMapper.map(
                dxscRecord = dxscRecord,
                xfraRecord = xfraRecord
            )
        ) {
            is DeutscheBoerseMarketDataMappingResult.Success ->
                ProviderResult.Success(mapping.dto)

            is DeutscheBoerseMarketDataMappingResult.Failure ->
                ProviderResult.DataAccessFailure
        }
    }
}
