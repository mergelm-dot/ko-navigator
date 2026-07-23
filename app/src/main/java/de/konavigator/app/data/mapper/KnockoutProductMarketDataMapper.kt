package de.konavigator.app.data.mapper

import de.konavigator.app.data.remote.dto.KnockoutProductMarketDataDto
import de.konavigator.app.domain.model.KnockoutProductMarketData

enum class KnockoutProductMarketDataDtoField {
    PRODUCT_ISIN,
    CURRENCY,
    SOURCE_ID
}

enum class KnockoutProductMarketDataMappingErrorReason {
    MISSING_REQUIRED_VALUE
}

data class KnockoutProductMarketDataMappingError(
    val field: KnockoutProductMarketDataDtoField,
    val reason: KnockoutProductMarketDataMappingErrorReason
)

sealed interface KnockoutProductMarketDataMappingResult {

    data class Success(
        val marketData: KnockoutProductMarketData
    ) : KnockoutProductMarketDataMappingResult

    data class Failure(
        val errors: List<KnockoutProductMarketDataMappingError>
    ) : KnockoutProductMarketDataMappingResult
}

/**
 * Übersetzt ausschließlich die technische DTO-Struktur in das Domainmodell.
 *
 * Der Mapper normalisiert, korrigiert und validiert keine fachlich
 * darstellbaren Werte. Nur fehlende Pflichtwerte verhindern die Konstruktion
 * des Domainmodells.
 */
object KnockoutProductMarketDataMapper {

    fun map(dto: KnockoutProductMarketDataDto): KnockoutProductMarketDataMappingResult {
        val productIsin = dto.productIsin
        val currency = dto.currency
        val sourceId = dto.sourceId
        val errors = buildList {
            appendMissing(productIsin, KnockoutProductMarketDataDtoField.PRODUCT_ISIN)
            appendMissing(currency, KnockoutProductMarketDataDtoField.CURRENCY)
            appendMissing(sourceId, KnockoutProductMarketDataDtoField.SOURCE_ID)
        }
        if (errors.isNotEmpty()) {
            return KnockoutProductMarketDataMappingResult.Failure(errors)
        }

        if (productIsin == null || currency == null || sourceId == null) {
            return KnockoutProductMarketDataMappingResult.Failure(errors)
        }

        return KnockoutProductMarketDataMappingResult.Success(
            KnockoutProductMarketData(
                productIsin = productIsin,
                bid = dto.bid,
                ask = dto.ask,
                bidTimestampEpochMillis = dto.bidTimestampEpochMillis,
                askTimestampEpochMillis = dto.askTimestampEpochMillis,
                currency = currency,
                sourceId = sourceId
            )
        )
    }

    private fun MutableList<KnockoutProductMarketDataMappingError>.appendMissing(
        value: Any?,
        field: KnockoutProductMarketDataDtoField
    ) {
        if (value == null) {
            add(
                KnockoutProductMarketDataMappingError(
                    field = field,
                    reason =
                        KnockoutProductMarketDataMappingErrorReason.MISSING_REQUIRED_VALUE
                )
            )
        }
    }
}
