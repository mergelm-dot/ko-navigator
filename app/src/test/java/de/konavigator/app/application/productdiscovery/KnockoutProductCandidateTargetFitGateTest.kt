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

class KnockoutProductCandidateTargetFitGateTest {
    private val gate = KnockoutProductCandidateTargetFitGate()

    @Test
    fun emptyCandidatesReturnNoInputCandidates() {
        assertSame(
            KnockoutProductCandidateTargetFitGateResult.NoInputCandidates,
            gate.filter(request(emptyList()))
        )
    }

    @Test
    fun matchingSuccessReturnsMatchingCandidates() {
        val candidate = candidate(success(withinAllTargetTolerances = true))
        val result = matchingResult(listOf(candidate))
        assertEquals(listOf(candidate), result.matchingCandidates)
        assertTrue(result.nonMatchingCandidates.isEmpty())
        assertTrue(result.failedCandidates.isEmpty())
    }

    @Test
    fun nonMatchingSuccessReturnsNoCandidatesWithinTargetTolerances() {
        val candidate = candidate(success(withinAllTargetTolerances = false))
        val result = noMatchingResult(listOf(candidate))
        assertEquals(listOf(candidate), result.nonMatchingCandidates)
        assertTrue(result.failedCandidates.isEmpty())
    }

    @Test
    fun failureReturnsNoCandidatesWithinTargetTolerances() {
        val targetFit = failure(ExistingKnockoutProductTargetFitError.INVALID_MAX_RELATIVE_LEVERAGE_DEVIATION_PERCENT)
        val candidate = candidate(targetFit)
        val result = noMatchingResult(listOf(candidate))
        assertSame(candidate, result.failedCandidates.single())
        assertSame(targetFit, result.failedCandidates.single().targetFitResult)
    }

    @Test
    fun matchingAndNonMatchingCandidatesAreSeparated() {
        val matching = candidate(success(withinAllTargetTolerances = true), "A")
        val nonMatching = candidate(success(withinAllTargetTolerances = false), "B")
        val result = matchingResult(listOf(matching, nonMatching))
        assertEquals(listOf(matching), result.matchingCandidates)
        assertEquals(listOf(nonMatching), result.nonMatchingCandidates)
    }

    @Test
    fun matchingAndFailedCandidatesAreSeparated() {
        val matching = candidate(success(withinAllTargetTolerances = true), "A")
        val failed = candidate(failure(ExistingKnockoutProductTargetFitError.INVALID_BARRIER_DEVIATION_PERCENT_OF_PLANNED_ENTRY), "B")
        val result = matchingResult(listOf(matching, failed))
        assertEquals(listOf(matching), result.matchingCandidates)
        assertEquals(listOf(failed), result.failedCandidates)
    }

    @Test
    fun nonMatchingAndFailedCandidatesWithoutMatchUseNoMatchingResult() {
        val nonMatching = candidate(success(withinAllTargetTolerances = false), "A")
        val failed = candidate(failure(ExistingKnockoutProductTargetFitError.INVALID_MAX_BARRIER_DEVIATION_PERCENT_OF_PLANNED_ENTRY), "B")
        val result = noMatchingResult(listOf(nonMatching, failed))
        assertEquals(listOf(nonMatching), result.nonMatchingCandidates)
        assertEquals(listOf(failed), result.failedCandidates)
    }

    @Test
    fun allThreeGroupsAreRetained() {
        val matching = candidate(success(withinAllTargetTolerances = true), "A")
        val nonMatching = candidate(success(withinAllTargetTolerances = false), "B")
        val failed = candidate(failure(ExistingKnockoutProductTargetFitError.INVALID_RELATIVE_LEVERAGE_DEVIATION_PERCENT), "C")
        val result = matchingResult(listOf(matching, nonMatching, failed))
        assertEquals(listOf(matching), result.matchingCandidates)
        assertEquals(listOf(nonMatching), result.nonMatchingCandidates)
        assertEquals(listOf(failed), result.failedCandidates)
    }

    @Test
    fun eachGroupPreservesInputOrderAndDuplicates() {
        val matching = candidate(success(withinAllTargetTolerances = true), "A")
        val nonMatching = candidate(success(withinAllTargetTolerances = false), "B")
        val failed = candidate(failure(ExistingKnockoutProductTargetFitError.INVALID_BARRIER_DEVIATION_PERCENT_OF_PLANNED_ENTRY), "C")
        val result = matchingResult(listOf(failed, matching, nonMatching, matching, failed, nonMatching))
        assertEquals(listOf(matching, matching), result.matchingCandidates)
        assertEquals(listOf(nonMatching, nonMatching), result.nonMatchingCandidates)
        assertEquals(listOf(failed, failed), result.failedCandidates)
    }

    @Test
    fun successClassificationUsesOnlyWithinAllTargetTolerances() {
        val matching = candidate(
            success(
                leverageWithinTolerance = false,
                barrierWithinTolerance = false,
                withinAllTargetTolerances = true
            ),
            "A"
        )
        val nonMatching = candidate(
            success(
                leverageWithinTolerance = true,
                barrierWithinTolerance = true,
                withinAllTargetTolerances = false
            ),
            "B"
        )
        val result = matchingResult(listOf(matching, nonMatching))
        assertEquals(listOf(matching), result.matchingCandidates)
        assertEquals(listOf(nonMatching), result.nonMatchingCandidates)
    }

    @Test
    fun failureErrorIsNotInterpretedAndOriginalReferencesArePreserved() {
        val firstResult = failure(ExistingKnockoutProductTargetFitError.INVALID_RELATIVE_LEVERAGE_DEVIATION_PERCENT)
        val secondResult = failure(ExistingKnockoutProductTargetFitError.INVALID_MAX_BARRIER_DEVIATION_PERCENT_OF_PLANNED_ENTRY)
        val first = candidate(firstResult, "A")
        val second = candidate(secondResult, "B")
        val result = noMatchingResult(listOf(first, second))
        assertSame(first, result.failedCandidates[0])
        assertSame(second, result.failedCandidates[1])
        assertSame(firstResult, result.failedCandidates[0].targetFitResult)
        assertSame(secondResult, result.failedCandidates[1].targetFitResult)
    }

    @Test
    fun inputListIsNotMutated() {
        val matching = candidate(success(withinAllTargetTolerances = true), "A")
        val nonMatching = candidate(success(withinAllTargetTolerances = false), "B")
        val input = mutableListOf(matching, nonMatching, matching)
        val original = input.toList()
        val request = request(input)
        val result = matchingResult(gate.filter(request))
        assertSame(input, request.candidates)
        assertEquals(original, input)
        assertSame(matching, result.matchingCandidates[0])
        assertSame(matching, result.matchingCandidates[1])
        assertSame(nonMatching, result.nonMatchingCandidates.single())
    }

    private fun matchingResult(
        candidates: List<KnockoutProductCandidateWithTargetFit>
    ) = matchingResult(gate.filter(request(candidates)))

    private fun matchingResult(
        result: KnockoutProductCandidateTargetFitGateResult
    ): KnockoutProductCandidateTargetFitGateResult.CandidatesWithinTargetTolerances {
        assertTrue(result is KnockoutProductCandidateTargetFitGateResult.CandidatesWithinTargetTolerances)
        return result as KnockoutProductCandidateTargetFitGateResult.CandidatesWithinTargetTolerances
    }

    private fun noMatchingResult(
        candidates: List<KnockoutProductCandidateWithTargetFit>
    ) = noMatchingResult(gate.filter(request(candidates)))

    private fun noMatchingResult(
        result: KnockoutProductCandidateTargetFitGateResult
    ): KnockoutProductCandidateTargetFitGateResult.NoCandidatesWithinTargetTolerances {
        assertTrue(result is KnockoutProductCandidateTargetFitGateResult.NoCandidatesWithinTargetTolerances)
        return result as KnockoutProductCandidateTargetFitGateResult.NoCandidatesWithinTargetTolerances
    }

    private fun request(candidates: List<KnockoutProductCandidateWithTargetFit>) =
        KnockoutProductCandidateTargetFitGateRequest(candidates)

    private fun success(
        leverageWithinTolerance: Boolean = true,
        barrierWithinTolerance: Boolean = true,
        withinAllTargetTolerances: Boolean = true
    ) = ExistingKnockoutProductTargetFitResult.Success(
        leverageWithinTolerance,
        barrierWithinTolerance,
        withinAllTargetTolerances
    )

    private fun failure(error: ExistingKnockoutProductTargetFitError) =
        ExistingKnockoutProductTargetFitResult.Failure(error)

    private fun candidate(
        targetFitResult: ExistingKnockoutProductTargetFitResult,
        isin: String = "DE000SYNTH01"
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
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0
            )
        )
        return KnockoutProductCandidateWithTargetFit(targetDeviation, targetFitResult)
    }

    private fun currencyCode(value: String): CurrencyCode =
        (CurrencyCode.create(value) as CurrencyCodeCreationResult.Success).currencyCode
}
