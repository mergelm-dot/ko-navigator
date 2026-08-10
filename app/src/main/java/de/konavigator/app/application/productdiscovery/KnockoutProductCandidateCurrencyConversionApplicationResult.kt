package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.currency.CurrencyCode
import de.konavigator.app.domain.currency.CurrencyCodeCreationError
import de.konavigator.app.domain.currency.CurrencyConversionPolicyError
import de.konavigator.app.domain.currency.FxRateQuote

sealed interface KnockoutProductCandidateCurrencyConversionEvidence {
    data object SameCurrency : KnockoutProductCandidateCurrencyConversionEvidence

    data class CrossCurrency(
        val sourceId: String,
        val observedAtEpochMillis: Long
    ) : KnockoutProductCandidateCurrencyConversionEvidence
}

data class KnockoutProductCandidateWithCurrencyConversion(
    val targetLeverageInput: KnockoutProductCandidateTargetLeverageInput,
    val evidence: KnockoutProductCandidateCurrencyConversionEvidence
)

sealed interface KnockoutProductCandidateCurrencyConversionFailure {
    val candidateWithCalculation: KnockoutProductCandidateWithCalculation

    data class InvalidUnderlyingCurrency(
        override val candidateWithCalculation: KnockoutProductCandidateWithCalculation,
        val error: CurrencyCodeCreationError
    ) : KnockoutProductCandidateCurrencyConversionFailure

    data class InvalidProductCurrency(
        override val candidateWithCalculation: KnockoutProductCandidateWithCalculation,
        val error: CurrencyCodeCreationError
    ) : KnockoutProductCandidateCurrencyConversionFailure

    data class FxProviderNotFound(
        override val candidateWithCalculation: KnockoutProductCandidateWithCalculation,
        val requestedUnderlyingCurrency: CurrencyCode,
        val requestedProductCurrency: CurrencyCode
    ) : KnockoutProductCandidateCurrencyConversionFailure

    data class FxProviderDataAccessFailure(
        override val candidateWithCalculation: KnockoutProductCandidateWithCalculation,
        val requestedUnderlyingCurrency: CurrencyCode,
        val requestedProductCurrency: CurrencyCode
    ) : KnockoutProductCandidateCurrencyConversionFailure

    data class FxProviderInvalidData(
        override val candidateWithCalculation: KnockoutProductCandidateWithCalculation,
        val requestedUnderlyingCurrency: CurrencyCode,
        val requestedProductCurrency: CurrencyCode
    ) : KnockoutProductCandidateCurrencyConversionFailure

    data class CurrencyPolicyBlocked(
        override val candidateWithCalculation: KnockoutProductCandidateWithCalculation,
        val fxQuote: FxRateQuote,
        val error: CurrencyConversionPolicyError
    ) : KnockoutProductCandidateCurrencyConversionFailure
}

sealed interface KnockoutProductCandidateCurrencyConversionApplicationResult {
    data class CandidatesWithCurrencyConversion(
        val successfulCandidates: List<KnockoutProductCandidateWithCurrencyConversion>,
        val failedCandidates: List<KnockoutProductCandidateCurrencyConversionFailure>
    ) : KnockoutProductCandidateCurrencyConversionApplicationResult

    data class NoCurrencyConvertibleCandidates(
        val failedCandidates: List<KnockoutProductCandidateCurrencyConversionFailure>
    ) : KnockoutProductCandidateCurrencyConversionApplicationResult

    data object NoInputCandidates :
        KnockoutProductCandidateCurrencyConversionApplicationResult
}
