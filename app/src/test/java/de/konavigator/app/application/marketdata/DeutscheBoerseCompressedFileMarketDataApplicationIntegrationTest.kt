package de.konavigator.app.application.marketdata

import de.konavigator.app.data.remote.DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult
import de.konavigator.app.data.remote.DeutscheBoerseCompressedFileMarketDataRepositoryFactory
import de.konavigator.app.data.remote.RemoteKnockoutProductSpecificationRepository
import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationDto
import de.konavigator.app.data.remote.provider.InMemoryKnockoutProductSpecificationProvider
import de.konavigator.app.data.remote.provider.deutscheboerse.DeutscheBoerseXfraRequiredColumn
import de.konavigator.app.domain.availability.MarketDataCalculationType
import de.konavigator.app.domain.dataquality.DataQualityFindingCode
import de.konavigator.app.domain.dataquality.DataQualityStatus
import de.konavigator.app.domain.freshness.MarketDataFreshnessPolicy
import de.konavigator.app.domain.freshness.MarketDataFreshnessThresholds
import de.konavigator.app.domain.orchestration.MarketDataCalculationOrchestrationResult
import de.konavigator.app.domain.orchestration.MarketDataCalculationOrchestrator
import de.konavigator.app.domain.orchestration.MarketDataCalculationValue
import de.konavigator.app.domain.source.MarketDataSourcePolicy
import de.konavigator.app.domain.source.MarketDataSourcePolicyConfig
import de.konavigator.app.domain.source.MarketDataSourceRule
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DeutscheBoerseCompressedFileMarketDataApplicationIntegrationTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun validLocalFilesCreateMarketDataRepository() {
        val files = validFiles()

        assertTrue(
            repositoryCreation(files) is
                DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult.Success
        )
    }

    @Test
    fun applicationServiceReturnsDomainEvaluated() = runTest {
        assertTrue(execute(validFiles()) is MarketDataCalculationApplicationResult.DomainEvaluated)
    }

    @Test
    fun validFilesProduceDomainSuccess() = runTest {
        assertTrue(domainResult(execute(validFiles())) is MarketDataCalculationOrchestrationResult.Success)
    }

    @Test
    fun validFilesProducePurchasePriceValue() = runTest {
        val result = domainSuccess(execute(validFiles()))

        assertTrue(result.value is MarketDataCalculationValue.PurchasePrice)
    }

    @Test
    fun purchasePriceUsesAskFromNewestDxscUpdate() = runTest {
        val value = purchasePrice(execute(validFiles()))

        assertEquals(2.05, value.value, 0.0)
    }

    @Test
    fun purchasePriceCurrencyIsEuro() = runTest {
        assertEquals("EUR", purchasePrice(execute(validFiles())).currency)
    }

    @Test
    fun validLocalDataProducesPassedAssessment() = runTest {
        assertEquals(
            DataQualityStatus.PASSED,
            domainSuccess(execute(validFiles())).dataQualityAssessment.status
        )
    }

    @Test
    fun olderDxscUpdateIsNotUsedForPurchasePrice() = runTest {
        assertFalse(purchasePrice(execute(validFiles())).value == 2.00)
    }

    @Test
    fun latestSelectionOccursInExistingProviderChainWithoutTestSorting() = runTest {
        val files = files(
            xfraLines = listOf(xfraLine(PRODUCT_ISIN)),
            dxscLines = listOf(
                dxscLine(PRODUCT_ISIN, bid = 1.85, ask = 2.05, timestamp = NEWER_TIMESTAMP),
                dxscLine(PRODUCT_ISIN, bid = 1.80, ask = 2.00, timestamp = OLDER_TIMESTAMP)
            )
        )

        assertEquals(2.05, purchasePrice(execute(files)).value, 0.0)
    }

    @Test
    fun staleDxscTimestampProducesNotFresh() = runTest {
        val result = execute(
            validFiles(),
            evaluationTimeEpochMillis = evaluationTime() + FRESHNESS_LIMIT_MILLIS + 1L
        )

        assertTrue(domainResult(result) is MarketDataCalculationOrchestrationResult.NotFresh)
    }

    @Test
    fun nullAskWithSharedTimestampIsBlockedAsOrphanAskTimestamp() = runTest {
        val files = files(
            xfraLines = listOf(xfraLine(PRODUCT_ISIN)),
            dxscLines = listOf(dxscLine(PRODUCT_ISIN, bid = 1.85, ask = null))
        )

        assertBlockedFinding(
            result = execute(files),
            expectedCode = DataQualityFindingCode.MARKET_DATA_ORPHAN_ASK_TIMESTAMP
        )
    }

    @Test
    fun currencyMismatchProducesStructuralDataQualityBlocked() = runTest {
        val files = files(
            xfraLines = listOf(xfraLine(PRODUCT_ISIN, currency = "USD")),
            dxscLines = listOf(dxscLine(PRODUCT_ISIN))
        )

        assertTrue(
            domainResult(execute(files)) is
                MarketDataCalculationOrchestrationResult.StructuralDataQualityBlocked
        )
    }

    @Test
    fun duplicateXfraRecordsProduceApplicationDataAccessFailure() = runTest {
        val files = files(
            xfraLines = listOf(xfraLine(PRODUCT_ISIN), xfraLine(PRODUCT_ISIN)),
            dxscLines = listOf(dxscLine(PRODUCT_ISIN))
        )

        assertEquals(
            MarketDataCalculationApplicationResult.DataUnavailable(
                MarketDataCalculationApplicationError.DATA_ACCESS_FAILURE
            ),
            execute(files)
        )
    }

    @Test
    fun missingDxscRecordProducesMarketDataNotFound() = runTest {
        val files = files(
            xfraLines = listOf(xfraLine(PRODUCT_ISIN)),
            dxscLines = listOf(dxscLine(OTHER_ISIN))
        )

        assertEquals(
            MarketDataCalculationApplicationResult.DataUnavailable(
                MarketDataCalculationApplicationError.MARKET_DATA_NOT_FOUND
            ),
            execute(files)
        )
    }

    @Test
    fun differentlyCasedIsinIsNotNormalized() = runTest {
        val requestedIsin = PRODUCT_ISIN.lowercase()
        val files = validFiles()

        assertEquals(
            MarketDataCalculationApplicationResult.DataUnavailable(
                MarketDataCalculationApplicationError.MARKET_DATA_NOT_FOUND
            ),
            execute(
                files = files,
                requestIsin = requestedIsin,
                specificationIsin = requestedIsin
            )
        )
    }

    @Test
    fun whitespaceInIsinIsNotTrimmed() = runTest {
        val requestedIsin = " $PRODUCT_ISIN "
        val files = validFiles()

        assertEquals(
            MarketDataCalculationApplicationResult.DataUnavailable(
                MarketDataCalculationApplicationError.MARKET_DATA_NOT_FOUND
            ),
            execute(
                files = files,
                requestIsin = requestedIsin,
                specificationIsin = requestedIsin
            )
        )
    }

    @Test
    fun nullBidWithSharedTimestampIsBlockedAsOrphanBidTimestamp() = runTest {
        val files = files(
            xfraLines = listOf(xfraLine(PRODUCT_ISIN)),
            dxscLines = listOf(dxscLine(PRODUCT_ISIN, bid = null, ask = 2.05))
        )

        assertBlockedFinding(
            result = execute(files),
            expectedCode = DataQualityFindingCode.MARKET_DATA_ORPHAN_BID_TIMESTAMP
        )
    }

    @Test
    fun zeroAskIsNotCorrectedAndIsBlockedAsInvalidAsk() = runTest {
        val files = files(
            xfraLines = listOf(xfraLine(PRODUCT_ISIN)),
            dxscLines = listOf(dxscLine(PRODUCT_ISIN, bid = 0.0, ask = 0.0))
        )

        assertBlockedFinding(
            result = execute(files),
            expectedCode = DataQualityFindingCode.MARKET_DATA_INVALID_ASK
        )
    }

    @Test
    fun configuredDeutscheBoerseSourceIsAccepted() = runTest {
        assertTrue(
            domainResult(execute(validFiles(), sourceAllowed = true)) is
                MarketDataCalculationOrchestrationResult.Success
        )
    }

    @Test
    fun unconfiguredDeutscheBoerseSourceIsBlocked() = runTest {
        assertTrue(
            domainResult(execute(validFiles(), sourceAllowed = false)) is
                MarketDataCalculationOrchestrationResult.SourceBlocked
        )
    }

    @Test
    fun temporaryFilesCanBeDeletedAfterApplicationExecution() = runTest {
        val files = validFiles()

        execute(files)

        assertTrue(files.xfra.delete())
        assertTrue(files.dxsc.delete())
    }

    private suspend fun execute(
        files: CompressedFiles,
        requestIsin: String = PRODUCT_ISIN,
        specificationIsin: String = requestIsin,
        specificationCurrency: String = "EUR",
        evaluationTimeEpochMillis: Long = evaluationTime(),
        sourceAllowed: Boolean = true
    ): MarketDataCalculationApplicationResult {
        val repositoryResult = repositoryCreation(
            files = files,
            requestedProductIsins = setOf(requestIsin)
        ) as DeutscheBoerseCompressedFileMarketDataRepositoryCreationResult.Success
        val specificationRepository = RemoteKnockoutProductSpecificationRepository(
            InMemoryKnockoutProductSpecificationProvider(
                mapOf(
                    requestIsin to specificationDto(
                        productIsin = specificationIsin,
                        productCurrency = specificationCurrency
                    )
                )
            )
        )
        val service = MarketDataCalculationApplicationService(
            specificationRepository = specificationRepository,
            marketDataRepository = repositoryResult.repository,
            orchestrator = MarketDataCalculationOrchestrator(
                freshnessPolicy = MarketDataFreshnessPolicy(
                    MarketDataFreshnessThresholds(
                        maxBidAgeMillis = FRESHNESS_LIMIT_MILLIS,
                        maxAskAgeMillis = FRESHNESS_LIMIT_MILLIS,
                        maxBidAskDifferenceMillis = FRESHNESS_LIMIT_MILLIS,
                        allowedFutureSkewMillis = 0L
                    )
                ),
                sourcePolicy = sourcePolicy(sourceAllowed)
            )
        )

        return service.execute(
            MarketDataCalculationApplicationRequest(
                productIsin = requestIsin,
                calculationType = MarketDataCalculationType.PURCHASE_PRICE,
                evaluationTimeEpochMillis = evaluationTimeEpochMillis
            )
        )
    }

    private fun repositoryCreation(
        files: CompressedFiles,
        requestedProductIsins: Set<String> = setOf(PRODUCT_ISIN)
    ) = DeutscheBoerseCompressedFileMarketDataRepositoryFactory.create(
        dxscGzipFile = files.dxsc,
        xfraZipFile = files.xfra,
        requestedProductIsins = requestedProductIsins
    )

    private fun sourcePolicy(
        sourceAllowed: Boolean
    ): MarketDataSourcePolicy = MarketDataSourcePolicy(
        MarketDataSourcePolicyConfig(
            rules = if (sourceAllowed) {
                listOf(
                    MarketDataSourceRule(
                        sourceId = SOURCE_ID,
                        supportedCalculationTypes = setOf(
                            MarketDataCalculationType.PURCHASE_PRICE
                        )
                    )
                )
            } else {
                emptyList()
            }
        )
    )

    private fun domainResult(
        result: MarketDataCalculationApplicationResult
    ): MarketDataCalculationOrchestrationResult =
        (result as MarketDataCalculationApplicationResult.DomainEvaluated).domainResult

    private fun domainSuccess(
        result: MarketDataCalculationApplicationResult
    ): MarketDataCalculationOrchestrationResult.Success =
        domainResult(result) as MarketDataCalculationOrchestrationResult.Success

    private fun purchasePrice(
        result: MarketDataCalculationApplicationResult
    ): MarketDataCalculationValue.PurchasePrice =
        domainSuccess(result).value as MarketDataCalculationValue.PurchasePrice

    private fun assertBlockedFinding(
        result: MarketDataCalculationApplicationResult,
        expectedCode: DataQualityFindingCode
    ) {
        assertTrue(result is MarketDataCalculationApplicationResult.DomainEvaluated)
        val domainResult = domainResult(result)
        assertTrue(
            domainResult is
                MarketDataCalculationOrchestrationResult.StructuralDataQualityBlocked
        )
        assertEquals(DataQualityStatus.BLOCKED, domainResult.dataQualityAssessment.status)
        assertTrue(
            domainResult.dataQualityAssessment.findings.any { finding ->
                finding.code == expectedCode
            }
        )
    }

    private fun specificationDto(
        productIsin: String,
        productCurrency: String
    ) = KnockoutProductSpecificationDto(
        productIsin = productIsin,
        productWkn = "TEST01",
        issuerId = "test-issuer",
        underlyingId = "test-underlying",
        direction = "LONG",
        basePrice = 80.0,
        knockoutBarrier = 82.0,
        ratio = 0.1,
        underlyingCurrency = "EUR",
        productCurrency = productCurrency
    )

    private fun validFiles(): CompressedFiles = files(
        xfraLines = listOf(xfraLine(PRODUCT_ISIN)),
        dxscLines = listOf(
            dxscLine(PRODUCT_ISIN, bid = 1.80, ask = 2.00, timestamp = OLDER_TIMESTAMP),
            dxscLine(PRODUCT_ISIN, bid = 1.85, ask = 2.05, timestamp = NEWER_TIMESTAMP)
        )
    )

    private fun files(
        xfraLines: List<String>,
        dxscLines: List<String>
    ) = CompressedFiles(
        dxsc = writeDxscGzip(dxscLines),
        xfra = writeXfraZip(xfraLines)
    )

    private fun writeDxscGzip(lines: List<String>): File =
        temporaryFolder.newFile("dxsc-${fileCounter++}.json.gz").also { file ->
            GZIPOutputStream(FileOutputStream(file))
                .bufferedWriter(StandardCharsets.UTF_8)
                .use { writer ->
                    writer.write(lines.joinToString(separator = "\n", postfix = "\n"))
                }
        }

    private fun writeXfraZip(lines: List<String>): File =
        temporaryFolder.newFile("xfra-${fileCounter++}.zip").also { file ->
            ZipOutputStream(FileOutputStream(file), StandardCharsets.UTF_8).use { zip ->
                zip.putNextEntry(ZipEntry("reference"))
                zip.write(csvContent(lines).toByteArray(StandardCharsets.UTF_8))
                zip.closeEntry()
            }
        }

    private fun csvContent(lines: List<String>): String =
        listOf("Market:;XFRA", "Date Last Update:;27.07.2026", validHeader())
            .plus(lines)
            .joinToString(separator = "\n", postfix = "\n")

    private fun validHeader(): String = DeutscheBoerseXfraRequiredColumn.entries
        .joinToString(";") { it.headerName }

    private fun xfraLine(
        isin: String,
        currency: String = "EUR"
    ): String = listOf(
        "Active",
        "Tradable",
        "Synthetic Instrument",
        isin,
        "TEST01",
        "XFRA",
        "Warrant",
        "EUR",
        currency,
        "Call",
        "08:00",
        "22:00"
    ).joinToString(";")

    private fun dxscLine(
        isin: String,
        bid: Double? = 1.85,
        ask: Double? = 2.05,
        timestamp: String = NEWER_TIMESTAMP
    ): String {
        val jsonBid = bid?.toString() ?: "null"
        val jsonAsk = ask?.toString() ?: "null"
        return """{"messageId":"pretrade","instrumentIdentificationCode":"$isin","bestBid":$jsonBid,"bestAsk":$jsonAsk,"updateDateAndTime":"$timestamp"}"""
    }

    private fun evaluationTime(): Long = Instant.parse(NEWER_TIMESTAMP).toEpochMilli()

    private data class CompressedFiles(
        val dxsc: File,
        val xfra: File
    )

    private var fileCounter = 0

    private companion object {
        const val PRODUCT_ISIN = "DE000TEST001"
        const val OTHER_ISIN = "DE000TEST002"
        const val SOURCE_ID = "DEUTSCHE_BOERSE_DXSC_DELAYED"
        const val OLDER_TIMESTAMP = "2026-07-27T19:29:56Z"
        const val NEWER_TIMESTAMP = "2026-07-27T19:29:57Z"
        const val FRESHNESS_LIMIT_MILLIS = 1_000L
    }
}
