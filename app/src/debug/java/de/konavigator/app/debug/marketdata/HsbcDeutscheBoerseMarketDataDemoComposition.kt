package de.konavigator.app.debug.marketdata

import de.konavigator.app.data.remote.DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult
import de.konavigator.app.data.remote.DeutscheBoerseCompressedFileMarketDataRepositoryLoader
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseKnockoutProductMarketDataMapper
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseSnapshotProviderCreationError
import de.konavigator.app.data.remote.provider.hsbc.HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingError
import de.konavigator.app.data.remote.provider.hsbc.HsbcKnockoutProductSpecificationResearchJsonFileSpecificationRepositoryLoader
import de.konavigator.app.data.remote.provider.hsbc.HsbcKnockoutProductSpecificationResearchJsonFileSpecificationRepositoryLoadingResult
import de.konavigator.app.domain.freshness.MarketDataFreshnessThresholds
import de.konavigator.app.presentation.marketdata.MarketDataCalculationViewModelFactory
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

sealed interface HsbcDeutscheBoerseMarketDataDemoCompositionError {

    data class MarketDataLoading(
        val error: DeutscheBoerseSnapshotProviderCreationError
    ) : HsbcDeutscheBoerseMarketDataDemoCompositionError

    data class SpecificationLoading(
        val error: HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingError
    ) : HsbcDeutscheBoerseMarketDataDemoCompositionError
}

sealed interface HsbcDeutscheBoerseMarketDataDemoCompositionResult {

    data class Success(
        val viewModelFactory: MarketDataCalculationViewModelFactory
    ) : HsbcDeutscheBoerseMarketDataDemoCompositionResult

    data class Failure(
        val error: HsbcDeutscheBoerseMarketDataDemoCompositionError
    ) : HsbcDeutscheBoerseMarketDataDemoCompositionResult
}

/**
 * Lokale Debug- und Demo-Composition fuer kontrollierte HSBC-Spezifikationsdateien und lokale
 * komprimierte Deutsche-Boerse-Marktdaten. Sie ist keine Release- oder Produktionskomponente
 * und besitzt keine Netzwerk- oder Live-Datenquelle.
 *
 * Die Composition verwendet ausschliesslich den bestehenden HSBC-Spezifikations-Repository-
 * Loader, den bestehenden Deutsche-Boerse-Repository-Loader und den bestehenden
 * providerneutralen Debug-Composer. Eingabemengen werden vor dem ersten suspendierenden
 * Aufruf defensiv kopiert. Deutsche-Boerse-Marktdaten werden zuerst geladen; bei einem
 * Marktdatenfehler wird der HSBC-Pfad nicht gestartet. Bei Fehlern entsteht keine
 * ViewModel-Factory, und Fehler beider Pfade bleiben typisiert getrennt und unveraendert.
 *
 * Die Deutsche-Boerse-Source-ID wird unveraendert an den Composer uebergeben. Dadurch bleiben
 * `PURCHASE_PRICE`, `SALE_PRICE`, `SPREAD` und `MID` unterstuetzt. Abruf- und
 * Anbieterzeitpunkt bleiben getrennt. Es erfolgen keine Normalisierung, keine Defaults und
 * kein stiller Zeitersatz.
 *
 * Der Baustein enthaelt keine eigene Datei-, Parser-, Mapper-, Provider-, Repository- oder
 * Berechnungslogik und fuehrt keine Repository-Abfragen aus.
 */
object HsbcDeutscheBoerseMarketDataDemoComposition {

    suspend fun createFactory(
        dxscGzipFile: File,
        xfraZipFile: File,
        requestedProductIsins: Set<String>,
        hsbcResearchJsonFilesByProductIsin: Map<String, File>,
        specificationRetrievedAtEpochMillis: Long,
        freshnessThresholds: MarketDataFreshnessThresholds,
        specificationFileDispatcher: CoroutineDispatcher = Dispatchers.IO,
        marketDataRepositoryLoader: DeutscheBoerseCompressedFileMarketDataRepositoryLoader =
            DeutscheBoerseCompressedFileMarketDataRepositoryLoader()
    ): HsbcDeutscheBoerseMarketDataDemoCompositionResult {
        val requestedProductIsinsSnapshot = requestedProductIsins.toSet()
        val hsbcResearchJsonFilesByProductIsinSnapshot =
            hsbcResearchJsonFilesByProductIsin.toMap()

        return when (
            val marketDataResult = marketDataRepositoryLoader.load(
                dxscGzipFile = dxscGzipFile,
                xfraZipFile = xfraZipFile,
                requestedProductIsins = requestedProductIsinsSnapshot
            )
        ) {
            is DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult.Failure ->
                HsbcDeutscheBoerseMarketDataDemoCompositionResult.Failure(
                    error = HsbcDeutscheBoerseMarketDataDemoCompositionError.MarketDataLoading(
                        error = marketDataResult.error
                    )
                )

            is DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult.Success ->
                when (
                    val specificationResult =
                        HsbcKnockoutProductSpecificationResearchJsonFileSpecificationRepositoryLoader
                            .load(
                                filesByProductIsin =
                                    hsbcResearchJsonFilesByProductIsinSnapshot,
                                retrievedAtEpochMillis =
                                    specificationRetrievedAtEpochMillis,
                                dispatcher = specificationFileDispatcher
                            )
                ) {
                    is HsbcKnockoutProductSpecificationResearchJsonFileSpecificationRepositoryLoadingResult
                        .Failure ->
                        HsbcDeutscheBoerseMarketDataDemoCompositionResult.Failure(
                            error =
                                HsbcDeutscheBoerseMarketDataDemoCompositionError
                                    .SpecificationLoading(error = specificationResult.error)
                        )

                    is HsbcKnockoutProductSpecificationResearchJsonFileSpecificationRepositoryLoadingResult
                        .Success -> {
                        val viewModelFactory =
                            MarketDataCalculationDemoViewModelFactoryComposer.create(
                                specificationRepository = specificationResult.repository,
                                marketDataRepository = marketDataResult.repository,
                                freshnessThresholds = freshnessThresholds,
                                marketDataSourceId =
                                    DeutscheBoerseKnockoutProductMarketDataMapper.SOURCE_ID
                            )

                        HsbcDeutscheBoerseMarketDataDemoCompositionResult.Success(
                            viewModelFactory = viewModelFactory
                        )
                    }
                }
        }
    }
}
