package de.konavigator.app.application.productdiscovery

import de.konavigator.app.application.repository.FxRateProvider
import de.konavigator.app.application.repository.FxRateProviderResult
import de.konavigator.app.domain.availability.MarketDataCalculationAvailabilityResult
import de.konavigator.app.domain.currency.CurrencyCode
import de.konavigator.app.domain.currency.CurrencyCodeCreationError
import de.konavigator.app.domain.currency.CurrencyCodeCreationResult
import de.konavigator.app.domain.currency.CurrencyConversion
import de.konavigator.app.domain.currency.CurrencyConversionPolicy
import de.konavigator.app.domain.currency.CurrencyConversionPolicyError
import de.konavigator.app.domain.currency.FxRateQuote
import de.konavigator.app.domain.dataquality.DataQualityAssessment
import de.konavigator.app.domain.freshness.MarketDataFreshnessResult
import de.konavigator.app.domain.model.KnockoutProductMarketData
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot
import de.konavigator.app.domain.model.TradeDirection
import de.konavigator.app.domain.orchestration.MarketDataCalculationValue
import de.konavigator.app.domain.source.MarketDataSourceResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductCandidateCurrencyConversionApplicationServiceTest {
    private val usd = currencyCode("USD")
    private val eur = currencyCode("EUR")

    @Test
    fun emptyInputReturnsNoInputWithoutProviderCall() = runTest {
        val provider = FakeFxRateProvider()

        val result = service(provider).execute(request(emptyList()))

        assertSame(
            KnockoutProductCandidateCurrencyConversionApplicationResult.NoInputCandidates,
            result
        )
        assertTrue(provider.calls.isEmpty())
    }

    @Test
    fun sameCurrencyCreatesDirectTargetLeverageInputWithoutFxFallback() = runTest {
        val provider = FakeFxRateProvider()
        val candidate = candidate("SAME", "eur", " EUR ")

        val result = success(service(provider).execute(request(listOf(candidate))))
        val converted = result.successfulCandidates.single()

        assertSame(candidate, converted.targetLeverageInput.candidateWithCalculation)
        assertTrue(
            converted.targetLeverageInput.currencyConversion is CurrencyConversion.SameCurrency
        )
        assertFalse(
            converted.targetLeverageInput.currencyConversion is CurrencyConversion.CrossCurrency
        )
        assertSame(
            KnockoutProductCandidateCurrencyConversionEvidence.SameCurrency,
            converted.evidence
        )
        assertTrue(provider.calls.isEmpty())
    }

    @Test
    fun approvedCrossCurrencyUsesPolicyConversionAndPreservesEvidence() = runTest {
        val quote = quote(sourceId = "EXACT_SOURCE", observedAtEpochMillis = 900L, rate = 1.2)
        val provider = FakeFxRateProvider(success(quote))
        val candidate = candidate("CROSS", "USD", "EUR")

        val result = success(
            service(provider).execute(
                request(listOf(candidate), evaluationTimeEpochMillis = 1_000L, maxFxAgeMillis = 100L)
            )
        )
        val converted = result.successfulCandidates.single()
        val conversion = converted.targetLeverageInput.currencyConversion

        assertSame(candidate, converted.targetLeverageInput.candidateWithCalculation)
        assertTrue(conversion is CurrencyConversion.CrossCurrency)
        conversion as CurrencyConversion.CrossCurrency
        assertEquals(1.2, conversion.underlyingCurrencyPerProductCurrencyRate, 0.0)
        assertEquals(usd, conversion.underlyingCurrency)
        assertEquals(eur, conversion.productCurrency)
        assertEquals(
            KnockoutProductCandidateCurrencyConversionEvidence.CrossCurrency(
                sourceId = "EXACT_SOURCE",
                observedAtEpochMillis = 900L
            ),
            converted.evidence
        )
    }

    @Test
    fun providerFailuresRemainTypedWithExactCandidateAndPair() = runTest {
        listOf(
            FxRateProviderResult.NotFound,
            FxRateProviderResult.DataAccessFailure,
            FxRateProviderResult.InvalidData
        ).forEach { providerResult ->
            val candidate = candidate("FAIL-$providerResult", "USD", "EUR")
            val result = noConvertible(
                service(FakeFxRateProvider(providerResult)).execute(request(listOf(candidate)))
            )
            val failure = result.failedCandidates.single()

            assertSame(candidate, failure.candidateWithCalculation)
            when (providerResult) {
                FxRateProviderResult.NotFound ->
                    assertTrue(
                        failure is KnockoutProductCandidateCurrencyConversionFailure
                            .FxProviderNotFound
                    )

                FxRateProviderResult.DataAccessFailure ->
                    assertTrue(
                        failure is KnockoutProductCandidateCurrencyConversionFailure
                            .FxProviderDataAccessFailure
                    )

                FxRateProviderResult.InvalidData ->
                    assertTrue(
                        failure is KnockoutProductCandidateCurrencyConversionFailure
                            .FxProviderInvalidData
                    )

                is FxRateProviderResult.Success -> error("Unexpected test provider success")
            }
            assertEquals(usd, requestedUnderlyingCurrency(failure))
            assertEquals(eur, requestedProductCurrency(failure))
        }
    }

    @Test
    fun reversedProviderPairIsBlockedWithoutRateInversion() = runTest {
        val reversedQuote = quote(
            underlyingCurrency = eur,
            productCurrency = usd,
            rate = 0.8
        )
        val candidate = candidate("REVERSED", "USD", "EUR")

        val failure = policyFailure(
            noConvertible(
                service(FakeFxRateProvider(success(reversedQuote)))
                    .execute(request(listOf(candidate)))
            ).failedCandidates.single()
        )

        assertSame(candidate, failure.candidateWithCalculation)
        assertSame(reversedQuote, failure.fxQuote)
        assertEquals(CurrencyConversionPolicyError.CURRENCY_PAIR_MISMATCH, failure.error)
        assertEquals(0.8, reversedQuote.underlyingCurrencyPerProductCurrencyRate, 0.0)
    }

    @Test
    fun exactRequestTimeAndMaximumAgeArePassedToPolicy() = runTest {
        val candidate = candidate("TIME", "USD", "EUR")
        val provider = FakeFxRateProvider(
            success(quote(observedAtEpochMillis = 900L))
        )

        val approved = service(provider).execute(
            request(
                listOf(candidate),
                evaluationTimeEpochMillis = 1_000L,
                maxFxAgeMillis = 100L
            )
        )
        val blocked = service(provider).execute(
            request(
                listOf(candidate),
                evaluationTimeEpochMillis = 1_001L,
                maxFxAgeMillis = 100L
            )
        )

        assertTrue(approved is KnockoutProductCandidateCurrencyConversionApplicationResult
            .CandidatesWithCurrencyConversion)
        assertEquals(
            CurrencyConversionPolicyError.FX_QUOTE_TOO_OLD,
            policyFailure(noConvertible(blocked).failedCandidates.single()).error
        )
    }

    @Test
    fun invalidUnderlyingCurrencyRemainsTypedWithoutProviderCall() = runTest {
        val provider = FakeFxRateProvider()
        val candidate = candidate("INVALID-U", "US", "EUR")

        val failure = noConvertible(
            service(provider).execute(request(listOf(candidate)))
        ).failedCandidates.single()

        assertTrue(
            failure is KnockoutProductCandidateCurrencyConversionFailure
                .InvalidUnderlyingCurrency
        )
        failure as KnockoutProductCandidateCurrencyConversionFailure.InvalidUnderlyingCurrency
        assertSame(candidate, failure.candidateWithCalculation)
        assertEquals(CurrencyCodeCreationError.INVALID_FORMAT, failure.error)
        assertTrue(provider.calls.isEmpty())
    }

    @Test
    fun invalidProductCurrencyRemainsTypedWithoutProviderCall() = runTest {
        val provider = FakeFxRateProvider()
        val candidate = candidate("INVALID-P", "USD", "EURO")

        val failure = noConvertible(
            service(provider).execute(request(listOf(candidate)))
        ).failedCandidates.single()

        assertTrue(
            failure is KnockoutProductCandidateCurrencyConversionFailure
                .InvalidProductCurrency
        )
        failure as KnockoutProductCandidateCurrencyConversionFailure.InvalidProductCurrency
        assertSame(candidate, failure.candidateWithCalculation)
        assertEquals(CurrencyCodeCreationError.INVALID_FORMAT, failure.error)
        assertTrue(provider.calls.isEmpty())
    }

    @Test
    fun mixedCandidatesProduceDirectBridgeInputsAndTypedFailures() = runTest {
        val same = candidate("A", "EUR", "EUR")
        val approvedCross = candidate("B", "USD", "EUR")
        val providerFailure = candidate("C", "GBP", "EUR")
        val policyFailure = candidate("D", "CHF", "EUR")
        val usdEur = pair("USD", "EUR")
        val gbpEur = pair("GBP", "EUR")
        val chfEur = pair("CHF", "EUR")
        val provider = FakeFxRateProvider(
            responses = mapOf(
                usdEur to success(quote(rate = 1.1)),
                gbpEur to FxRateProviderResult.NotFound,
                chfEur to success(
                    quote(
                        underlyingCurrency = currencyCode("CHF"),
                        observedAtEpochMillis = 899L
                    )
                )
            )
        )
        val candidates = listOf(same, approvedCross, providerFailure, policyFailure)

        val result = success(service(provider).execute(request(candidates)))

        assertEquals(
            listOf(same, approvedCross),
            result.successfulCandidates.map {
                it.targetLeverageInput.candidateWithCalculation
            }
        )
        assertEquals(
            listOf(providerFailure, policyFailure),
            result.failedCandidates.map { it.candidateWithCalculation }
        )
        assertTrue(
            result.failedCandidates[0] is KnockoutProductCandidateCurrencyConversionFailure
                .FxProviderNotFound
        )
        assertEquals(
            CurrencyConversionPolicyError.FX_QUOTE_TOO_OLD,
            policyFailure(result.failedCandidates[1]).error
        )
        assertSame(candidates, request(candidates).candidates)
    }

    @Test
    fun allFailuresReturnNoCurrencyConvertibleCandidatesInOriginalOrder() = runTest {
        val first = candidate("FIRST", "USD", "EUR")
        val second = candidate("SECOND", "GBP", "EUR")
        val provider = FakeFxRateProvider(FxRateProviderResult.NotFound)

        val result = noConvertible(
            service(provider).execute(request(listOf(first, second)))
        )

        assertEquals(
            listOf(first, second),
            result.failedCandidates.map { it.candidateWithCalculation }
        )
    }

    @Test
    fun identicalDirectedPairIsLoadedOnceWhileCandidatesAndDuplicatesRemainSeparate() = runTest {
        val duplicate = candidate("DUPLICATE", "USD", "EUR")
        val second = candidate("SECOND", "USD", "EUR")
        val provider = FakeFxRateProvider(success(quote()))
        val input = listOf(duplicate, second, duplicate)
        val request = request(input)

        val result = success(service(provider).execute(request))

        assertEquals(listOf(usd to eur), provider.calls)
        assertEquals(3, result.successfulCandidates.size)
        assertSame(
            duplicate,
            result.successfulCandidates[0].targetLeverageInput.candidateWithCalculation
        )
        assertSame(
            second,
            result.successfulCandidates[1].targetLeverageInput.candidateWithCalculation
        )
        assertSame(
            duplicate,
            result.successfulCandidates[2].targetLeverageInput.candidateWithCalculation
        )
        assertSame(input, request.candidates)
    }

    @Test
    fun differentDirectedPairsAreLoadedSeparatelyInInputOrder() = runTest {
        val usdEurCandidate = candidate("USD-EUR", "USD", "EUR")
        val eurUsdCandidate = candidate("EUR-USD", "EUR", "USD")
        val provider = FakeFxRateProvider(
            responses = mapOf(
                pair("USD", "EUR") to success(quote(rate = 1.1)),
                pair("EUR", "USD") to success(
                    quote(
                        underlyingCurrency = eur,
                        productCurrency = usd,
                        rate = 0.9
                    )
                )
            )
        )

        val result = success(
            service(provider).execute(request(listOf(usdEurCandidate, eurUsdCandidate)))
        )

        assertEquals(listOf(usd to eur, eur to usd), provider.calls)
        assertEquals(
            listOf(1.1, 0.9),
            result.successfulCandidates.map {
                (it.targetLeverageInput.currencyConversion as CurrencyConversion.CrossCurrency)
                    .underlyingCurrencyPerProductCurrencyRate
            }
        )
    }

    private fun service(
        provider: FakeFxRateProvider
    ) = KnockoutProductCandidateCurrencyConversionApplicationService(
        fxRateProvider = provider,
        currencyConversionPolicy = CurrencyConversionPolicy()
    )

    private fun request(
        candidates: List<KnockoutProductCandidateWithCalculation>,
        evaluationTimeEpochMillis: Long = 1_000L,
        maxFxAgeMillis: Long = 100L
    ) = KnockoutProductCandidateCurrencyConversionApplicationRequest(
        candidates = candidates,
        evaluationTimeEpochMillis = evaluationTimeEpochMillis,
        maxFxAgeMillis = maxFxAgeMillis
    )

    private fun candidate(
        productIsin: String,
        underlyingCurrency: String,
        productCurrency: String
    ): KnockoutProductCandidateWithCalculation {
        val specification = KnockoutProductSpecification(
            productIsin = productIsin,
            productWkn = "SYN001",
            issuerId = "issuer",
            underlyingId = "underlying",
            direction = TradeDirection.LONG,
            basePrice = 100.0,
            knockoutBarrier = 95.0,
            ratio = 0.1,
            underlyingCurrency = underlyingCurrency,
            productCurrency = productCurrency
        )
        val snapshot = KnockoutProductSpecificationSnapshot(
            specification = specification,
            sourceId = "SYNTH_SPEC",
            retrievedAtEpochMillis = 1_000L,
            sourceTimestampEpochMillis = 900L
        )
        val marketData = KnockoutProductMarketData(
            productIsin = productIsin,
            bid = 1.0,
            ask = 1.2,
            bidTimestampEpochMillis = 950L,
            askTimestampEpochMillis = 950L,
            currency = productCurrency,
            sourceId = "SYNTH_MARKET"
        )
        val marketCandidate = KnockoutProductCandidateWithMarketData(snapshot, marketData)
        val qualityCandidate = KnockoutProductCandidateWithDataQuality(
            marketCandidate,
            DataQualityAssessment.passed()
        )
        val availabilityCandidate = KnockoutProductCandidateWithCalculationAvailability(
            qualityCandidate,
            MarketDataCalculationAvailabilityResult.StructurallyAvailable
        )
        val freshnessCandidate = KnockoutProductCandidateWithFreshness(
            availabilityCandidate,
            MarketDataFreshnessResult.Fresh
        )
        val sourceCandidate = KnockoutProductCandidateWithSourceEvaluation(
            freshnessCandidate,
            MarketDataSourceResult.Allowed
        )
        return KnockoutProductCandidateWithCalculation(
            sourceCandidate,
            KnockoutProductCandidateCalculationOutcome.Success(
                MarketDataCalculationValue.MidPrice(1.1, productCurrency)
            )
        )
    }

    private fun quote(
        underlyingCurrency: CurrencyCode = usd,
        productCurrency: CurrencyCode = eur,
        rate: Double = 1.1,
        sourceId: String = "SYNTH_FX",
        observedAtEpochMillis: Long = 900L
    ) = FxRateQuote(
        underlyingCurrency = underlyingCurrency,
        productCurrency = productCurrency,
        underlyingCurrencyPerProductCurrencyRate = rate,
        sourceId = sourceId,
        observedAtEpochMillis = observedAtEpochMillis
    )

    private fun success(
        quote: FxRateQuote
    ) = FxRateProviderResult.Success(quote)

    private fun pair(
        underlyingCurrency: String,
        productCurrency: String
    ) = currencyCode(underlyingCurrency) to currencyCode(productCurrency)

    private fun success(
        result: KnockoutProductCandidateCurrencyConversionApplicationResult
    ): KnockoutProductCandidateCurrencyConversionApplicationResult
        .CandidatesWithCurrencyConversion {
        assertTrue(
            result is KnockoutProductCandidateCurrencyConversionApplicationResult
                .CandidatesWithCurrencyConversion
        )
        return result as KnockoutProductCandidateCurrencyConversionApplicationResult
            .CandidatesWithCurrencyConversion
    }

    private fun noConvertible(
        result: KnockoutProductCandidateCurrencyConversionApplicationResult
    ): KnockoutProductCandidateCurrencyConversionApplicationResult
        .NoCurrencyConvertibleCandidates {
        assertTrue(
            result is KnockoutProductCandidateCurrencyConversionApplicationResult
                .NoCurrencyConvertibleCandidates
        )
        return result as KnockoutProductCandidateCurrencyConversionApplicationResult
            .NoCurrencyConvertibleCandidates
    }

    private fun policyFailure(
        failure: KnockoutProductCandidateCurrencyConversionFailure
    ): KnockoutProductCandidateCurrencyConversionFailure.CurrencyPolicyBlocked {
        assertTrue(
            failure is KnockoutProductCandidateCurrencyConversionFailure.CurrencyPolicyBlocked
        )
        return failure as KnockoutProductCandidateCurrencyConversionFailure
            .CurrencyPolicyBlocked
    }

    private fun requestedUnderlyingCurrency(
        failure: KnockoutProductCandidateCurrencyConversionFailure
    ): CurrencyCode = when (failure) {
        is KnockoutProductCandidateCurrencyConversionFailure.FxProviderNotFound ->
            failure.requestedUnderlyingCurrency

        is KnockoutProductCandidateCurrencyConversionFailure.FxProviderDataAccessFailure ->
            failure.requestedUnderlyingCurrency

        is KnockoutProductCandidateCurrencyConversionFailure.FxProviderInvalidData ->
            failure.requestedUnderlyingCurrency

        else -> error("Unexpected failure: $failure")
    }

    private fun requestedProductCurrency(
        failure: KnockoutProductCandidateCurrencyConversionFailure
    ): CurrencyCode = when (failure) {
        is KnockoutProductCandidateCurrencyConversionFailure.FxProviderNotFound ->
            failure.requestedProductCurrency

        is KnockoutProductCandidateCurrencyConversionFailure.FxProviderDataAccessFailure ->
            failure.requestedProductCurrency

        is KnockoutProductCandidateCurrencyConversionFailure.FxProviderInvalidData ->
            failure.requestedProductCurrency

        else -> error("Unexpected failure: $failure")
    }

    private fun currencyCode(value: String): CurrencyCode =
        when (val result = CurrencyCode.create(value)) {
            is CurrencyCodeCreationResult.Success -> result.currencyCode
            is CurrencyCodeCreationResult.Failure ->
                error("Unexpected invalid test currency: ${result.error}")
        }

    private class FakeFxRateProvider(
        private val defaultResult: FxRateProviderResult = FxRateProviderResult.NotFound,
        private val responses: Map<Pair<CurrencyCode, CurrencyCode>, FxRateProviderResult> =
            emptyMap()
    ) : FxRateProvider {
        val calls = mutableListOf<Pair<CurrencyCode, CurrencyCode>>()

        override suspend fun findRate(
            underlyingCurrency: CurrencyCode,
            productCurrency: CurrencyCode
        ): FxRateProviderResult {
            val pair = underlyingCurrency to productCurrency
            calls += pair
            return responses[pair] ?: defaultResult
        }
    }
}
