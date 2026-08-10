package de.konavigator.app.application.productdiscovery

import de.konavigator.app.calculator.ExistingKnockoutProductEntryCalculationResult
import de.konavigator.app.calculator.ExistingKnockoutProductTargetDeviationResult
import de.konavigator.app.calculator.ExistingKnockoutProductTargetFitError
import de.konavigator.app.calculator.ExistingKnockoutProductTargetFitResult
import de.konavigator.app.calculator.TradeCalculationResult
import de.konavigator.app.domain.availability.MarketDataCalculationAvailabilityResult
import de.konavigator.app.domain.currency.CurrencyCode
import de.konavigator.app.domain.currency.CurrencyCodeCreationResult
import de.konavigator.app.domain.currency.CurrencyConversion
import de.konavigator.app.domain.dataquality.DataQualityAssessment
import de.konavigator.app.domain.freshness.MarketDataFreshnessResult
import de.konavigator.app.domain.model.KnockoutProductMarketData
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot
import de.konavigator.app.domain.model.TradeDirection
import de.konavigator.app.domain.orchestration.MarketDataCalculationValue
import de.konavigator.app.domain.source.MarketDataSourceResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductCandidateTargetFitSelectorTest {
    private val selector = KnockoutProductCandidateTargetFitSelector()

    @Test
    fun emptyCandidatesReturnNoInputCandidates() {
        assertSame(
            KnockoutProductCandidateTargetFitSelectionResult.NoInputCandidates,
            selector.select(request(emptyList()))
        )
    }

    @Test
    fun oneCandidateBecomesPrimaryWithoutAlternatives() {
        val candidate = candidate("A")
        val result = selected(listOf(candidate))
        assertSame(candidate, result.primaryCandidate)
        assertTrue(result.alternativeCandidates.isEmpty())
    }

    @Test
    fun twoCandidatesUseFirstAsPrimaryAndSecondAsAlternative() {
        val first = candidate("A")
        val second = candidate("B")
        val result = selected(listOf(first, second))
        assertSame(first, result.primaryCandidate)
        assertEquals(listOf(second), result.alternativeCandidates)
        assertSame(second, result.alternativeCandidates.single())
    }

    @Test
    fun threeCandidatesUseFirstThreePositions() {
        val first = candidate("A")
        val second = candidate("B")
        val third = candidate("C")
        val result = selected(listOf(first, second, third))
        assertSame(first, result.primaryCandidate)
        assertEquals(listOf(second, third), result.alternativeCandidates)
        assertSame(second, result.alternativeCandidates[0])
        assertSame(third, result.alternativeCandidates[1])
    }

    @Test
    fun fourOrMoreCandidatesOnlyExposeFirstThreePositions() {
        val candidates = listOf(candidate("A"), candidate("B"), candidate("C"), candidate("D"), candidate("E"))
        val result = selected(candidates)
        assertSame(candidates[0], result.primaryCandidate)
        assertEquals(listOf(candidates[1], candidates[2]), result.alternativeCandidates)
        assertTrue(candidates[3] !in result.alternativeCandidates)
        assertTrue(candidates[4] !in result.alternativeCandidates)
    }

    @Test
    fun duplicateCandidatesAreNotDeduplicated() {
        val duplicate = candidate("A")
        val third = candidate("B")
        val result = selected(listOf(duplicate, duplicate, third))
        assertSame(duplicate, result.primaryCandidate)
        assertEquals(listOf(duplicate, third), result.alternativeCandidates)
        assertSame(duplicate, result.alternativeCandidates[0])
    }

    @Test
    fun inputListIsNotMutated() {
        val first = candidate("A")
        val second = candidate("B")
        val third = candidate("C")
        val fourth = candidate("D")
        val input = mutableListOf(first, second, third, fourth)
        val original = input.toList()
        val request = request(input)
        val result = selected(request)

        assertSame(input, request.rankedCandidates)
        assertEquals(original, input)
        assertSame(first, result.primaryCandidate)
        assertEquals(listOf(second, third), result.alternativeCandidates)
    }

    @Test
    fun selectorUsesPositionsEvenWhenValuesSuggestAnotherOrder() {
        val first = candidate(
            isin = "A",
            relativeLeverageDeviationPercent = 100.0,
            barrierDeviationPercent = 100.0,
            targetFitResult = ExistingKnockoutProductTargetFitResult.Failure(
                ExistingKnockoutProductTargetFitError.INVALID_RELATIVE_LEVERAGE_DEVIATION_PERCENT
            )
        )
        val second = candidate(
            isin = "B",
            relativeLeverageDeviationPercent = 0.0,
            barrierDeviationPercent = 0.0,
            targetFitResult = ExistingKnockoutProductTargetFitResult.Success(true, true, true)
        )
        val result = selected(listOf(first, second))
        assertSame(first, result.primaryCandidate)
        assertSame(second, result.alternativeCandidates.single())
    }

    private fun selected(
        candidates: List<KnockoutProductCandidateWithTargetFit>
    ) = selected(request(candidates))

    private fun selected(
        request: KnockoutProductCandidateTargetFitSelectionRequest
    ): KnockoutProductCandidateTargetFitSelectionResult.SelectedCandidates {
        val result = selector.select(request)
        assertTrue(result is KnockoutProductCandidateTargetFitSelectionResult.SelectedCandidates)
        return result as KnockoutProductCandidateTargetFitSelectionResult.SelectedCandidates
    }

    private fun request(candidates: List<KnockoutProductCandidateWithTargetFit>) =
        KnockoutProductCandidateTargetFitSelectionRequest(candidates)

    private fun candidate(
        isin: String,
        relativeLeverageDeviationPercent: Double = 1.0,
        barrierDeviationPercent: Double = 1.0,
        targetFitResult: ExistingKnockoutProductTargetFitResult =
            ExistingKnockoutProductTargetFitResult.Success(true, true, true)
    ): KnockoutProductCandidateWithTargetFit {
        val specification = KnockoutProductSpecification(
            isin, "SYN001", "issuer", "underlying", TradeDirection.LONG,
            100.0, 90.0, 0.1, "EUR", "EUR"
        )
        val snapshot = KnockoutProductSpecificationSnapshot(specification, "snapshot", 1000L, 900L)
        val marketData = KnockoutProductMarketData(isin, 1.0, 1.2, 950L, 950L, "EUR", "SYNTH_SOURCE")
        val marketCandidate = KnockoutProductCandidateWithMarketData(snapshot, marketData)
        val qualityCandidate = KnockoutProductCandidateWithDataQuality(marketCandidate, DataQualityAssessment.passed())
        val availabilityCandidate = KnockoutProductCandidateWithCalculationAvailability(qualityCandidate, MarketDataCalculationAvailabilityResult.StructurallyAvailable)
        val freshnessCandidate = KnockoutProductCandidateWithFreshness(availabilityCandidate, MarketDataFreshnessResult.Fresh)
        val sourceCandidate = KnockoutProductCandidateWithSourceEvaluation(freshnessCandidate, MarketDataSourceResult.Allowed)
        val calculationCandidate = KnockoutProductCandidateWithCalculation(
            sourceCandidate,
            KnockoutProductCandidateCalculationOutcome.Success(MarketDataCalculationValue.MidPrice(1.0, "EUR"))
        )
        val targetInput = KnockoutProductCandidateTargetLeverageInput(
            calculationCandidate,
            CurrencyConversion.SameCurrency(currencyCode("EUR"))
        )
        val targetPlan = KnockoutProductCandidateWithTargetLeveragePlan(
            targetInput,
            TradeCalculationResult(
                true, 120.0, 5.0, 90.0, 1.0, 1.0, 12.0,
                5.0, currencyCode("EUR"), currencyCode("EUR"), 30.0, 25.0
            )
        )
        val existingEntry = KnockoutProductCandidateWithExistingEntryCalculation(
            targetPlan,
            ExistingKnockoutProductEntryCalculationResult.Success(
                1.0, 1.0, 20.0, 20.0, 12.0, 5.0,
                currencyCode("EUR"), currencyCode("EUR")
            )
        )
        val targetDeviation = KnockoutProductCandidateWithTargetDeviation(
            existingEntry,
            ExistingKnockoutProductTargetDeviationResult.Success(
                0.0, 0.0, relativeLeverageDeviationPercent,
                0.0, 0.0, barrierDeviationPercent
            )
        )
        return KnockoutProductCandidateWithTargetFit(targetDeviation, targetFitResult)
    }

    private fun currencyCode(value: String): CurrencyCode =
        (CurrencyCode.create(value) as CurrencyCodeCreationResult.Success).currencyCode
}
