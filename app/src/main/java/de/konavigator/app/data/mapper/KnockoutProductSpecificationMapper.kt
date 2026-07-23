package de.konavigator.app.data.mapper

import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationDto
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.TradeDirection

enum class KnockoutProductSpecificationDtoField {
    PRODUCT_ISIN,
    ISSUER_ID,
    UNDERLYING_ID,
    DIRECTION,
    BASE_PRICE,
    KNOCKOUT_BARRIER,
    RATIO,
    UNDERLYING_CURRENCY,
    PRODUCT_CURRENCY
}

enum class KnockoutProductSpecificationMappingErrorReason {
    MISSING_REQUIRED_VALUE,
    UNSUPPORTED_VALUE
}

data class KnockoutProductSpecificationMappingError(
    val field: KnockoutProductSpecificationDtoField,
    val reason: KnockoutProductSpecificationMappingErrorReason
)

sealed interface KnockoutProductSpecificationMappingResult {

    data class Success(
        val specification: KnockoutProductSpecification
    ) : KnockoutProductSpecificationMappingResult

    data class Failure(
        val errors: List<KnockoutProductSpecificationMappingError>
    ) : KnockoutProductSpecificationMappingResult
}

/**
 * Übersetzt ausschließlich die technische DTO-Struktur in das Domainmodell.
 *
 * Der Mapper normalisiert, korrigiert und validiert keine fachlich
 * darstellbaren Werte. Nur fehlende Pflichtwerte und eine nicht exakt
 * unterstützte Richtung verhindern die Konstruktion des Domainmodells.
 */
object KnockoutProductSpecificationMapper {

    fun map(
        dto: KnockoutProductSpecificationDto
    ): KnockoutProductSpecificationMappingResult {
        val productIsin = dto.productIsin
        val issuerId = dto.issuerId
        val underlyingId = dto.underlyingId
        val directionValue = dto.direction
        val basePrice = dto.basePrice
        val knockoutBarrier = dto.knockoutBarrier
        val ratio = dto.ratio
        val underlyingCurrency = dto.underlyingCurrency
        val productCurrency = dto.productCurrency
        val errors = buildList {
            appendMissing(productIsin, KnockoutProductSpecificationDtoField.PRODUCT_ISIN)
            appendMissing(issuerId, KnockoutProductSpecificationDtoField.ISSUER_ID)
            appendMissing(underlyingId, KnockoutProductSpecificationDtoField.UNDERLYING_ID)
            appendDirectionError(directionValue)
            appendMissing(basePrice, KnockoutProductSpecificationDtoField.BASE_PRICE)
            appendMissing(
                knockoutBarrier,
                KnockoutProductSpecificationDtoField.KNOCKOUT_BARRIER
            )
            appendMissing(ratio, KnockoutProductSpecificationDtoField.RATIO)
            appendMissing(
                underlyingCurrency,
                KnockoutProductSpecificationDtoField.UNDERLYING_CURRENCY
            )
            appendMissing(
                productCurrency,
                KnockoutProductSpecificationDtoField.PRODUCT_CURRENCY
            )
        }
        if (errors.isNotEmpty()) {
            return KnockoutProductSpecificationMappingResult.Failure(errors)
        }

        if (
            productIsin == null ||
            issuerId == null ||
            underlyingId == null ||
            directionValue == null ||
            basePrice == null ||
            knockoutBarrier == null ||
            ratio == null ||
            underlyingCurrency == null ||
            productCurrency == null
        ) {
            return KnockoutProductSpecificationMappingResult.Failure(errors)
        }

        val direction = when (directionValue) {
            "LONG" -> TradeDirection.LONG
            "SHORT" -> TradeDirection.SHORT
            else -> {
                return KnockoutProductSpecificationMappingResult.Failure(
                    listOf(
                        mappingError(
                            field = KnockoutProductSpecificationDtoField.DIRECTION,
                            reason =
                                KnockoutProductSpecificationMappingErrorReason.UNSUPPORTED_VALUE
                        )
                    )
                )
            }
        }

        return KnockoutProductSpecificationMappingResult.Success(
            KnockoutProductSpecification(
                productIsin = productIsin,
                productWkn = dto.productWkn,
                issuerId = issuerId,
                underlyingId = underlyingId,
                direction = direction,
                basePrice = basePrice,
                knockoutBarrier = knockoutBarrier,
                ratio = ratio,
                underlyingCurrency = underlyingCurrency,
                productCurrency = productCurrency
            )
        )
    }

    private fun MutableList<KnockoutProductSpecificationMappingError>.appendDirectionError(
        value: String?
    ) {
        when (value) {
            null -> add(
                mappingError(
                    field = KnockoutProductSpecificationDtoField.DIRECTION,
                    reason = KnockoutProductSpecificationMappingErrorReason.MISSING_REQUIRED_VALUE
                )
            )

            "LONG",
            "SHORT" -> Unit

            else -> add(
                mappingError(
                    field = KnockoutProductSpecificationDtoField.DIRECTION,
                    reason = KnockoutProductSpecificationMappingErrorReason.UNSUPPORTED_VALUE
                )
            )
        }
    }

    private fun MutableList<KnockoutProductSpecificationMappingError>.appendMissing(
        value: Any?,
        field: KnockoutProductSpecificationDtoField
    ) {
        if (value == null) {
            add(
                mappingError(
                    field = field,
                    reason = KnockoutProductSpecificationMappingErrorReason.MISSING_REQUIRED_VALUE
                )
            )
        }
    }

    private fun mappingError(
        field: KnockoutProductSpecificationDtoField,
        reason: KnockoutProductSpecificationMappingErrorReason
    ) = KnockoutProductSpecificationMappingError(
        field = field,
        reason = reason
    )
}
