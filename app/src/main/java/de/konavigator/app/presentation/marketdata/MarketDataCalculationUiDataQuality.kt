package de.konavigator.app.presentation.marketdata

import de.konavigator.app.domain.dataquality.DataQualityAssessment
import de.konavigator.app.domain.dataquality.DataQualityCategory
import de.konavigator.app.domain.dataquality.DataQualityComponent
import de.konavigator.app.domain.dataquality.DataQualityFinding
import de.konavigator.app.domain.dataquality.DataQualityFindingCode
import de.konavigator.app.domain.dataquality.DataQualitySeverity
import de.konavigator.app.domain.dataquality.DataQualityStatus

data class MarketDataCalculationUiDataQuality(
    val status: MarketDataCalculationUiDataQualityStatus,
    val findings: List<MarketDataCalculationUiDataQualityFinding>
)

data class MarketDataCalculationUiDataQualityFinding(
    val category: MarketDataCalculationUiDataQualityCategory,
    val severity: MarketDataCalculationUiDataQualitySeverity,
    val code: MarketDataCalculationUiDataQualityFindingCode,
    val component: MarketDataCalculationUiDataQualityComponent
)

enum class MarketDataCalculationUiDataQualityStatus {
    PASSED,
    WARNING,
    BLOCKED
}

enum class MarketDataCalculationUiDataQualitySeverity {
    WARNING,
    BLOCKING
}

enum class MarketDataCalculationUiDataQualityCategory {
    MISSING_REQUIRED_DATA,
    INVALID_IDENTIFIER,
    INVALID_NUMERIC_VALUE,
    INVALID_CURRENCY,
    INVALID_RATIO,
    INVALID_QUOTE_RELATION,
    INCONSISTENT_IDENTIFIERS,
    INCONSISTENT_CURRENCIES,
    INCONSISTENT_TIMESTAMPS
}

enum class MarketDataCalculationUiDataQualityComponent {
    PRODUCT_SPECIFICATION,
    PRODUCT_MARKET_DATA,
    CROSS_MODEL_COMPATIBILITY
}

enum class MarketDataCalculationUiDataQualityFindingCode {
    SPECIFICATION_MISSING_PRODUCT_ISIN,
    SPECIFICATION_INVALID_PRODUCT_WKN,
    SPECIFICATION_MISSING_ISSUER_ID,
    SPECIFICATION_MISSING_UNDERLYING_ID,
    SPECIFICATION_INVALID_BASE_PRICE,
    SPECIFICATION_INVALID_KNOCKOUT_BARRIER,
    SPECIFICATION_INVALID_RATIO,
    SPECIFICATION_INVALID_UNDERLYING_CURRENCY,
    SPECIFICATION_INVALID_PRODUCT_CURRENCY,
    MARKET_DATA_MISSING_PRODUCT_ISIN,
    MARKET_DATA_MISSING_SOURCE_ID,
    MARKET_DATA_INVALID_CURRENCY,
    MARKET_DATA_INVALID_BID,
    MARKET_DATA_MISSING_BID_TIMESTAMP,
    MARKET_DATA_ORPHAN_BID_TIMESTAMP,
    MARKET_DATA_INVALID_ASK,
    MARKET_DATA_MISSING_ASK_TIMESTAMP,
    MARKET_DATA_ORPHAN_ASK_TIMESTAMP,
    MARKET_DATA_BID_ABOVE_ASK,
    COMPATIBILITY_PRODUCT_ISIN_MISMATCH,
    COMPATIBILITY_PRODUCT_CURRENCY_MISMATCH
}

internal fun DataQualityAssessment.toUiDataQuality(): MarketDataCalculationUiDataQuality =
    MarketDataCalculationUiDataQuality(
        status = status.toUiStatus(),
        findings = findings.map(DataQualityFinding::toUiFinding)
    )

private fun DataQualityStatus.toUiStatus(): MarketDataCalculationUiDataQualityStatus =
    when (this) {
        DataQualityStatus.PASSED -> MarketDataCalculationUiDataQualityStatus.PASSED
        DataQualityStatus.WARNING -> MarketDataCalculationUiDataQualityStatus.WARNING
        DataQualityStatus.BLOCKED -> MarketDataCalculationUiDataQualityStatus.BLOCKED
    }

private fun DataQualityFinding.toUiFinding(): MarketDataCalculationUiDataQualityFinding =
    MarketDataCalculationUiDataQualityFinding(
        category = category.toUiCategory(),
        severity = severity.toUiSeverity(),
        code = code.toUiFindingCode(),
        component = component.toUiComponent()
    )

private fun DataQualitySeverity.toUiSeverity(): MarketDataCalculationUiDataQualitySeverity =
    when (this) {
        DataQualitySeverity.WARNING -> MarketDataCalculationUiDataQualitySeverity.WARNING
        DataQualitySeverity.BLOCKING -> MarketDataCalculationUiDataQualitySeverity.BLOCKING
    }

private fun DataQualityCategory.toUiCategory(): MarketDataCalculationUiDataQualityCategory =
    when (this) {
        DataQualityCategory.MISSING_REQUIRED_DATA ->
            MarketDataCalculationUiDataQualityCategory.MISSING_REQUIRED_DATA

        DataQualityCategory.INVALID_IDENTIFIER ->
            MarketDataCalculationUiDataQualityCategory.INVALID_IDENTIFIER

        DataQualityCategory.INVALID_NUMERIC_VALUE ->
            MarketDataCalculationUiDataQualityCategory.INVALID_NUMERIC_VALUE

        DataQualityCategory.INVALID_CURRENCY ->
            MarketDataCalculationUiDataQualityCategory.INVALID_CURRENCY

        DataQualityCategory.INVALID_RATIO ->
            MarketDataCalculationUiDataQualityCategory.INVALID_RATIO

        DataQualityCategory.INVALID_QUOTE_RELATION ->
            MarketDataCalculationUiDataQualityCategory.INVALID_QUOTE_RELATION

        DataQualityCategory.INCONSISTENT_IDENTIFIERS ->
            MarketDataCalculationUiDataQualityCategory.INCONSISTENT_IDENTIFIERS

        DataQualityCategory.INCONSISTENT_CURRENCIES ->
            MarketDataCalculationUiDataQualityCategory.INCONSISTENT_CURRENCIES

        DataQualityCategory.INCONSISTENT_TIMESTAMPS ->
            MarketDataCalculationUiDataQualityCategory.INCONSISTENT_TIMESTAMPS
    }

private fun DataQualityComponent.toUiComponent(): MarketDataCalculationUiDataQualityComponent =
    when (this) {
        DataQualityComponent.PRODUCT_SPECIFICATION ->
            MarketDataCalculationUiDataQualityComponent.PRODUCT_SPECIFICATION

        DataQualityComponent.PRODUCT_MARKET_DATA ->
            MarketDataCalculationUiDataQualityComponent.PRODUCT_MARKET_DATA

        DataQualityComponent.CROSS_MODEL_COMPATIBILITY ->
            MarketDataCalculationUiDataQualityComponent.CROSS_MODEL_COMPATIBILITY
    }

private fun DataQualityFindingCode.toUiFindingCode():
    MarketDataCalculationUiDataQualityFindingCode = when (this) {
    DataQualityFindingCode.SPECIFICATION_MISSING_PRODUCT_ISIN ->
        MarketDataCalculationUiDataQualityFindingCode.SPECIFICATION_MISSING_PRODUCT_ISIN

    DataQualityFindingCode.SPECIFICATION_INVALID_PRODUCT_WKN ->
        MarketDataCalculationUiDataQualityFindingCode.SPECIFICATION_INVALID_PRODUCT_WKN

    DataQualityFindingCode.SPECIFICATION_MISSING_ISSUER_ID ->
        MarketDataCalculationUiDataQualityFindingCode.SPECIFICATION_MISSING_ISSUER_ID

    DataQualityFindingCode.SPECIFICATION_MISSING_UNDERLYING_ID ->
        MarketDataCalculationUiDataQualityFindingCode.SPECIFICATION_MISSING_UNDERLYING_ID

    DataQualityFindingCode.SPECIFICATION_INVALID_BASE_PRICE ->
        MarketDataCalculationUiDataQualityFindingCode.SPECIFICATION_INVALID_BASE_PRICE

    DataQualityFindingCode.SPECIFICATION_INVALID_KNOCKOUT_BARRIER ->
        MarketDataCalculationUiDataQualityFindingCode.SPECIFICATION_INVALID_KNOCKOUT_BARRIER

    DataQualityFindingCode.SPECIFICATION_INVALID_RATIO ->
        MarketDataCalculationUiDataQualityFindingCode.SPECIFICATION_INVALID_RATIO

    DataQualityFindingCode.SPECIFICATION_INVALID_UNDERLYING_CURRENCY ->
        MarketDataCalculationUiDataQualityFindingCode.SPECIFICATION_INVALID_UNDERLYING_CURRENCY

    DataQualityFindingCode.SPECIFICATION_INVALID_PRODUCT_CURRENCY ->
        MarketDataCalculationUiDataQualityFindingCode.SPECIFICATION_INVALID_PRODUCT_CURRENCY

    DataQualityFindingCode.MARKET_DATA_MISSING_PRODUCT_ISIN ->
        MarketDataCalculationUiDataQualityFindingCode.MARKET_DATA_MISSING_PRODUCT_ISIN

    DataQualityFindingCode.MARKET_DATA_MISSING_SOURCE_ID ->
        MarketDataCalculationUiDataQualityFindingCode.MARKET_DATA_MISSING_SOURCE_ID

    DataQualityFindingCode.MARKET_DATA_INVALID_CURRENCY ->
        MarketDataCalculationUiDataQualityFindingCode.MARKET_DATA_INVALID_CURRENCY

    DataQualityFindingCode.MARKET_DATA_INVALID_BID ->
        MarketDataCalculationUiDataQualityFindingCode.MARKET_DATA_INVALID_BID

    DataQualityFindingCode.MARKET_DATA_MISSING_BID_TIMESTAMP ->
        MarketDataCalculationUiDataQualityFindingCode.MARKET_DATA_MISSING_BID_TIMESTAMP

    DataQualityFindingCode.MARKET_DATA_ORPHAN_BID_TIMESTAMP ->
        MarketDataCalculationUiDataQualityFindingCode.MARKET_DATA_ORPHAN_BID_TIMESTAMP

    DataQualityFindingCode.MARKET_DATA_INVALID_ASK ->
        MarketDataCalculationUiDataQualityFindingCode.MARKET_DATA_INVALID_ASK

    DataQualityFindingCode.MARKET_DATA_MISSING_ASK_TIMESTAMP ->
        MarketDataCalculationUiDataQualityFindingCode.MARKET_DATA_MISSING_ASK_TIMESTAMP

    DataQualityFindingCode.MARKET_DATA_ORPHAN_ASK_TIMESTAMP ->
        MarketDataCalculationUiDataQualityFindingCode.MARKET_DATA_ORPHAN_ASK_TIMESTAMP

    DataQualityFindingCode.MARKET_DATA_BID_ABOVE_ASK ->
        MarketDataCalculationUiDataQualityFindingCode.MARKET_DATA_BID_ABOVE_ASK

    DataQualityFindingCode.COMPATIBILITY_PRODUCT_ISIN_MISMATCH ->
        MarketDataCalculationUiDataQualityFindingCode.COMPATIBILITY_PRODUCT_ISIN_MISMATCH

    DataQualityFindingCode.COMPATIBILITY_PRODUCT_CURRENCY_MISMATCH ->
        MarketDataCalculationUiDataQualityFindingCode.COMPATIBILITY_PRODUCT_CURRENCY_MISMATCH
}
