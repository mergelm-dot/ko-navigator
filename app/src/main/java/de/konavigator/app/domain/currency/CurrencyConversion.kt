package de.konavigator.app.domain.currency

/**
 * Geschlossener Währungsumrechnungsvertrag für theoretische Produktwerte.
 *
 * Cross-Currency-Kurse sind immer als Einheiten der Basiswertwährung je einer
 * Einheit Produktwährung definiert. Der Vertrag enthält weder Kursquelle noch
 * Zeitstempel, Freshness-Prüfung oder Providerzugriff.
 */
sealed interface CurrencyConversion {
    val underlyingCurrency: CurrencyCode
    val productCurrency: CurrencyCode

    data class SameCurrency(
        override val underlyingCurrency: CurrencyCode
    ) : CurrencyConversion {
        override val productCurrency: CurrencyCode
            get() = underlyingCurrency
    }

    class CrossCurrency private constructor(
        override val underlyingCurrency: CurrencyCode,
        override val productCurrency: CurrencyCode,
        val underlyingCurrencyPerProductCurrencyRate: Double
    ) : CurrencyConversion {

        override fun equals(other: Any?): Boolean =
            this === other ||
                other is CrossCurrency &&
                underlyingCurrency == other.underlyingCurrency &&
                productCurrency == other.productCurrency &&
                underlyingCurrencyPerProductCurrencyRate ==
                    other.underlyingCurrencyPerProductCurrencyRate

        override fun hashCode(): Int {
            var result = underlyingCurrency.hashCode()
            result = 31 * result + productCurrency.hashCode()
            result = 31 * result + underlyingCurrencyPerProductCurrencyRate.hashCode()
            return result
        }

        override fun toString(): String =
            "CrossCurrency(" +
                "underlyingCurrency=$underlyingCurrency, " +
                "productCurrency=$productCurrency, " +
                "underlyingCurrencyPerProductCurrencyRate=" +
                "$underlyingCurrencyPerProductCurrencyRate)"

        companion object {
            fun create(
                underlyingCurrency: CurrencyCode,
                productCurrency: CurrencyCode,
                underlyingCurrencyPerProductCurrencyRate: Double
            ): CurrencyConversionCreationResult {
                if (underlyingCurrency == productCurrency) {
                    return CurrencyConversionCreationResult.Failure(
                        CurrencyConversionCreationError.IDENTICAL_CURRENCIES
                    )
                }

                if (
                    !underlyingCurrencyPerProductCurrencyRate.isFinite() ||
                    underlyingCurrencyPerProductCurrencyRate <= 0.0
                ) {
                    return CurrencyConversionCreationResult.Failure(
                        CurrencyConversionCreationError.INVALID_EXCHANGE_RATE
                    )
                }

                return CurrencyConversionCreationResult.Success(
                    CrossCurrency(
                        underlyingCurrency = underlyingCurrency,
                        productCurrency = productCurrency,
                        underlyingCurrencyPerProductCurrencyRate =
                            underlyingCurrencyPerProductCurrencyRate
                    )
                )
            }
        }
    }
}

sealed interface CurrencyConversionCreationResult {
    data class Success(
        val conversion: CurrencyConversion.CrossCurrency
    ) : CurrencyConversionCreationResult

    data class Failure(
        val error: CurrencyConversionCreationError
    ) : CurrencyConversionCreationResult
}

enum class CurrencyConversionCreationError {
    INVALID_EXCHANGE_RATE,
    IDENTICAL_CURRENCIES
}
