package de.konavigator.app.application.productdiscovery

import de.konavigator.app.calculator.ExistingKnockoutProductEntryCalculationResult
import de.konavigator.app.calculator.ExistingKnockoutProductTargetDeviationError
import de.konavigator.app.calculator.ExistingKnockoutProductTargetDeviationResult
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductCandidateTargetDeviationGateTest {
    private val gate = KnockoutProductCandidateTargetDeviationGate()

    @Test
    fun emptyListReturnsNoInputCandidates() {
        assertSame(
            KnockoutProductCandidateTargetDeviationGateResult.NoInputCandidates,
            gate.filter(request(emptyList()))
        )
    }

    @Test
    fun oneSuccessReturnsSuccessfulTargetDeviationCandidates() {
        val candidate = candidate(success())
        val result = successfulResult(listOf(candidate))
        assertEquals(listOf(candidate), result.successfulCandidates)
        assertTrue(result.failedCandidates.isEmpty())
    }

    @Test
    fun oneFailureReturnsNoSuccessfulTargetDeviationCandidates() {
        val deviation = failure(ExistingKnockoutProductTargetDeviationError.INVALID_PLANNED_ENTRY_PRICE)
        val candidate = candidate(deviation)
        val result = gate.filter(request(listOf(candidate)))
            as KnockoutProductCandidateTargetDeviationGateResult.NoSuccessfulTargetDeviationCandidates
        assertSame(candidate, result.failedCandidates.single())
        assertSame(deviation, result.failedCandidates.single().targetDeviationResult)
    }

    @Test
    fun mixedCandidatesArePartitioned() {
        val first = candidate(success(), isin = "A")
        val second = candidate(failure(ExistingKnockoutProductTargetDeviationError.INVALID_TARGET_LEVERAGE), isin = "B")
        val third = candidate(success(), isin = "C")
        val result = successfulResult(listOf(first, second, third))
        assertEquals(listOf(first, third), result.successfulCandidates)
        assertEquals(listOf(second), result.failedCandidates)
        assertEquals(3, result.successfulCandidates.size + result.failedCandidates.size)
        assertFalse(result.successfulCandidates.any { it === second })
    }

    @Test
    fun multipleSuccessCandidatesPreserveOrder() {
        val first = candidate(success(), isin = "A")
        val failed = candidate(failure(ExistingKnockoutProductTargetDeviationError.INVALID_ACTUAL_LEVERAGE), isin = "B")
        val third = candidate(success(), isin = "C")
        assertEquals(listOf(first, third), successfulResult(listOf(first, failed, third)).successfulCandidates)
    }

    @Test
    fun multipleFailureCandidatesPreserveOrder() {
        val first = candidate(failure(ExistingKnockoutProductTargetDeviationError.INVALID_TARGET_KNOCKOUT_BARRIER), isin = "A")
        val success = candidate(success(), isin = "B")
        val third = candidate(failure(ExistingKnockoutProductTargetDeviationError.INVALID_BARRIER_DEVIATION), isin = "C")
        assertEquals(listOf(first, third), successfulResult(listOf(first, success, third)).failedCandidates)
    }

    @Test
    fun mixedOrderIsPreservedInsideEachPartition() {
        val candidates = listOf(
            candidate(failure(ExistingKnockoutProductTargetDeviationError.INVALID_TARGET_LEVERAGE), isin = "A"),
            candidate(success(), isin = "B"),
            candidate(failure(ExistingKnockoutProductTargetDeviationError.INVALID_ACTUAL_KNOCKOUT_BARRIER), isin = "C"),
            candidate(success(), isin = "D")
        )
        val result = successfulResult(candidates)
        assertEquals(listOf(candidates[1], candidates[3]), result.successfulCandidates)
        assertEquals(listOf(candidates[0], candidates[2]), result.failedCandidates)
    }

    @Test
    fun duplicatesRemainPreserved() {
        val success = candidate(success())
        val failure = candidate(failure(ExistingKnockoutProductTargetDeviationError.INVALID_LEVERAGE_DEVIATION))
        val result = successfulResult(listOf(success, failure, success, failure))
        assertEquals(listOf(success, success), result.successfulCandidates)
        assertEquals(listOf(failure, failure), result.failedCandidates)
        assertSame(success, result.successfulCandidates[0])
        assertSame(failure, result.failedCandidates[0])
    }

    @Test
    fun originalCandidateReferencesRemainUnchanged() {
        val deviation = success()
        val candidate = candidate(deviation)
        val preserved = successfulResult(listOf(candidate)).successfulCandidates.single()
        assertSame(candidate, preserved)
        assertSame(deviation, preserved.targetDeviationResult)
        assertSame(candidate.candidateWithExistingEntryCalculation, preserved.candidateWithExistingEntryCalculation)
        assertSame(candidate.candidateWithExistingEntryCalculation.existingEntryCalculationResult, preserved.candidateWithExistingEntryCalculation.existingEntryCalculationResult)
        assertSame(candidate.candidateWithExistingEntryCalculation.candidateWithTargetLeveragePlan, preserved.candidateWithExistingEntryCalculation.candidateWithTargetLeveragePlan)
    }

    @Test
    fun differentSuccessDeviationValuesDoNotAffectPartition() {
        val exact = candidate(success())
        val veryLarge = candidate(success(1_000_000.0, 1_000_000.0, 20_000_000.0, -3_000_000.0, 3_000_000.0, 2_500_000.0), isin = "B")
        assertEquals(listOf(exact, veryLarge), successfulResult(listOf(exact, veryLarge)).successfulCandidates)
    }

    @Test
    fun differentFailureErrorsRemainFailuresWithoutTranslation() {
        val firstResult = failure(ExistingKnockoutProductTargetDeviationError.INVALID_PLANNED_ENTRY_PRICE)
        val secondResult = failure(ExistingKnockoutProductTargetDeviationError.INVALID_BARRIER_DEVIATION)
        val first = candidate(firstResult)
        val second = candidate(secondResult, isin = "B")
        val result = gate.filter(request(listOf(first, second)))
            as KnockoutProductCandidateTargetDeviationGateResult.NoSuccessfulTargetDeviationCandidates
        assertSame(firstResult, result.failedCandidates[0].targetDeviationResult)
        assertSame(secondResult, result.failedCandidates[1].targetDeviationResult)
    }

    @Test
    fun inputListIsNotMutated() {
        val success = candidate(success(), isin = "A")
        val failure = candidate(failure(ExistingKnockoutProductTargetDeviationError.INVALID_TARGET_LEVERAGE), isin = "B")
        val input = mutableListOf(success, failure, success)
        val original = input.toList()
        val request = request(input)
        val result = successfulResult(gate.filter(request))
        assertSame(input, request.candidates)
        assertEquals(original, input)
        assertEquals(listOf(success, success), result.successfulCandidates)
        assertEquals(listOf(failure), result.failedCandidates)
    }

    private fun successfulResult(
        candidates: List<KnockoutProductCandidateWithTargetDeviation>
    ) = successfulResult(gate.filter(request(candidates)))

    private fun successfulResult(
        result: KnockoutProductCandidateTargetDeviationGateResult
    ): KnockoutProductCandidateTargetDeviationGateResult.SuccessfulTargetDeviationCandidates {
        assertTrue(result is KnockoutProductCandidateTargetDeviationGateResult.SuccessfulTargetDeviationCandidates)
        return result as KnockoutProductCandidateTargetDeviationGateResult.SuccessfulTargetDeviationCandidates
    }

    private fun request(candidates: List<KnockoutProductCandidateWithTargetDeviation>) =
        KnockoutProductCandidateTargetDeviationGateRequest(candidates)

    private fun success(
        leverageDifference: Double = 0.0,
        absoluteLeverageDeviation: Double = 0.0,
        relativeLeverageDeviationPercent: Double = 0.0,
        barrierDifference: Double = 0.0,
        absoluteBarrierDeviation: Double = 0.0,
        barrierDeviationPercentOfPlannedEntry: Double = 0.0
    ) = ExistingKnockoutProductTargetDeviationResult.Success(
        leverageDifference,
        absoluteLeverageDeviation,
        relativeLeverageDeviationPercent,
        barrierDifference,
        absoluteBarrierDeviation,
        barrierDeviationPercentOfPlannedEntry
    )

    private fun failure(error: ExistingKnockoutProductTargetDeviationError) =
        ExistingKnockoutProductTargetDeviationResult.Failure(error)

    private fun candidate(
        targetDeviationResult: ExistingKnockoutProductTargetDeviationResult,
        isin: String = "DE000SYNTH01"
    ): KnockoutProductCandidateWithTargetDeviation {
        val specification = KnockoutProductSpecification(isin, "SYN001", "issuer", "underlying", TradeDirection.LONG, 100.0, 90.0, 0.1, "EUR", "EUR")
        val snapshot = KnockoutProductSpecificationSnapshot(specification, "snapshot", 1000L, 900L)
        val marketData = KnockoutProductMarketData(isin, 1.0, 1.2, 950L, 950L, "EUR", "SYNTH_SOURCE")
        val marketCandidate = KnockoutProductCandidateWithMarketData(snapshot, marketData)
        val qualityCandidate = KnockoutProductCandidateWithDataQuality(marketCandidate, DataQualityAssessment.passed())
        val availabilityCandidate = KnockoutProductCandidateWithCalculationAvailability(qualityCandidate, MarketDataCalculationAvailabilityResult.StructurallyAvailable)
        val freshnessCandidate = KnockoutProductCandidateWithFreshness(availabilityCandidate, MarketDataFreshnessResult.Fresh)
        val sourceCandidate = KnockoutProductCandidateWithSourceEvaluation(freshnessCandidate, MarketDataSourceResult.Allowed)
        val calculationCandidate = KnockoutProductCandidateWithCalculation(sourceCandidate, KnockoutProductCandidateCalculationOutcome.Success(MarketDataCalculationValue.MidPrice(1.0, "EUR")))
        val targetInput = KnockoutProductCandidateTargetLeverageInput(calculationCandidate, CurrencyConversion.SameCurrency(currencyCode("EUR")))
        val targetPlan = KnockoutProductCandidateWithTargetLeveragePlan(
            targetInput,
            TradeCalculationResult(true, 120.0, 5.0, 90.0, 1.0, 1.0, 12.0, 5.0, currencyCode("EUR"), currencyCode("EUR"), 30.0, 25.0)
        )
        val existingEntry = KnockoutProductCandidateWithExistingEntryCalculation(
            targetPlan,
            ExistingKnockoutProductEntryCalculationResult.Success(1.0, 1.0, 20.0, 20.0, 12.0, 5.0, currencyCode("EUR"), currencyCode("EUR"))
        )
        return KnockoutProductCandidateWithTargetDeviation(existingEntry, targetDeviationResult)
    }

    private fun currencyCode(value: String): CurrencyCode =
        (CurrencyCode.create(value) as CurrencyCodeCreationResult.Success).currencyCode
}
