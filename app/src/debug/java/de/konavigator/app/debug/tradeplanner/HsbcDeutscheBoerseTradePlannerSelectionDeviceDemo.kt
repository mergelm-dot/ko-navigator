package de.konavigator.app.debug.tradeplanner

import de.konavigator.app.application.repository.FxRateProvider
import de.konavigator.app.application.repository.FxRateProviderResult
import de.konavigator.app.data.remote.RemoteKnockoutProductBrokerAvailabilityRepository
import de.konavigator.app.data.remote.provider.KnockoutProductBrokerAvailabilityProvider
import de.konavigator.app.data.remote.provider.KnockoutProductBrokerAvailabilityProviderResult
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseKnockoutProductMarketDataMapper
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseXfraRequiredColumn
import de.konavigator.app.domain.availability.MarketDataCalculationType
import de.konavigator.app.domain.currency.CurrencyCode
import de.konavigator.app.domain.freshness.MarketDataFreshnessPolicy
import de.konavigator.app.domain.freshness.MarketDataFreshnessThresholds
import de.konavigator.app.domain.source.MarketDataSourcePolicy
import de.konavigator.app.domain.source.MarketDataSourcePolicyConfig
import de.konavigator.app.domain.source.MarketDataSourceRule
import de.konavigator.app.presentation.tradeplanner.TradePlannerBrokerUiOption
import de.konavigator.app.presentation.tradeplanner.TradePlannerIssuerUiOption
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionEvaluationTimeProvider
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionExecutionSettings
import de.konavigator.app.presentation.tradeplanner.TradePlannerSelectionViewModelFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

sealed interface HsbcDeutscheBoerseTradePlannerSelectionDeviceDemoError {

    data object FixtureDirectoryUnavailable :
        HsbcDeutscheBoerseTradePlannerSelectionDeviceDemoError

    data object FixtureWritingFailed : HsbcDeutscheBoerseTradePlannerSelectionDeviceDemoError

    data class CompositionFailure(
        val error: HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionError
    ) : HsbcDeutscheBoerseTradePlannerSelectionDeviceDemoError
}

sealed interface HsbcDeutscheBoerseTradePlannerSelectionDeviceDemoResult {

    data class Success(
        val viewModelFactory: TradePlannerSelectionViewModelFactory,
        val brokerOptions: List<TradePlannerBrokerUiOption>,
        val issuerOptions: List<TradePlannerIssuerUiOption>
    ) : HsbcDeutscheBoerseTradePlannerSelectionDeviceDemoResult

    data class Failure(
        val error: HsbcDeutscheBoerseTradePlannerSelectionDeviceDemoError
    ) : HsbcDeutscheBoerseTradePlannerSelectionDeviceDemoResult
}

/**
 * Debug-only fixture builder for the controlled synthetic NVIDIA selection demo.
 *
 * It writes only loader input files into the caller-provided directory, then delegates the
 * complete selection setup to [HsbcDeutscheBoerseTradePlannerSelectionDemoComposition]. No
 * network, system clock, product calculation, ranking, or manual product selection is present.
 */
object HsbcDeutscheBoerseTradePlannerSelectionDeviceDemo {

    suspend fun create(
        workingDirectory: File
    ): HsbcDeutscheBoerseTradePlannerSelectionDeviceDemoResult {
        val fixtures = when (val fixtureResult = createFixtures(workingDirectory)) {
            is FixtureCreationResult.Failure ->
                return HsbcDeutscheBoerseTradePlannerSelectionDeviceDemoResult.Failure(
                    error = fixtureResult.error
                )

            is FixtureCreationResult.Success -> fixtureResult.fixtures
        }

        return when (
            val compositionResult =
                HsbcDeutscheBoerseTradePlannerSelectionDemoComposition.createFactory(
                    dxscGzipFile = fixtures.dxscGzipFile,
                    xfraZipFile = fixtures.xfraZipFile,
                    hsbcResearchJsonFilesByProductIsin = fixtures.hsbcFilesByProductIsin,
                    specificationRetrievedAtEpochMillis = SPECIFICATION_RETRIEVED_AT_EPOCH_MILLIS,
                    brokerAvailabilityRepository =
                        RemoteKnockoutProductBrokerAvailabilityRepository(
                            provider = DebugBrokerAvailabilityProvider
                        ),
                    fxRateProvider = DebugFxRateProvider,
                    freshnessPolicy = debugFreshnessPolicy(),
                    sourcePolicy = debugSourcePolicy(),
                    executionSettings = debugExecutionSettings(),
                    evaluationTimeProvider = TradePlannerSelectionEvaluationTimeProvider {
                        MARKET_DATA_TIMESTAMP_EPOCH_MILLIS
                    }
                )
        ) {
            is HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionResult.Failure ->
                HsbcDeutscheBoerseTradePlannerSelectionDeviceDemoResult.Failure(
                    HsbcDeutscheBoerseTradePlannerSelectionDeviceDemoError.CompositionFailure(
                        error = compositionResult.error
                    )
                )

            is HsbcDeutscheBoerseTradePlannerSelectionDemoCompositionResult.Success ->
                HsbcDeutscheBoerseTradePlannerSelectionDeviceDemoResult.Success(
                    viewModelFactory = compositionResult.viewModelFactory,
                    brokerOptions = listOf(
                        TradePlannerBrokerUiOption(
                            id = DEBUG_BROKER_ID,
                            displayName = "Debug Broker"
                        )
                    ),
                    issuerOptions = listOf(
                        TradePlannerIssuerUiOption(
                            id = SYNTHETIC_ISSUER_ID,
                            displayName = "Synthetic Issuer"
                        )
                    )
                )
        }
    }

    private fun createFixtures(
        workingDirectory: File
    ): FixtureCreationResult {
        val fixtureDirectory = File(workingDirectory, FIXTURE_DIRECTORY_NAME)
        if (!fixtureDirectory.isDirectory && !fixtureDirectory.mkdirs()) {
            return FixtureCreationResult.Failure(
                HsbcDeutscheBoerseTradePlannerSelectionDeviceDemoError
                    .FixtureDirectoryUnavailable
            )
        }

        return try {
            val hsbcFilesByProductIsin = linkedMapOf(
                PRODUCT_A_ISIN to writeHsbcResearchJson(
                    directory = fixtureDirectory,
                    productIsin = PRODUCT_A_ISIN,
                    productWkn = PRODUCT_A_WKN
                ),
                PRODUCT_B_ISIN to writeHsbcResearchJson(
                    directory = fixtureDirectory,
                    productIsin = PRODUCT_B_ISIN,
                    productWkn = PRODUCT_B_WKN
                )
            )
            val dxscGzipFile = writeDxscGzip(
                directory = fixtureDirectory,
                productIsins = hsbcFilesByProductIsin.keys
            )
            val xfraZipFile = writeXfraZip(
                directory = fixtureDirectory,
                productIsins = hsbcFilesByProductIsin.keys
            )

            FixtureCreationResult.Success(
                DeviceDemoFixtures(
                    dxscGzipFile = dxscGzipFile,
                    xfraZipFile = xfraZipFile,
                    hsbcFilesByProductIsin = hsbcFilesByProductIsin
                )
            )
        } catch (_: IOException) {
            FixtureCreationResult.Failure(
                HsbcDeutscheBoerseTradePlannerSelectionDeviceDemoError.FixtureWritingFailed
            )
        } catch (_: SecurityException) {
            FixtureCreationResult.Failure(
                HsbcDeutscheBoerseTradePlannerSelectionDeviceDemoError.FixtureWritingFailed
            )
        }
    }

    private fun writeHsbcResearchJson(
        directory: File,
        productIsin: String,
        productWkn: String
    ): File = File(directory, "hsbc-$productIsin.json").also { file ->
        file.writeText(
            text = """{
                "productIsin":"$productIsin",
                "productWkn":"$productWkn",
                "issuerId":"$SYNTHETIC_ISSUER_ID",
                "underlyingId":"$NVIDIA_UNDERLYING_ID",
                "directionLabel":"Call",
                "basePrice":90.0,
                "knockoutBarrier":80.0,
                "ratio":0.1,
                "underlyingCurrency":"USD",
                "productCurrency":"USD",
                "sourceTimestampEpochMillis":$MARKET_DATA_TIMESTAMP_EPOCH_MILLIS
            }""".trimIndent(),
            charset = StandardCharsets.UTF_8
        )
    }

    private fun writeDxscGzip(
        directory: File,
        productIsins: Set<String>
    ): File = File(directory, "deutsche-boerse-dxsc.json.gz").also { file ->
        GZIPOutputStream(FileOutputStream(file))
            .bufferedWriter(StandardCharsets.UTF_8)
            .use { writer ->
                writer.write(
                    productIsins.joinToString(separator = "\n", postfix = "\n") { productIsin ->
                        """{"messageId":"pretrade","instrumentIdentificationCode":"$productIsin","bestBid":1.0,"bestAsk":1.1,"updateDateAndTime":"$MARKET_DATA_TIMESTAMP"}"""
                    }
                )
            }
    }

    private fun writeXfraZip(
        directory: File,
        productIsins: Set<String>
    ): File = File(directory, "deutsche-boerse-xfra.zip").also { file ->
        ZipOutputStream(FileOutputStream(file), StandardCharsets.UTF_8).use { zip ->
            zip.putNextEntry(ZipEntry("reference"))
            zip.write(
                xfraCsvContent(productIsins)
                    .toByteArray(StandardCharsets.UTF_8)
            )
            zip.closeEntry()
        }
    }

    private fun xfraCsvContent(productIsins: Set<String>) =
        listOf(
            "Market:;XFRA",
            "Date Last Update:;27.07.2026",
            DeutscheBoerseXfraRequiredColumn.entries.joinToString(";") { it.headerName }
        ).plus(
            productIsins.map { productIsin ->
                listOf(
                    "Active",
                    "Tradable",
                    "Synthetic Instrument",
                    productIsin,
                    "SYNTHETIC",
                    "XFRA",
                    "Warrant",
                    "USD",
                    "USD",
                    "Call",
                    "08:00",
                    "22:00"
                ).joinToString(";")
            }
        ).joinToString(separator = "\n", postfix = "\n")

    private fun debugFreshnessPolicy() = MarketDataFreshnessPolicy(
        MarketDataFreshnessThresholds(
            maxBidAgeMillis = 1_000L,
            maxAskAgeMillis = 1_000L,
            maxBidAskDifferenceMillis = 1_000L,
            allowedFutureSkewMillis = 0L
        )
    )

    private fun debugSourcePolicy() = MarketDataSourcePolicy(
        MarketDataSourcePolicyConfig(
            rules = listOf(
                MarketDataSourceRule(
                    sourceId = DeutscheBoerseKnockoutProductMarketDataMapper.SOURCE_ID,
                    supportedCalculationTypes = setOf(MarketDataCalculationType.MID)
                )
            )
        )
    )

    private fun debugExecutionSettings() = TradePlannerSelectionExecutionSettings(
        calculationType = MarketDataCalculationType.MID,
        maxFxAgeMillis = 1_000L,
        maxRelativeLeverageDeviationPercent = 100.0,
        maxBarrierDeviationPercentOfPlannedEntry = 10.0
    )

    private object DebugBrokerAvailabilityProvider : KnockoutProductBrokerAvailabilityProvider {

        override suspend fun findTradableProductIsins(
            brokerId: String,
            productIsins: List<String>
        ): KnockoutProductBrokerAvailabilityProviderResult =
            KnockoutProductBrokerAvailabilityProviderResult.Success(
                tradableProductIsins = productIsins.filterTo(linkedSetOf()) {
                    it == PRODUCT_A_ISIN
                }
            )
    }

    private object DebugFxRateProvider : FxRateProvider {

        override suspend fun findRate(
            underlyingCurrency: CurrencyCode,
            productCurrency: CurrencyCode
        ): FxRateProviderResult = FxRateProviderResult.NotFound
    }

    private sealed interface FixtureCreationResult {

        data class Success(
            val fixtures: DeviceDemoFixtures
        ) : FixtureCreationResult

        data class Failure(
            val error: HsbcDeutscheBoerseTradePlannerSelectionDeviceDemoError
        ) : FixtureCreationResult
    }

    private data class DeviceDemoFixtures(
        val dxscGzipFile: File,
        val xfraZipFile: File,
        val hsbcFilesByProductIsin: Map<String, File>
    )

    private const val FIXTURE_DIRECTORY_NAME = "ko-selection-demo"
    private const val NVIDIA_UNDERLYING_ID = "nvidia"
    private const val DEBUG_BROKER_ID = "synthetic-broker"
    private const val SYNTHETIC_ISSUER_ID = "synthetic-issuer"
    private const val PRODUCT_A_ISIN = "DE000SYNTH01"
    private const val PRODUCT_A_WKN = "SYN001"
    private const val PRODUCT_B_ISIN = "DE000SYNTH02"
    private const val PRODUCT_B_WKN = "SYN002"
    private const val MARKET_DATA_TIMESTAMP = "2026-07-27T19:29:57Z"
    private const val MARKET_DATA_TIMESTAMP_EPOCH_MILLIS = 1_785_180_597_000L
    private const val SPECIFICATION_RETRIEVED_AT_EPOCH_MILLIS = 1_700_000_000_500L
}
