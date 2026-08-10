package de.konavigator.app.application.productdiscovery

import de.konavigator.app.application.repository.FxRateProvider
import de.konavigator.app.application.repository.FxRateProviderResult
import de.konavigator.app.domain.currency.CurrencyCode
import de.konavigator.app.domain.currency.CurrencyCodeCreationResult
import de.konavigator.app.domain.currency.CurrencyConversion
import de.konavigator.app.domain.currency.CurrencyConversionPolicy
import de.konavigator.app.domain.currency.CurrencyConversionPolicyInput
import de.konavigator.app.domain.currency.CurrencyConversionPolicyResult
import de.konavigator.app.domain.currency.FxRateQuote

/**
 * Verbindet berechnete Kandidaten mit einer typisierten Währungsbeziehung,
 * ohne Calculation, Zielhebel oder Produktauswahl erneut auszuführen.
 */
class KnockoutProductCandidateCurrencyConversionApplicationService(
    private val fxRateProvider: FxRateProvider,
    private val currencyConversionPolicy: CurrencyConversionPolicy
) {

    suspend fun execute(
        request: KnockoutProductCandidateCurrencyConversionApplicationRequest
    ): KnockoutProductCandidateCurrencyConversionApplicationResult {
        if (request.candidates.isEmpty()) {
            return KnockoutProductCandidateCurrencyConversionApplicationResult
                .NoInputCandidates
        }

        val successfulCandidates =
            mutableListOf<KnockoutProductCandidateWithCurrencyConversion>()
        val failedCandidates =
            mutableListOf<KnockoutProductCandidateCurrencyConversionFailure>()
        val crossCurrencyResolutions = mutableMapOf<CurrencyPair, CurrencyPairResolution>()

        request.candidates.forEach { candidate ->
            val specification = candidate.candidateWithSourceEvaluation
                .candidateWithFreshness
                .candidateWithCalculationAvailability
                .candidateWithDataQuality
                .candidateWithMarketData
                .specificationSnapshot
                .specification

            val underlyingCurrency = when (
                val result = CurrencyCode.create(specification.underlyingCurrency)
            ) {
                is CurrencyCodeCreationResult.Success -> result.currencyCode
                is CurrencyCodeCreationResult.Failure -> {
                    failedCandidates += KnockoutProductCandidateCurrencyConversionFailure
                        .InvalidUnderlyingCurrency(candidate, result.error)
                    return@forEach
                }
            }

            val productCurrency = when (
                val result = CurrencyCode.create(specification.productCurrency)
            ) {
                is CurrencyCodeCreationResult.Success -> result.currencyCode
                is CurrencyCodeCreationResult.Failure -> {
                    failedCandidates += KnockoutProductCandidateCurrencyConversionFailure
                        .InvalidProductCurrency(candidate, result.error)
                    return@forEach
                }
            }

            if (underlyingCurrency == productCurrency) {
                successfulCandidates += convertedCandidate(
                    candidate = candidate,
                    conversion = CurrencyConversion.SameCurrency(underlyingCurrency),
                    evidence = KnockoutProductCandidateCurrencyConversionEvidence.SameCurrency
                )
                return@forEach
            }

            val pair = CurrencyPair(underlyingCurrency, productCurrency)
            val resolution = crossCurrencyResolutions[pair] ?: resolveCrossCurrency(
                pair = pair,
                request = request
            ).also { resolved -> crossCurrencyResolutions[pair] = resolved }

            when (resolution) {
                is CurrencyPairResolution.Approved ->
                    successfulCandidates += convertedCandidate(
                        candidate = candidate,
                        conversion = resolution.conversion,
                        evidence = KnockoutProductCandidateCurrencyConversionEvidence
                            .CrossCurrency(
                                sourceId = resolution.sourceId,
                                observedAtEpochMillis = resolution.observedAtEpochMillis
                            )
                    )

                CurrencyPairResolution.ProviderNotFound ->
                    failedCandidates += KnockoutProductCandidateCurrencyConversionFailure
                        .FxProviderNotFound(candidate, underlyingCurrency, productCurrency)

                CurrencyPairResolution.ProviderDataAccessFailure ->
                    failedCandidates += KnockoutProductCandidateCurrencyConversionFailure
                        .FxProviderDataAccessFailure(
                            candidate,
                            underlyingCurrency,
                            productCurrency
                        )

                CurrencyPairResolution.ProviderInvalidData ->
                    failedCandidates += KnockoutProductCandidateCurrencyConversionFailure
                        .FxProviderInvalidData(candidate, underlyingCurrency, productCurrency)

                is CurrencyPairResolution.PolicyBlocked ->
                    failedCandidates += KnockoutProductCandidateCurrencyConversionFailure
                        .CurrencyPolicyBlocked(candidate, resolution.quote, resolution.error)
            }
        }

        return if (successfulCandidates.isEmpty()) {
            KnockoutProductCandidateCurrencyConversionApplicationResult
                .NoCurrencyConvertibleCandidates(failedCandidates)
        } else {
            KnockoutProductCandidateCurrencyConversionApplicationResult
                .CandidatesWithCurrencyConversion(successfulCandidates, failedCandidates)
        }
    }

    private suspend fun resolveCrossCurrency(
        pair: CurrencyPair,
        request: KnockoutProductCandidateCurrencyConversionApplicationRequest
    ): CurrencyPairResolution = when (
        val providerResult = fxRateProvider.findRate(
            underlyingCurrency = pair.underlyingCurrency,
            productCurrency = pair.productCurrency
        )
    ) {
        is FxRateProviderResult.Success -> when (
            val policyResult = currencyConversionPolicy.evaluate(
                CurrencyConversionPolicyInput(
                    requestedUnderlyingCurrency = pair.underlyingCurrency,
                    requestedProductCurrency = pair.productCurrency,
                    fxQuote = providerResult.quote,
                    evaluationTimeEpochMillis = request.evaluationTimeEpochMillis,
                    maxFxAgeMillis = request.maxFxAgeMillis
                )
            )
        ) {
            is CurrencyConversionPolicyResult.Approved -> CurrencyPairResolution.Approved(
                conversion = policyResult.conversion,
                sourceId = policyResult.sourceId,
                observedAtEpochMillis = policyResult.observedAtEpochMillis
            )

            is CurrencyConversionPolicyResult.Blocked -> CurrencyPairResolution.PolicyBlocked(
                quote = providerResult.quote,
                error = policyResult.error
            )
        }

        FxRateProviderResult.NotFound -> CurrencyPairResolution.ProviderNotFound
        FxRateProviderResult.DataAccessFailure ->
            CurrencyPairResolution.ProviderDataAccessFailure

        FxRateProviderResult.InvalidData -> CurrencyPairResolution.ProviderInvalidData
    }

    private fun convertedCandidate(
        candidate: KnockoutProductCandidateWithCalculation,
        conversion: CurrencyConversion,
        evidence: KnockoutProductCandidateCurrencyConversionEvidence
    ) = KnockoutProductCandidateWithCurrencyConversion(
        targetLeverageInput = KnockoutProductCandidateTargetLeverageInput(
            candidateWithCalculation = candidate,
            currencyConversion = conversion
        ),
        evidence = evidence
    )

    private data class CurrencyPair(
        val underlyingCurrency: CurrencyCode,
        val productCurrency: CurrencyCode
    )

    private sealed interface CurrencyPairResolution {
        data class Approved(
            val conversion: CurrencyConversion.CrossCurrency,
            val sourceId: String,
            val observedAtEpochMillis: Long
        ) : CurrencyPairResolution

        data object ProviderNotFound : CurrencyPairResolution

        data object ProviderDataAccessFailure : CurrencyPairResolution

        data object ProviderInvalidData : CurrencyPairResolution

        data class PolicyBlocked(
            val quote: FxRateQuote,
            val error: de.konavigator.app.domain.currency.CurrencyConversionPolicyError
        ) : CurrencyPairResolution
    }
}
