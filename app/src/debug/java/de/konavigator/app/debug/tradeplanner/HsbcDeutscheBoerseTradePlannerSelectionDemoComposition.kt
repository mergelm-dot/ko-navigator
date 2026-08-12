package de.konavigator.app.debug.tradeplanner

import de.konavigator.app.application.repository.FxRateProvider
import de.konavigator.app.application.repository.KnockoutProductBrokerAvailabilityRepository
import de.konavigator.app.composition.TradePlannerSelectionComposition
import de.konavigator.app.composition.TradePlannerSelectionCompositionDependencies
import de.konavigator.app.data.remote.DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult
import de.konavigator.app.data.remote.DeutscheBoerseCompressedFileMarketDataRepositoryLoader
import de.konavigator.app.data.remote.RemoteKnockoutProductSpecificationCatalogRepository
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseSnapshotProviderCreationError
import de.konavigator.app.data.remote.provider.hsbc.HsbcKnockoutProductSpecificationResearchJsonFileLoader
import de.konavigator.app.data.remote.provider.hsbc.HsbcKnockoutProductSpecificationResearchJsonFileLoadingError
import de.konavigator.app.data.remote.provider.hsbc.HsbcKnockoutProductSpecificationResearchJsonFileLoadingResult
import de.konavigator.app.data.remote.provider.hsbc.HsbcResearchKnockoutProductSpecificationCatalogProviderCreationError
import de.konavigator.app.data.remote.provider.hsbc.HsbcResearchKnockoutProductSpecificationCatalogProviderCreationResult
import de.konavigator.app.data.remote.provider.hsbc.HsbcResearchKnockoutProductSpecificationCatalogProviderFactory
import de.konavigator.app.domain.freshness.MarketDataFreshnessPolicy
import de.konavigator.app.domain.source.MarketDataSourcePolicy
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionEvaluationTimeProvider
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionExecutionSettings
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionViewModelFactory
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

sealed interface HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionError {

    data class HsbcFileLoading(
        val errors: List<HsbcKnockoutProductSpecificationResearchJsonFileLoadingError>
    ) : HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionError

    data class HsbcCatalogProviderCreation(
        val errors: List<HsbcResearchKnockoutProductSpecificationCatalogProviderCreationError>
    ) : HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionError

    data class MarketDataLoading(
        val error: DeutscheBoerseSnapshotProviderCreationError
    ) : HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionError
}

sealed interface HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionResult {

    data class Success(
        val viewModelFactory: TradePlannerSelectionViewModelFactory
    ) : HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionResult

    data class Failure(
        val error: HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionError
    ) : HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionResult
}

/**
 * Lokale Debug-Composition fuer kontrollierte HSBC-Research-Dateien und lokale
 * Deutsche-Boerse-DXSC-/XFRA-Dateien. Sie ist keine Release-Komponente und verwendet weder
 * Netzwerk noch Systemzeit noch Android-Abhaengigkeiten.
 *
 * Der Aufbau ist absichtlich fail-fast: Zuerst werden die HSBC-Dateien geladen, anschliessend
 * der vorhandene HSBC-Katalogprovider erstellt und erst danach die Deutsche-Boerse-Marktdaten
 * fuer die kontrollierten HSBC-Produkt-ISINs geladen. Bei einem Fehler entsteht keine
 * ViewModel-Factory. Die eigentliche Selection-Pipeline wird ausschliesslich durch die
 * bestehende [TradePlannerSelectionComposition] verdrahtet.
 */
object HsbcDeutscheBoerseTradePlannerSelectionDemoComposition {

    suspend fun createFactory(
        dxscGzipFile: File,
        xfraZipFile: File,
        hsbcResearchJsonFilesByProductIsin: Map<String, File>,
        specificationRetrievedAtEpochMillis: Long,
        brokerAvailabilityRepository: KnockoutProductBrokerAvailabilityRepository,
        fxRateProvider: FxRateProvider,
        freshnessPolicy: MarketDataFreshnessPolicy,
        sourcePolicy: MarketDataSourcePolicy,
        executionSettings: TradePlannerSelectionExecutionSettings,
        evaluationTimeProvider: TradePlannerSelectionEvaluationTimeProvider,
        specificationFileDispatcher: CoroutineDispatcher = Dispatchers.IO,
        marketDataRepositoryLoader: DeutscheBoerseCompressedFileMarketDataRepositoryLoader =
            DeutscheBoerseCompressedFileMarketDataRepositoryLoader()
    ): HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionResult {
        val hsbcResearchJsonFilesByProductIsinSnapshot =
            hsbcResearchJsonFilesByProductIsin.toMap()

        return when (
            val fileLoadingResult = HsbcKnockoutProductSpecificationResearchJsonFileLoader.load(
                filesByProductIsin = hsbcResearchJsonFilesByProductIsinSnapshot,
                dispatcher = specificationFileDispatcher
            )
        ) {
            is HsbcKnockoutProductSpecificationResearchJsonFileLoadingResult.Failure ->
                HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionResult.Failure(
                    HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionError.HsbcFileLoading(
                        errors = fileLoadingResult.errors
                    )
                )

            is HsbcKnockoutProductSpecificationResearchJsonFileLoadingResult.Success ->
                when (
                    val catalogProviderCreationResult =
                        HsbcResearchKnockoutProductSpecificationCatalogProviderFactory.create(
                            researchJsonByProductIsin =
                                fileLoadingResult.researchJsonByProductIsin,
                            retrievedAtEpochMillis = specificationRetrievedAtEpochMillis
                        )
                ) {
                    is HsbcResearchKnockoutProductSpecificationCatalogProviderCreationResult.Failure ->
                        HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionResult.Failure(
                            HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionError
                                .HsbcCatalogProviderCreation(
                                    errors = catalogProviderCreationResult.errors
                                )
                        )

                    is HsbcResearchKnockoutProductSpecificationCatalogProviderCreationResult.Success ->
                        when (
                            val marketDataLoadingResult = marketDataRepositoryLoader.load(
                                dxscGzipFile = dxscGzipFile,
                                xfraZipFile = xfraZipFile,
                                requestedProductIsins =
                                    hsbcResearchJsonFilesByProductIsinSnapshot.keys.toSet()
                            )
                        ) {
                            is DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult.Failure ->
                                HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionResult
                                    .Failure(
                                        HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionError
                                            .MarketDataLoading(
                                                error = marketDataLoadingResult.error
                                            )
                                    )

                            is DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult.Success ->
                                HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionResult
                                    .Success(
                                        viewModelFactory =
                                            TradePlannerSelectionComposition
                                                .createViewModelFactory(
                                                    TradePlannerSelectionCompositionDependencies(
                                                        specificationCatalogRepository =
                                                            RemoteKnockoutProductSpecificationCatalogRepository(
                                                                provider =
                                                                    catalogProviderCreationResult
                                                                        .provider
                                                            ),
                                                        brokerAvailabilityRepository =
                                                            brokerAvailabilityRepository,
                                                        marketDataRepository =
                                                            marketDataLoadingResult.repository,
                                                        fxRateProvider = fxRateProvider,
                                                        freshnessPolicy = freshnessPolicy,
                                                        sourcePolicy = sourcePolicy,
                                                        executionSettings = executionSettings,
                                                        evaluationTimeProvider = evaluationTimeProvider
                                                    )
                                                )
                                    )
                        }
                }
        }
    }
}
