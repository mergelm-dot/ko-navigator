package de.konavigator.app.data.remote.provider.hsbc

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

enum class HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode {
    INVALID_JSON,
    ROOT_NOT_OBJECT,
    UNEXPECTED_FIELD,
    INVALID_PRODUCT_ISIN_TYPE,
    INVALID_PRODUCT_WKN_TYPE,
    INVALID_ISSUER_ID_TYPE,
    INVALID_UNDERLYING_ID_TYPE,
    INVALID_DIRECTION_LABEL_TYPE,
    INVALID_BASE_PRICE_TYPE,
    INVALID_KNOCKOUT_BARRIER_TYPE,
    INVALID_RATIO_TYPE,
    INVALID_UNDERLYING_CURRENCY_TYPE,
    INVALID_PRODUCT_CURRENCY_TYPE,
    INVALID_SOURCE_TIMESTAMP_EPOCH_MILLIS_TYPE
}

sealed interface HsbcKnockoutProductSpecificationResearchJsonParsingResult {

    data class Success(
        val record: HsbcKnockoutProductSpecificationRecord
    ) : HsbcKnockoutProductSpecificationResearchJsonParsingResult

    data class Failure(
        val errors: List<HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode>
    ) : HsbcKnockoutProductSpecificationResearchJsonParsingResult
}

/**
 * Parser für ein eigenes, kontrolliertes lokales Forschungsformat.
 *
 * Die Eingaben müssen vorab bereinigt und ausdrücklich freigegeben sein. Der Parser bildet
 * weder eine originale HSBC-Antwort noch eine Vaadin-Antwort ab. Eine strikte Liste zulässiger
 * Felder schützt vor der versehentlichen Verarbeitung fremder Antwortstrukturen.
 *
 * Fehlende und explizit auf `null` gesetzte Werte bleiben `null`. Es erfolgen keine
 * Normalisierung, fachliche Interpretation, Data-Quality-, Freshness- oder
 * Berechnungsentscheidung sowie keine Systemzeit oder Zeitumrechnung. Der Parser hat keine
 * Netzwerk-, Datei-, Provider- oder Repository-Verantwortung.
 */
object HsbcKnockoutProductSpecificationResearchJsonParser {

    fun parse(json: String): HsbcKnockoutProductSpecificationResearchJsonParsingResult {
        val element = try {
            Json.parseToJsonElement(json)
        } catch (_: SerializationException) {
            return failure(HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode.INVALID_JSON)
        } catch (_: IllegalArgumentException) {
            return failure(HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode.INVALID_JSON)
        }
        if (element !is JsonObject) {
            return failure(
                HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode.ROOT_NOT_OBJECT
            )
        }

        val errors = mutableListOf<HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode>()
        if (element.keys.any { it !in allowedFields }) {
            errors += HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode.UNEXPECTED_FIELD
        }

        val productIsin = element.stringValue(
            "productIsin",
            HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode
                .INVALID_PRODUCT_ISIN_TYPE,
            errors
        )
        val productWkn = element.stringValue(
            "productWkn",
            HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode
                .INVALID_PRODUCT_WKN_TYPE,
            errors
        )
        val issuerId = element.stringValue(
            "issuerId",
            HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode.INVALID_ISSUER_ID_TYPE,
            errors
        )
        val underlyingId = element.stringValue(
            "underlyingId",
            HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode
                .INVALID_UNDERLYING_ID_TYPE,
            errors
        )
        val directionLabel = element.stringValue(
            "directionLabel",
            HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode
                .INVALID_DIRECTION_LABEL_TYPE,
            errors
        )
        val basePrice = element.doubleValue(
            "basePrice",
            HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode.INVALID_BASE_PRICE_TYPE,
            errors
        )
        val knockoutBarrier = element.doubleValue(
            "knockoutBarrier",
            HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode
                .INVALID_KNOCKOUT_BARRIER_TYPE,
            errors
        )
        val ratio = element.doubleValue(
            "ratio",
            HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode.INVALID_RATIO_TYPE,
            errors
        )
        val underlyingCurrency = element.stringValue(
            "underlyingCurrency",
            HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode
                .INVALID_UNDERLYING_CURRENCY_TYPE,
            errors
        )
        val productCurrency = element.stringValue(
            "productCurrency",
            HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode
                .INVALID_PRODUCT_CURRENCY_TYPE,
            errors
        )
        val sourceTimestampEpochMillis = element.longValue(
            "sourceTimestampEpochMillis",
            HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode
                .INVALID_SOURCE_TIMESTAMP_EPOCH_MILLIS_TYPE,
            errors
        )

        if (errors.isNotEmpty()) {
            return HsbcKnockoutProductSpecificationResearchJsonParsingResult.Failure(errors)
        }
        return HsbcKnockoutProductSpecificationResearchJsonParsingResult.Success(
            HsbcKnockoutProductSpecificationRecord(
                productIsin = productIsin,
                productWkn = productWkn,
                issuerId = issuerId,
                underlyingId = underlyingId,
                directionLabel = directionLabel,
                basePrice = basePrice,
                knockoutBarrier = knockoutBarrier,
                ratio = ratio,
                underlyingCurrency = underlyingCurrency,
                productCurrency = productCurrency,
                sourceTimestampEpochMillis = sourceTimestampEpochMillis
            )
        )
    }

    private fun JsonObject.stringValue(
        fieldName: String,
        errorCode: HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode,
        errors: MutableList<HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode>
    ): String? {
        val value = this[fieldName]
        if (value == null || value is JsonNull) return null
        if (value !is JsonPrimitive || !value.isString) {
            errors += errorCode
            return null
        }
        return value.content
    }

    private fun JsonObject.doubleValue(
        fieldName: String,
        errorCode: HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode,
        errors: MutableList<HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode>
    ): Double? {
        val value = this[fieldName]
        if (value == null || value is JsonNull) return null
        val number = if (value is JsonPrimitive && !value.isString) {
            value.content.toDoubleOrNull()
        } else {
            null
        }
        if (number == null || !number.isFinite()) {
            errors += errorCode
            return null
        }
        return number
    }

    private fun JsonObject.longValue(
        fieldName: String,
        errorCode: HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode,
        errors: MutableList<HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode>
    ): Long? {
        val value = this[fieldName]
        if (value == null || value is JsonNull) return null
        val content = if (value is JsonPrimitive && !value.isString) value.content else null
        val number = if (content != null && integerPattern.matches(content)) {
            content.toLongOrNull()
        } else {
            null
        }
        if (number == null) {
            errors += errorCode
            return null
        }
        return number
    }

    private fun failure(
        error: HsbcKnockoutProductSpecificationResearchJsonParsingErrorCode
    ) = HsbcKnockoutProductSpecificationResearchJsonParsingResult.Failure(listOf(error))

    private val allowedFields = setOf(
        "productIsin",
        "productWkn",
        "issuerId",
        "underlyingId",
        "directionLabel",
        "basePrice",
        "knockoutBarrier",
        "ratio",
        "underlyingCurrency",
        "productCurrency",
        "sourceTimestampEpochMillis"
    )

    private val integerPattern = Regex("-?(0|[1-9][0-9]*)")
}
