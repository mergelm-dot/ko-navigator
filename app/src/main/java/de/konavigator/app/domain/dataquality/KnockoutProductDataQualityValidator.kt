package de.konavigator.app.domain.dataquality

import de.konavigator.app.domain.model.KnockoutProductMarketData
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.validation.KnockoutProductCompatibilityError
import de.konavigator.app.domain.validation.KnockoutProductMarketDataCompatibilityValidator
import de.konavigator.app.domain.validation.KnockoutProductMarketDataValidationError
import de.konavigator.app.domain.validation.KnockoutProductMarketDataValidator
import de.konavigator.app.domain.validation.KnockoutProductSpecificationValidator
import de.konavigator.app.domain.validation.KnockoutProductValidationError

/**
 * Koordiniert ausschließlich die vorhandenen strukturellen Validatoren.
 *
 * Die bestehenden Validatoren bleiben Single Source of Truth ihrer Regeln und
 * Fehlerreihenfolgen. Cross-Model-Kompatibilität wird nur für zwei intern
 * gültige Modelle geprüft. Es werden keine Werte normalisiert, korrigiert oder
 * berechnet und weder Systemzeit noch externe Daten gelesen.
 */
object KnockoutProductDataQualityValidator {

    fun assess(
        specification: KnockoutProductSpecification,
        marketData: KnockoutProductMarketData
    ): DataQualityAssessment {
        val specificationErrors = KnockoutProductSpecificationValidator.validate(specification)
        val marketDataErrors = KnockoutProductMarketDataValidator.validate(marketData)
        val findings = mutableListOf<DataQualityFinding>()

        findings += specificationErrors.map { it.toDataQualityFinding() }
        findings += marketDataErrors.map { it.toDataQualityFinding() }

        if (specificationErrors.isEmpty() && marketDataErrors.isEmpty()) {
            findings += KnockoutProductMarketDataCompatibilityValidator.validate(
                specification = specification,
                marketData = marketData
            ).map { it.toDataQualityFinding() }
        }

        return if (findings.isEmpty()) {
            DataQualityAssessment.passed()
        } else {
            DataQualityAssessment.blocked(findings)
        }
    }

    private fun KnockoutProductValidationError.toDataQualityFinding(): DataQualityFinding {
        val category = when (this) {
            KnockoutProductValidationError.MISSING_PRODUCT_ISIN,
            KnockoutProductValidationError.MISSING_ISSUER_ID,
            KnockoutProductValidationError.MISSING_UNDERLYING_ID ->
                DataQualityCategory.MISSING_REQUIRED_DATA

            KnockoutProductValidationError.INVALID_PRODUCT_WKN ->
                DataQualityCategory.INVALID_IDENTIFIER

            KnockoutProductValidationError.INVALID_BASE_PRICE,
            KnockoutProductValidationError.INVALID_KNOCKOUT_BARRIER ->
                DataQualityCategory.INVALID_NUMERIC_VALUE

            KnockoutProductValidationError.INVALID_RATIO ->
                DataQualityCategory.INVALID_RATIO

            KnockoutProductValidationError.INVALID_UNDERLYING_CURRENCY,
            KnockoutProductValidationError.INVALID_PRODUCT_CURRENCY ->
                DataQualityCategory.INVALID_CURRENCY
        }
        val code = when (this) {
            KnockoutProductValidationError.MISSING_PRODUCT_ISIN ->
                DataQualityFindingCode.SPECIFICATION_MISSING_PRODUCT_ISIN
            KnockoutProductValidationError.INVALID_PRODUCT_WKN ->
                DataQualityFindingCode.SPECIFICATION_INVALID_PRODUCT_WKN
            KnockoutProductValidationError.MISSING_ISSUER_ID ->
                DataQualityFindingCode.SPECIFICATION_MISSING_ISSUER_ID
            KnockoutProductValidationError.MISSING_UNDERLYING_ID ->
                DataQualityFindingCode.SPECIFICATION_MISSING_UNDERLYING_ID
            KnockoutProductValidationError.INVALID_BASE_PRICE ->
                DataQualityFindingCode.SPECIFICATION_INVALID_BASE_PRICE
            KnockoutProductValidationError.INVALID_KNOCKOUT_BARRIER ->
                DataQualityFindingCode.SPECIFICATION_INVALID_KNOCKOUT_BARRIER
            KnockoutProductValidationError.INVALID_RATIO ->
                DataQualityFindingCode.SPECIFICATION_INVALID_RATIO
            KnockoutProductValidationError.INVALID_UNDERLYING_CURRENCY ->
                DataQualityFindingCode.SPECIFICATION_INVALID_UNDERLYING_CURRENCY
            KnockoutProductValidationError.INVALID_PRODUCT_CURRENCY ->
                DataQualityFindingCode.SPECIFICATION_INVALID_PRODUCT_CURRENCY
        }

        return DataQualityFinding(
            category = category,
            severity = DataQualitySeverity.BLOCKING,
            code = code,
            component = DataQualityComponent.PRODUCT_SPECIFICATION
        )
    }

    private fun KnockoutProductMarketDataValidationError.toDataQualityFinding():
        DataQualityFinding {
        val category = when (this) {
            KnockoutProductMarketDataValidationError.MISSING_PRODUCT_ISIN,
            KnockoutProductMarketDataValidationError.MISSING_SOURCE_ID ->
                DataQualityCategory.MISSING_REQUIRED_DATA

            KnockoutProductMarketDataValidationError.INVALID_CURRENCY ->
                DataQualityCategory.INVALID_CURRENCY

            KnockoutProductMarketDataValidationError.INVALID_BID,
            KnockoutProductMarketDataValidationError.INVALID_ASK ->
                DataQualityCategory.INVALID_NUMERIC_VALUE

            KnockoutProductMarketDataValidationError.MISSING_BID_TIMESTAMP,
            KnockoutProductMarketDataValidationError.ORPHAN_BID_TIMESTAMP,
            KnockoutProductMarketDataValidationError.MISSING_ASK_TIMESTAMP,
            KnockoutProductMarketDataValidationError.ORPHAN_ASK_TIMESTAMP ->
                DataQualityCategory.INCONSISTENT_TIMESTAMPS

            KnockoutProductMarketDataValidationError.BID_ABOVE_ASK ->
                DataQualityCategory.INVALID_QUOTE_RELATION
        }
        val code = when (this) {
            KnockoutProductMarketDataValidationError.MISSING_PRODUCT_ISIN ->
                DataQualityFindingCode.MARKET_DATA_MISSING_PRODUCT_ISIN
            KnockoutProductMarketDataValidationError.MISSING_SOURCE_ID ->
                DataQualityFindingCode.MARKET_DATA_MISSING_SOURCE_ID
            KnockoutProductMarketDataValidationError.INVALID_CURRENCY ->
                DataQualityFindingCode.MARKET_DATA_INVALID_CURRENCY
            KnockoutProductMarketDataValidationError.INVALID_BID ->
                DataQualityFindingCode.MARKET_DATA_INVALID_BID
            KnockoutProductMarketDataValidationError.MISSING_BID_TIMESTAMP ->
                DataQualityFindingCode.MARKET_DATA_MISSING_BID_TIMESTAMP
            KnockoutProductMarketDataValidationError.ORPHAN_BID_TIMESTAMP ->
                DataQualityFindingCode.MARKET_DATA_ORPHAN_BID_TIMESTAMP
            KnockoutProductMarketDataValidationError.INVALID_ASK ->
                DataQualityFindingCode.MARKET_DATA_INVALID_ASK
            KnockoutProductMarketDataValidationError.MISSING_ASK_TIMESTAMP ->
                DataQualityFindingCode.MARKET_DATA_MISSING_ASK_TIMESTAMP
            KnockoutProductMarketDataValidationError.ORPHAN_ASK_TIMESTAMP ->
                DataQualityFindingCode.MARKET_DATA_ORPHAN_ASK_TIMESTAMP
            KnockoutProductMarketDataValidationError.BID_ABOVE_ASK ->
                DataQualityFindingCode.MARKET_DATA_BID_ABOVE_ASK
        }

        return DataQualityFinding(
            category = category,
            severity = DataQualitySeverity.BLOCKING,
            code = code,
            component = DataQualityComponent.PRODUCT_MARKET_DATA
        )
    }

    private fun KnockoutProductCompatibilityError.toDataQualityFinding(): DataQualityFinding {
        val category = when (this) {
            KnockoutProductCompatibilityError.PRODUCT_ISIN_MISMATCH ->
                DataQualityCategory.INCONSISTENT_IDENTIFIERS
            KnockoutProductCompatibilityError.PRODUCT_CURRENCY_MISMATCH ->
                DataQualityCategory.INCONSISTENT_CURRENCIES
        }
        val code = when (this) {
            KnockoutProductCompatibilityError.PRODUCT_ISIN_MISMATCH ->
                DataQualityFindingCode.COMPATIBILITY_PRODUCT_ISIN_MISMATCH
            KnockoutProductCompatibilityError.PRODUCT_CURRENCY_MISMATCH ->
                DataQualityFindingCode.COMPATIBILITY_PRODUCT_CURRENCY_MISMATCH
        }

        return DataQualityFinding(
            category = category,
            severity = DataQualitySeverity.BLOCKING,
            code = code,
            component = DataQualityComponent.CROSS_MODEL_COMPATIBILITY
        )
    }
}
