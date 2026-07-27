package de.konavigator.app.presentation.marketdata

import de.konavigator.app.application.marketdata.MarketDataCalculationApplicationError
import de.konavigator.app.application.marketdata.MarketDataCalculationApplicationResult
import de.konavigator.app.domain.dataquality.DataQualityAssessment
import de.konavigator.app.domain.dataquality.DataQualityCategory
import de.konavigator.app.domain.dataquality.DataQualityComponent
import de.konavigator.app.domain.dataquality.DataQualityFinding
import de.konavigator.app.domain.dataquality.DataQualityFindingCode
import de.konavigator.app.domain.dataquality.DataQualitySeverity
import de.konavigator.app.domain.orchestration.MarketDataCalculationOrchestrationResult
import de.konavigator.app.domain.orchestration.MarketDataCalculationValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MarketDataCalculationUiResultTest {

    @Test
    fun invalidProductSpecificationMapsToInvalidSpecification() {
        val result = toUiResult(
            MarketDataCalculationApplicationError.INVALID_PRODUCT_SPECIFICATION
        )

        assertEquals(
            MarketDataCalculationUiResult.Failure(
                error = MarketDataCalculationUiError.INVALID_SPECIFICATION,
                dataQuality = null
            ),
            result
        )
    }

    @Test
    fun invalidProductMarketDataMapsToInvalidMarketData() {
        val result = toUiResult(
            MarketDataCalculationApplicationError.INVALID_PRODUCT_MARKET_DATA
        )

        assertEquals(
            MarketDataCalculationUiResult.Failure(
                error = MarketDataCalculationUiError.INVALID_MARKET_DATA,
                dataQuality = null
            ),
            result
        )
    }

    @Test
    fun invalidProductErrorsMapToDistinctUiErrors() {
        val specificationResult = toUiResult(
            MarketDataCalculationApplicationError.INVALID_PRODUCT_SPECIFICATION
        )
        val marketDataResult = toUiResult(
            MarketDataCalculationApplicationError.INVALID_PRODUCT_MARKET_DATA
        )

        assertNotEquals(specificationResult, marketDataResult)
    }

    @Test
    fun domainSuccessPreservesWarningAssessmentAndFindingOrder() {
        val findings = listOf(
            DataQualityFinding(
                category = DataQualityCategory.INVALID_IDENTIFIER,
                severity = DataQualitySeverity.WARNING,
                code = DataQualityFindingCode.SPECIFICATION_INVALID_PRODUCT_WKN,
                component = DataQualityComponent.PRODUCT_SPECIFICATION
            ),
            DataQualityFinding(
                category = DataQualityCategory.INCONSISTENT_TIMESTAMPS,
                severity = DataQualitySeverity.WARNING,
                code = DataQualityFindingCode.MARKET_DATA_ORPHAN_ASK_TIMESTAMP,
                component = DataQualityComponent.PRODUCT_MARKET_DATA
            )
        )

        val result = MarketDataCalculationApplicationResult.DomainEvaluated(
            MarketDataCalculationOrchestrationResult.Success(
                value = MarketDataCalculationValue.PurchasePrice(2.125, "EUR"),
                dataQualityAssessment = DataQualityAssessment.warning(findings)
            )
        ).toUiResult()

        assertEquals(
            MarketDataCalculationUiResult.PurchasePrice(
                value = 2.125,
                currency = "EUR",
                dataQuality = DataQualityAssessment.warning(findings).toUiDataQuality()
            ),
            result
        )
    }

    @Test
    fun structuralDataQualityBlockedPreservesBlockedAssessmentAndFinding() {
        val finding = DataQualityFinding(
            category = DataQualityCategory.INVALID_NUMERIC_VALUE,
            severity = DataQualitySeverity.BLOCKING,
            code = DataQualityFindingCode.MARKET_DATA_INVALID_BID,
            component = DataQualityComponent.PRODUCT_MARKET_DATA
        )
        val assessment = DataQualityAssessment.blocked(listOf(finding))

        val result = MarketDataCalculationApplicationResult.DomainEvaluated(
            MarketDataCalculationOrchestrationResult.StructuralDataQualityBlocked(
                assessment
            )
        ).toUiResult()

        assertEquals(
            MarketDataCalculationUiResult.Failure(
                error = MarketDataCalculationUiError.INVALID_MARKET_DATA,
                dataQuality = assessment.toUiDataQuality()
            ),
            result
        )
    }

    private fun toUiResult(
        error: MarketDataCalculationApplicationError
    ): MarketDataCalculationUiResult =
        MarketDataCalculationApplicationResult.DataUnavailable(error).toUiResult()
}
