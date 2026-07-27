package de.konavigator.app.data.remote.provider.deutscheboerse

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

enum class DeutscheBoerseDxscJsonLineParsingErrorCode {
    INVALID_JSON,
    ROOT_NOT_OBJECT,
    INVALID_MESSAGE_ID_TYPE,
    INVALID_INSTRUMENT_IDENTIFICATION_CODE_TYPE,
    INVALID_BEST_BID_TYPE,
    INVALID_BEST_BID_QUANTITY_TYPE,
    INVALID_BEST_ASK_TYPE,
    INVALID_BEST_ASK_QUANTITY_TYPE,
    INVALID_UPDATE_DATE_AND_TIME_TYPE
}

sealed interface DeutscheBoerseDxscJsonLineParsingResult {

    data class Success(
        val record: DeutscheBoerseDxscPretradeRecord
    ) : DeutscheBoerseDxscJsonLineParsingResult

    data class Failure(
        val errors: List<DeutscheBoerseDxscJsonLineParsingErrorCode>
    ) : DeutscheBoerseDxscJsonLineParsingResult
}

object DeutscheBoerseDxscPretradeJsonLineParser {

    fun parse(line: String): DeutscheBoerseDxscJsonLineParsingResult {
        val element = try {
            Json.parseToJsonElement(line)
        } catch (_: SerializationException) {
            return failure(DeutscheBoerseDxscJsonLineParsingErrorCode.INVALID_JSON)
        } catch (_: IllegalArgumentException) {
            return failure(DeutscheBoerseDxscJsonLineParsingErrorCode.INVALID_JSON)
        }
        if (element !is JsonObject) {
            return failure(DeutscheBoerseDxscJsonLineParsingErrorCode.ROOT_NOT_OBJECT)
        }

        val errors = mutableListOf<DeutscheBoerseDxscJsonLineParsingErrorCode>()
        val messageId = element.stringValue(
            fieldName = "messageId",
            errorCode =
                DeutscheBoerseDxscJsonLineParsingErrorCode.INVALID_MESSAGE_ID_TYPE,
            errors = errors
        )
        val instrumentIdentificationCode = element.stringValue(
            fieldName = "instrumentIdentificationCode",
            errorCode =
                DeutscheBoerseDxscJsonLineParsingErrorCode
                    .INVALID_INSTRUMENT_IDENTIFICATION_CODE_TYPE,
            errors = errors
        )
        val bestBid = element.doubleValue(
            fieldName = "bestBid",
            errorCode = DeutscheBoerseDxscJsonLineParsingErrorCode.INVALID_BEST_BID_TYPE,
            errors = errors
        )
        val bestBidQuantity = element.doubleValue(
            fieldName = "bestBidQty",
            errorCode =
                DeutscheBoerseDxscJsonLineParsingErrorCode
                    .INVALID_BEST_BID_QUANTITY_TYPE,
            errors = errors
        )
        val bestAsk = element.doubleValue(
            fieldName = "bestAsk",
            errorCode = DeutscheBoerseDxscJsonLineParsingErrorCode.INVALID_BEST_ASK_TYPE,
            errors = errors
        )
        val bestAskQuantity = element.doubleValue(
            fieldName = "bestAskQty",
            errorCode =
                DeutscheBoerseDxscJsonLineParsingErrorCode
                    .INVALID_BEST_ASK_QUANTITY_TYPE,
            errors = errors
        )
        val updateDateAndTime = element.stringValue(
            fieldName = "updateDateAndTime",
            errorCode =
                DeutscheBoerseDxscJsonLineParsingErrorCode
                    .INVALID_UPDATE_DATE_AND_TIME_TYPE,
            errors = errors
        )
        if (errors.isNotEmpty()) {
            return DeutscheBoerseDxscJsonLineParsingResult.Failure(errors)
        }

        return DeutscheBoerseDxscJsonLineParsingResult.Success(
            DeutscheBoerseDxscPretradeRecord(
                messageId = messageId,
                instrumentIdentificationCode = instrumentIdentificationCode,
                bestBid = bestBid,
                bestBidQuantity = bestBidQuantity,
                bestAsk = bestAsk,
                bestAskQuantity = bestAskQuantity,
                updateDateAndTime = updateDateAndTime
            )
        )
    }

    private fun JsonObject.stringValue(
        fieldName: String,
        errorCode: DeutscheBoerseDxscJsonLineParsingErrorCode,
        errors: MutableList<DeutscheBoerseDxscJsonLineParsingErrorCode>
    ): String? {
        val value = this[fieldName]
        if (value == null || value is JsonNull) {
            return null
        }
        if (value !is JsonPrimitive || !value.isString) {
            errors += errorCode
            return null
        }
        return value.content
    }

    private fun JsonObject.doubleValue(
        fieldName: String,
        errorCode: DeutscheBoerseDxscJsonLineParsingErrorCode,
        errors: MutableList<DeutscheBoerseDxscJsonLineParsingErrorCode>
    ): Double? {
        val value = this[fieldName]
        if (value == null || value is JsonNull) {
            return null
        }
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

    private fun failure(
        error: DeutscheBoerseDxscJsonLineParsingErrorCode
    ) = DeutscheBoerseDxscJsonLineParsingResult.Failure(listOf(error))
}
