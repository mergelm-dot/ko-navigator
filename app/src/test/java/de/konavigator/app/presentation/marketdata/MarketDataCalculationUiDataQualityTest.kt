package de.konavigator.app.presentation.marketdata

import de.konavigator.app.domain.dataquality.DataQualityAssessment
import de.konavigator.app.domain.dataquality.DataQualityCategory
import de.konavigator.app.domain.dataquality.DataQualityComponent
import de.konavigator.app.domain.dataquality.DataQualityFinding
import de.konavigator.app.domain.dataquality.DataQualityFindingCode
import de.konavigator.app.domain.dataquality.DataQualitySeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketDataCalculationUiDataQualityTest {

    @Test
    fun passedAssessmentMapsToPassedWithoutFindings() {
        val result = DataQualityAssessment.passed().toUiDataQuality()

        assertEquals(MarketDataCalculationUiDataQualityStatus.PASSED, result.status)
        assertTrue(result.findings.isEmpty())
    }

    @Test
    fun warningAssessmentPreservesFindingOrderCountAndFields() {
        val assessment = DataQualityAssessment.warning(
            listOf(
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
        )

        val result = assessment.toUiDataQuality()

        assertEquals(MarketDataCalculationUiDataQualityStatus.WARNING, result.status)
        assertEquals(
            listOf(
                MarketDataCalculationUiDataQualityFinding(
                    category = MarketDataCalculationUiDataQualityCategory.INVALID_IDENTIFIER,
                    severity = MarketDataCalculationUiDataQualitySeverity.WARNING,
                    code =
                        MarketDataCalculationUiDataQualityFindingCode
                            .SPECIFICATION_INVALID_PRODUCT_WKN,
                    component =
                        MarketDataCalculationUiDataQualityComponent.PRODUCT_SPECIFICATION
                ),
                MarketDataCalculationUiDataQualityFinding(
                    category =
                        MarketDataCalculationUiDataQualityCategory.INCONSISTENT_TIMESTAMPS,
                    severity = MarketDataCalculationUiDataQualitySeverity.WARNING,
                    code =
                        MarketDataCalculationUiDataQualityFindingCode
                            .MARKET_DATA_ORPHAN_ASK_TIMESTAMP,
                    component = MarketDataCalculationUiDataQualityComponent.PRODUCT_MARKET_DATA
                )
            ),
            result.findings
        )
    }

    @Test
    fun blockedAssessmentPreservesAllFindingFields() {
        val assessment = DataQualityAssessment.blocked(
            listOf(
                DataQualityFinding(
                    category = DataQualityCategory.INCONSISTENT_CURRENCIES,
                    severity = DataQualitySeverity.BLOCKING,
                    code =
                        DataQualityFindingCode.COMPATIBILITY_PRODUCT_CURRENCY_MISMATCH,
                    component = DataQualityComponent.CROSS_MODEL_COMPATIBILITY
                )
            )
        )

        val result = assessment.toUiDataQuality()

        assertEquals(MarketDataCalculationUiDataQualityStatus.BLOCKED, result.status)
        assertEquals(
            listOf(
                MarketDataCalculationUiDataQualityFinding(
                    category =
                        MarketDataCalculationUiDataQualityCategory.INCONSISTENT_CURRENCIES,
                    severity = MarketDataCalculationUiDataQualitySeverity.BLOCKING,
                    code =
                        MarketDataCalculationUiDataQualityFindingCode
                            .COMPATIBILITY_PRODUCT_CURRENCY_MISMATCH,
                    component =
                        MarketDataCalculationUiDataQualityComponent.CROSS_MODEL_COMPATIBILITY
                )
            ),
            result.findings
        )
    }
}
