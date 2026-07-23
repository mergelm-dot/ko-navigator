package de.konavigator.app.domain.dataquality

import de.konavigator.app.domain.model.KnockoutProductMarketData
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.TradeDirection
import de.konavigator.app.domain.validation.KnockoutProductMarketDataValidationError
import de.konavigator.app.domain.validation.KnockoutProductValidationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductDataQualityValidatorTest {

    @Test
    fun validCompatibleModelsProducePassedAssessment() {
        assertEquals(DataQualityStatus.PASSED, assess().status)
    }

    @Test
    fun passedAssessmentContainsNoFindings() {
        assertTrue(assess().findings.isEmpty())
    }

    @Test
    fun structuralErrorProducesBlockedAssessment() {
        val assessment = assess(specification = createSpecification(basePrice = 0.0))

        assertEquals(DataQualityStatus.BLOCKED, assessment.status)
    }

    @Test
    fun blockedAssessmentContainsAtLeastOneFinding() {
        val assessment = assess(marketData = createMarketData(ask = 0.0))

        assertTrue(assessment.findings.isNotEmpty())
    }

    @Test
    fun validatorNeverProducesWarningStatusOrSeverity() {
        val assessments = listOf(
            assess(),
            assess(specification = completelyInvalidSpecification()),
            assess(marketData = completelyInvalidMarketData()),
            assess(marketData = createMarketData(productIsin = "OTHER", currency = "USD"))
        )

        assertTrue(assessments.none { it.status == DataQualityStatus.WARNING })
        assertTrue(
            assessments.flatMap { it.findings }
                .none { it.severity == DataQualitySeverity.WARNING }
        )
    }

    @Test
    fun findingsFollowSpecificationThenMarketDataOrder() {
        val assessment = assess(
            specification = createSpecification(basePrice = 0.0),
            marketData = createMarketData(ask = 0.0)
        )

        assertEquals(
            listOf(
                DataQualityFindingCode.SPECIFICATION_INVALID_BASE_PRICE,
                DataQualityFindingCode.MARKET_DATA_INVALID_ASK
            ),
            assessment.findings.map { it.code }
        )
    }

    @Test
    fun identicalInputsProduceExactlyEqualAssessments() {
        val specification = completelyInvalidSpecification()
        val marketData = completelyInvalidMarketData()

        assertEquals(
            assess(specification, marketData),
            assess(specification, marketData)
        )
    }

    @Test
    fun assessmentDoesNotMutateInputModels() {
        val specification = completelyInvalidSpecification()
        val marketData = completelyInvalidMarketData()
        val originalSpecification = specification.copy()
        val originalMarketData = marketData.copy()

        assess(specification, marketData)

        assertEquals(originalSpecification, specification)
        assertEquals(originalMarketData, marketData)
    }

    @Test
    fun everySpecificationErrorMapsToItsOwnFinding() {
        val cases = listOf(
            SpecificationMappingCase(
                createSpecification(productIsin = ""),
                DataQualityFindingCode.SPECIFICATION_MISSING_PRODUCT_ISIN,
                DataQualityCategory.MISSING_REQUIRED_DATA
            ),
            SpecificationMappingCase(
                createSpecification(productWkn = ""),
                DataQualityFindingCode.SPECIFICATION_INVALID_PRODUCT_WKN,
                DataQualityCategory.INVALID_IDENTIFIER
            ),
            SpecificationMappingCase(
                createSpecification(issuerId = ""),
                DataQualityFindingCode.SPECIFICATION_MISSING_ISSUER_ID,
                DataQualityCategory.MISSING_REQUIRED_DATA
            ),
            SpecificationMappingCase(
                createSpecification(underlyingId = ""),
                DataQualityFindingCode.SPECIFICATION_MISSING_UNDERLYING_ID,
                DataQualityCategory.MISSING_REQUIRED_DATA
            ),
            SpecificationMappingCase(
                createSpecification(basePrice = 0.0),
                DataQualityFindingCode.SPECIFICATION_INVALID_BASE_PRICE,
                DataQualityCategory.INVALID_NUMERIC_VALUE
            ),
            SpecificationMappingCase(
                createSpecification(knockoutBarrier = Double.NaN),
                DataQualityFindingCode.SPECIFICATION_INVALID_KNOCKOUT_BARRIER,
                DataQualityCategory.INVALID_NUMERIC_VALUE
            ),
            SpecificationMappingCase(
                createSpecification(ratio = -1.0),
                DataQualityFindingCode.SPECIFICATION_INVALID_RATIO,
                DataQualityCategory.INVALID_RATIO
            ),
            SpecificationMappingCase(
                createSpecification(underlyingCurrency = "usd"),
                DataQualityFindingCode.SPECIFICATION_INVALID_UNDERLYING_CURRENCY,
                DataQualityCategory.INVALID_CURRENCY
            ),
            SpecificationMappingCase(
                createSpecification(productCurrency = "eur"),
                DataQualityFindingCode.SPECIFICATION_INVALID_PRODUCT_CURRENCY,
                DataQualityCategory.INVALID_CURRENCY
            )
        )

        assertEquals(KnockoutProductValidationError.entries.size, cases.size)
        cases.forEach { case ->
            assertEquals(
                listOf(
                    finding(
                        code = case.code,
                        category = case.category,
                        component = DataQualityComponent.PRODUCT_SPECIFICATION
                    )
                ),
                assess(specification = case.specification).findings
            )
        }
    }

    @Test
    fun multipleSpecificationErrorsRemainCompleteAndOrdered() {
        val findings = assess(specification = completelyInvalidSpecification()).findings

        assertEquals(ALL_SPECIFICATION_CODES, findings.map { it.code })
    }

    @Test
    fun specificationFindingsUseSpecificationComponent() {
        val findings = assess(specification = completelyInvalidSpecification()).findings

        assertTrue(findings.all { it.component == DataQualityComponent.PRODUCT_SPECIFICATION })
    }

    @Test
    fun specificationFindingsAreAlwaysBlocking() {
        val findings = assess(specification = completelyInvalidSpecification()).findings

        assertTrue(findings.all { it.severity == DataQualitySeverity.BLOCKING })
    }

    @Test
    fun everyMarketDataErrorMapsToItsOwnFinding() {
        val cases = listOf(
            MarketDataMappingCase(
                createMarketData(productIsin = ""),
                DataQualityFindingCode.MARKET_DATA_MISSING_PRODUCT_ISIN,
                DataQualityCategory.MISSING_REQUIRED_DATA
            ),
            MarketDataMappingCase(
                createMarketData(sourceId = ""),
                DataQualityFindingCode.MARKET_DATA_MISSING_SOURCE_ID,
                DataQualityCategory.MISSING_REQUIRED_DATA
            ),
            MarketDataMappingCase(
                createMarketData(currency = "eur"),
                DataQualityFindingCode.MARKET_DATA_INVALID_CURRENCY,
                DataQualityCategory.INVALID_CURRENCY
            ),
            MarketDataMappingCase(
                createMarketData(bid = -1.0),
                DataQualityFindingCode.MARKET_DATA_INVALID_BID,
                DataQualityCategory.INVALID_NUMERIC_VALUE
            ),
            MarketDataMappingCase(
                createMarketData(bidTimestampEpochMillis = null),
                DataQualityFindingCode.MARKET_DATA_MISSING_BID_TIMESTAMP,
                DataQualityCategory.INCONSISTENT_TIMESTAMPS
            ),
            MarketDataMappingCase(
                createMarketData(bid = null),
                DataQualityFindingCode.MARKET_DATA_ORPHAN_BID_TIMESTAMP,
                DataQualityCategory.INCONSISTENT_TIMESTAMPS
            ),
            MarketDataMappingCase(
                createMarketData(ask = 0.0),
                DataQualityFindingCode.MARKET_DATA_INVALID_ASK,
                DataQualityCategory.INVALID_NUMERIC_VALUE
            ),
            MarketDataMappingCase(
                createMarketData(askTimestampEpochMillis = null),
                DataQualityFindingCode.MARKET_DATA_MISSING_ASK_TIMESTAMP,
                DataQualityCategory.INCONSISTENT_TIMESTAMPS
            ),
            MarketDataMappingCase(
                createMarketData(ask = null),
                DataQualityFindingCode.MARKET_DATA_ORPHAN_ASK_TIMESTAMP,
                DataQualityCategory.INCONSISTENT_TIMESTAMPS
            ),
            MarketDataMappingCase(
                createMarketData(bid = 2.01, ask = 2.0),
                DataQualityFindingCode.MARKET_DATA_BID_ABOVE_ASK,
                DataQualityCategory.INVALID_QUOTE_RELATION
            )
        )

        assertEquals(KnockoutProductMarketDataValidationError.entries.size, cases.size)
        cases.forEach { case ->
            assertEquals(
                listOf(
                    finding(
                        code = case.code,
                        category = case.category,
                        component = DataQualityComponent.PRODUCT_MARKET_DATA
                    )
                ),
                assess(marketData = case.marketData).findings
            )
        }
    }

    @Test
    fun multipleMarketDataErrorsRemainCompleteAndOrdered() {
        val findings = assess(marketData = completelyInvalidMarketData()).findings

        assertEquals(ALL_MARKET_DATA_CODES, findings.map { it.code })
    }

    @Test
    fun marketDataFindingsUseMarketDataComponent() {
        val findings = assess(marketData = completelyInvalidMarketData()).findings

        assertTrue(findings.all { it.component == DataQualityComponent.PRODUCT_MARKET_DATA })
    }

    @Test
    fun marketDataFindingsAreAlwaysBlocking() {
        val findings = assess(marketData = completelyInvalidMarketData()).findings

        assertTrue(findings.all { it.severity == DataQualitySeverity.BLOCKING })
    }

    @Test
    fun compatibleModelsProduceNoCompatibilityFindings() {
        assertTrue(
            assess().findings.none {
                it.component == DataQualityComponent.CROSS_MODEL_COMPATIBILITY
            }
        )
    }

    @Test
    fun productIsinMismatchMapsToStructuredFinding() {
        val findings = assess(marketData = createMarketData(productIsin = "OTHER")).findings

        assertEquals(
            listOf(
                finding(
                    code = DataQualityFindingCode.COMPATIBILITY_PRODUCT_ISIN_MISMATCH,
                    category = DataQualityCategory.INCONSISTENT_IDENTIFIERS,
                    component = DataQualityComponent.CROSS_MODEL_COMPATIBILITY
                )
            ),
            findings
        )
    }

    @Test
    fun productCurrencyMismatchMapsToStructuredFinding() {
        val findings = assess(marketData = createMarketData(currency = "USD")).findings

        assertEquals(
            listOf(
                finding(
                    code = DataQualityFindingCode.COMPATIBILITY_PRODUCT_CURRENCY_MISMATCH,
                    category = DataQualityCategory.INCONSISTENT_CURRENCIES,
                    component = DataQualityComponent.CROSS_MODEL_COMPATIBILITY
                )
            ),
            findings
        )
    }

    @Test
    fun allCompatibilityErrorsRemainCompleteAndOrdered() {
        val findings = assess(
            marketData = createMarketData(productIsin = "OTHER", currency = "USD")
        ).findings

        assertEquals(
            listOf(
                DataQualityFindingCode.COMPATIBILITY_PRODUCT_ISIN_MISMATCH,
                DataQualityFindingCode.COMPATIBILITY_PRODUCT_CURRENCY_MISMATCH
            ),
            findings.map { it.code }
        )
        assertTrue(findings.all { it.severity == DataQualitySeverity.BLOCKING })
    }

    @Test
    fun compatibilityIsSkippedWhenSpecificationIsInvalid() {
        val findings = assess(
            specification = createSpecification(productIsin = ""),
            marketData = createMarketData(productIsin = "OTHER")
        ).findings

        assertEquals(
            listOf(DataQualityFindingCode.SPECIFICATION_MISSING_PRODUCT_ISIN),
            findings.map { it.code }
        )
    }

    @Test
    fun compatibilityIsSkippedWhenMarketDataIsInvalid() {
        val findings = assess(
            marketData = createMarketData(productIsin = "OTHER", currency = "usd")
        ).findings

        assertEquals(
            listOf(DataQualityFindingCode.MARKET_DATA_INVALID_CURRENCY),
            findings.map { it.code }
        )
    }

    @Test
    fun assessmentTakesDefensiveSnapshotOfFindings() {
        val mutableFindings = mutableListOf(
            finding(
                code = DataQualityFindingCode.SPECIFICATION_INVALID_BASE_PRICE,
                category = DataQualityCategory.INVALID_NUMERIC_VALUE,
                component = DataQualityComponent.PRODUCT_SPECIFICATION
            )
        )
        val assessment = DataQualityAssessment.blocked(mutableFindings)

        mutableFindings.clear()

        assertEquals(1, assessment.findings.size)
    }

    @Test
    fun assessmentFindingsCannotBeMutatedThroughExposedList() {
        val assessment = assess(specification = createSpecification(basePrice = 0.0))

        assertThrows(UnsupportedOperationException::class.java) {
            (assessment.findings as MutableList).clear()
        }
    }

    @Test
    fun blockedAssessmentRejectsEmptyFindings() {
        assertThrows(IllegalArgumentException::class.java) {
            DataQualityAssessment.blocked(emptyList())
        }
    }

    @Test
    fun warningContractRejectsBlockingFindings() {
        val blockingFinding = finding(
            code = DataQualityFindingCode.SPECIFICATION_INVALID_BASE_PRICE,
            category = DataQualityCategory.INVALID_NUMERIC_VALUE,
            component = DataQualityComponent.PRODUCT_SPECIFICATION
        )

        assertThrows(IllegalArgumentException::class.java) {
            DataQualityAssessment.warning(listOf(blockingFinding))
        }
    }

    @Test
    fun assessmentPerformsNoNormalizationOrSilentCorrection() {
        val specification = createSpecification(
            productIsin = " DE000TEST001 ",
            underlyingCurrency = "usd"
        )
        val marketData = createMarketData(productIsin = "DE000TEST001")

        val assessment = assess(specification, marketData)

        assertEquals(" DE000TEST001 ", specification.productIsin)
        assertEquals("usd", specification.underlyingCurrency)
        assertEquals("DE000TEST001", marketData.productIsin)
        assertFalse(assessment.findings.isEmpty())
    }

    private fun assess(
        specification: KnockoutProductSpecification = createSpecification(),
        marketData: KnockoutProductMarketData = createMarketData()
    ): DataQualityAssessment {
        return KnockoutProductDataQualityValidator.assess(specification, marketData)
    }

    private fun finding(
        code: DataQualityFindingCode,
        category: DataQualityCategory,
        component: DataQualityComponent
    ): DataQualityFinding {
        return DataQualityFinding(
            category = category,
            severity = DataQualitySeverity.BLOCKING,
            code = code,
            component = component
        )
    }

    private fun completelyInvalidSpecification(): KnockoutProductSpecification {
        return createSpecification(
            productIsin = "",
            productWkn = "",
            issuerId = "",
            underlyingId = "",
            basePrice = Double.NaN,
            knockoutBarrier = Double.POSITIVE_INFINITY,
            ratio = -1.0,
            underlyingCurrency = "usd",
            productCurrency = "eur"
        )
    }

    private fun completelyInvalidMarketData(): KnockoutProductMarketData {
        return createMarketData(
            productIsin = "",
            sourceId = "",
            currency = "eur",
            bid = Double.NaN,
            ask = 0.0,
            bidTimestampEpochMillis = null,
            askTimestampEpochMillis = null
        )
    }

    private fun createSpecification(
        productIsin: String = "DE000TEST001",
        productWkn: String? = "TEST01",
        issuerId: String = "issuer-1",
        underlyingId: String = "underlying-1",
        direction: TradeDirection = TradeDirection.LONG,
        basePrice: Double = 80.0,
        knockoutBarrier: Double = 82.0,
        ratio: Double = 0.1,
        underlyingCurrency: String = "USD",
        productCurrency: String = "EUR"
    ): KnockoutProductSpecification {
        return KnockoutProductSpecification(
            productIsin = productIsin,
            productWkn = productWkn,
            issuerId = issuerId,
            underlyingId = underlyingId,
            direction = direction,
            basePrice = basePrice,
            knockoutBarrier = knockoutBarrier,
            ratio = ratio,
            underlyingCurrency = underlyingCurrency,
            productCurrency = productCurrency
        )
    }

    private fun createMarketData(
        productIsin: String = "DE000TEST001",
        bid: Double? = 1.95,
        ask: Double? = 2.0,
        bidTimestampEpochMillis: Long? = 1_700_000_000_000L,
        askTimestampEpochMillis: Long? = 1_700_000_000_100L,
        currency: String = "EUR",
        sourceId: String = "source-1"
    ): KnockoutProductMarketData {
        return KnockoutProductMarketData(
            productIsin = productIsin,
            bid = bid,
            ask = ask,
            bidTimestampEpochMillis = bidTimestampEpochMillis,
            askTimestampEpochMillis = askTimestampEpochMillis,
            currency = currency,
            sourceId = sourceId
        )
    }

    private data class SpecificationMappingCase(
        val specification: KnockoutProductSpecification,
        val code: DataQualityFindingCode,
        val category: DataQualityCategory
    )

    private data class MarketDataMappingCase(
        val marketData: KnockoutProductMarketData,
        val code: DataQualityFindingCode,
        val category: DataQualityCategory
    )

    private companion object {
        val ALL_SPECIFICATION_CODES = listOf(
            DataQualityFindingCode.SPECIFICATION_MISSING_PRODUCT_ISIN,
            DataQualityFindingCode.SPECIFICATION_INVALID_PRODUCT_WKN,
            DataQualityFindingCode.SPECIFICATION_MISSING_ISSUER_ID,
            DataQualityFindingCode.SPECIFICATION_MISSING_UNDERLYING_ID,
            DataQualityFindingCode.SPECIFICATION_INVALID_BASE_PRICE,
            DataQualityFindingCode.SPECIFICATION_INVALID_KNOCKOUT_BARRIER,
            DataQualityFindingCode.SPECIFICATION_INVALID_RATIO,
            DataQualityFindingCode.SPECIFICATION_INVALID_UNDERLYING_CURRENCY,
            DataQualityFindingCode.SPECIFICATION_INVALID_PRODUCT_CURRENCY
        )

        val ALL_MARKET_DATA_CODES = listOf(
            DataQualityFindingCode.MARKET_DATA_MISSING_PRODUCT_ISIN,
            DataQualityFindingCode.MARKET_DATA_MISSING_SOURCE_ID,
            DataQualityFindingCode.MARKET_DATA_INVALID_CURRENCY,
            DataQualityFindingCode.MARKET_DATA_INVALID_BID,
            DataQualityFindingCode.MARKET_DATA_MISSING_BID_TIMESTAMP,
            DataQualityFindingCode.MARKET_DATA_INVALID_ASK,
            DataQualityFindingCode.MARKET_DATA_MISSING_ASK_TIMESTAMP
        )
    }
}
