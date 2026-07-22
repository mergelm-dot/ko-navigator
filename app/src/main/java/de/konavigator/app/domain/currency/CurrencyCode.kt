package de.konavigator.app.domain.currency

/**
 * Syntaktisch validierter, ISO-4217-artiger Währungscode.
 *
 * Die Validierung bestätigt ausschließlich drei ASCII-Buchstaben. Sie prüft
 * nicht, ob der normalisierte Code tatsächlich in einem offiziellen
 * Währungsverzeichnis existiert.
 */
@JvmInline
value class CurrencyCode private constructor(
    val value: String
) {
    companion object {
        private val validInputPattern = Regex("[A-Za-z]{3}")

        fun create(value: String): CurrencyCodeCreationResult {
            val trimmedValue = value.trim()
            return if (validInputPattern.matches(trimmedValue)) {
                CurrencyCodeCreationResult.Success(
                    CurrencyCode(trimmedValue.uppercase())
                )
            } else {
                CurrencyCodeCreationResult.Failure(
                    CurrencyCodeCreationError.INVALID_FORMAT
                )
            }
        }
    }
}

sealed interface CurrencyCodeCreationResult {
    data class Success(
        val currencyCode: CurrencyCode
    ) : CurrencyCodeCreationResult

    data class Failure(
        val error: CurrencyCodeCreationError
    ) : CurrencyCodeCreationResult
}

enum class CurrencyCodeCreationError {
    INVALID_FORMAT
}
