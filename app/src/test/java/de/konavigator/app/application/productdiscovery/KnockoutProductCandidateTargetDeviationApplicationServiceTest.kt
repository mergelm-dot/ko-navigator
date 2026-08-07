package de.konavigator.app.application.productdiscovery

import de.konavigator.app.calculator.ExistingKnockoutProductEntryCalculationResult
import de.konavigator.app.calculator.ExistingKnockoutProductTargetDeviationError
import de.konavigator.app.calculator.ExistingKnockoutProductTargetDeviationResult
import de.konavigator.app.calculator.ExistingKnockoutProductTargetDeviationCalculator
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

class KnockoutProductCandidateTargetDeviationApplicationServiceTest {
    private val service = KnockoutProductCandidateTargetDeviationApplicationService(
        ExistingKnockoutProductTargetDeviationCalculator
    )

    @Test
    fun emptyCandidatesReturnNoInputCandidates() {
        assertSame(
            KnockoutProductCandidateTargetDeviationResult.NoInputCandidates,
            service.execute(request(emptyList()))
        )
    }

    @Test
    fun oneCandidateProducesExactlyOneDeviationResult() {
        val candidate = candidate()
        val result = result(request(listOf(candidate)))
        assertEquals(1, result.candidates.size)
        assertSame(candidate, result.candidates.single().candidateWithExistingEntryCalculation)
    }

    @Test
    fun targetLeverageComesFromValidTradeCalculationResult() {
        val candidate = candidate(targetLeverage = 8.0, actualLeverage = 6.0)
        val deviation = success(single(candidate).targetDeviationResult)
        assertEquals(-2.0, deviation.leverageDifference, 0.0)
    }

    @Test
    fun actualLeverageComesFromExistingEntrySuccess() {
        val candidate = candidate(targetLeverage = 5.0, actualLeverage = 7.0)
        assertEquals(2.0, success(single(candidate).targetDeviationResult).leverageDifference, 0.0)
    }

    @Test
    fun targetKnockoutBarrierComesFromTradeCalculationResult() {
        val candidate = candidate(targetBarrier = 80.0, actualBarrier = 100.0)
        assertEquals(20.0, success(single(candidate).targetDeviationResult).barrierDifference, 0.0)
    }

    @Test
    fun actualKnockoutBarrierComesFromProductSpecification() {
        val candidate = candidate(targetBarrier = 90.0, actualBarrier = 110.0)
        assertEquals(20.0, success(single(candidate).targetDeviationResult).barrierDifference, 0.0)
    }

    @Test
    fun basePriceIsNotUsedAsActualKnockoutBarrier() {
        val candidate = candidate(basePrice = 10.0, targetBarrier = 90.0, actualBarrier = 100.0)
        val deviation = success(single(candidate).targetDeviationResult)
        assertEquals(10.0, deviation.barrierDifference, 0.0)
        assertTrue(deviation.barrierDifference != 10.0 - 90.0)
    }

    @Test
    fun exactMatchesProduceAllowedZeroDeviations() {
        val candidate = candidate(targetLeverage = 5.0, actualLeverage = 5.0, targetBarrier = 90.0, actualBarrier = 90.0)
        val deviation = success(single(candidate).targetDeviationResult)
        assertEquals(0.0, deviation.leverageDifference, 0.0)
        assertEquals(0.0, deviation.absoluteLeverageDeviation, 0.0)
        assertEquals(0.0, deviation.relativeLeverageDeviationPercent, 0.0)
        assertEquals(0.0, deviation.barrierDifference, 0.0)
        assertEquals(0.0, deviation.absoluteBarrierDeviation, 0.0)
        assertEquals(0.0, deviation.barrierDeviationPercentOfPlannedEntry, 0.0)
    }

    @Test
    fun positiveAndNegativeDifferencesAreTransportedUnchanged() {
        val positive = candidate(isin = "A", targetLeverage = 5.0, actualLeverage = 7.0, targetBarrier = 90.0, actualBarrier = 100.0)
        val negative = candidate(isin = "B", targetLeverage = 7.0, actualLeverage = 5.0, targetBarrier = 100.0, actualBarrier = 90.0)
        val candidates = result(request(listOf(positive, negative))).candidates
        assertEquals(2.0, success(candidates[0].targetDeviationResult).leverageDifference, 0.0)
        assertEquals(10.0, success(candidates[0].targetDeviationResult).barrierDifference, 0.0)
        assertEquals(-2.0, success(candidates[1].targetDeviationResult).leverageDifference, 0.0)
        assertEquals(-10.0, success(candidates[1].targetDeviationResult).barrierDifference, 0.0)
    }

    @Test
    fun typedDeviationFailureIsTransportedWithoutFilteringCandidate() {
        val candidate = candidate()
        val result = result(request(listOf(candidate), plannedEntryPrice = 0.0))
        assertEquals(1, result.candidates.size)
        assertSame(candidate, result.candidates.single().candidateWithExistingEntryCalculation)
        val failure = result.candidates.single().targetDeviationResult
        assertTrue(failure is ExistingKnockoutProductTargetDeviationResult.Failure)
        assertEquals(
            ExistingKnockoutProductTargetDeviationError.INVALID_PLANNED_ENTRY_PRICE,
            (failure as ExistingKnockoutProductTargetDeviationResult.Failure).error
        )
    }

    @Test
    fun inputOrderIsPreserved() {
        val first = candidate(isin = "A")
        val second = candidate(isin = "B")
        val third = candidate(isin = "C")
        assertEquals(
            listOf(first, second, third),
            result(request(listOf(first, second, third))).candidates.map { it.candidateWithExistingEntryCalculation }
        )
    }

    @Test
    fun duplicateCandidatesRemainDuplicated() {
        val candidate = candidate()
        val candidates = result(request(listOf(candidate, candidate))).candidates
        assertEquals(2, candidates.size)
        assertSame(candidate, candidates[0].candidateWithExistingEntryCalculation)
        assertSame(candidate, candidates[1].candidateWithExistingEntryCalculation)
    }

    @Test
    fun originalCandidateReferenceAndInputListRemainUnchanged() {
        val candidate = candidate()
        val candidates = mutableListOf(candidate, candidate)
        val original = candidates.toList()
        val request = request(candidates, plannedEntryPrice = 123.45)
        val result = result(request)
        assertSame(candidates, request.candidates)
        assertEquals(original, candidates)
        assertSame(candidate, result.candidates.first().candidateWithExistingEntryCalculation)
        assertSame(candidate.existingEntryCalculationResult, result.candidates.first().candidateWithExistingEntryCalculation.existingEntryCalculationResult)
        assertSame(candidate.candidateWithTargetLeveragePlan, result.candidates.first().candidateWithExistingEntryCalculation.candidateWithTargetLeveragePlan)
        assertEquals(123.45, request.plannedEntryPrice, 0.0)
    }

    private fun single(
        candidate: KnockoutProductCandidateWithExistingEntryCalculation
    ) = result(request(listOf(candidate))).candidates.single()

    private fun result(
        request: KnockoutProductCandidateTargetDeviationRequest
    ): KnockoutProductCandidateTargetDeviationResult.CandidatesWithTargetDeviation {
        val result = service.execute(request)
        assertTrue(result is KnockoutProductCandidateTargetDeviationResult.CandidatesWithTargetDeviation)
        return result as KnockoutProductCandidateTargetDeviationResult.CandidatesWithTargetDeviation
    }

    private fun success(
        result: ExistingKnockoutProductTargetDeviationResult
    ): ExistingKnockoutProductTargetDeviationResult.Success {
        assertTrue(result is ExistingKnockoutProductTargetDeviationResult.Success)
        return result as ExistingKnockoutProductTargetDeviationResult.Success
    }

    private fun request(
        candidates: List<KnockoutProductCandidateWithExistingEntryCalculation>,
        plannedEntryPrice: Double = 120.0
    ) = KnockoutProductCandidateTargetDeviationRequest(candidates, plannedEntryPrice)

    private fun candidate(
        isin: String = "DE000SYNTH01",
        basePrice: Double = 100.0,
        targetLeverage: Double = 5.0,
        actualLeverage: Double = 6.0,
        targetBarrier: Double = 90.0,
        actualBarrier: Double = 100.0
    ): KnockoutProductCandidateWithExistingEntryCalculation {
        val specification = KnockoutProductSpecification(
            isin, "SYN001", "issuer", "underlying", TradeDirection.LONG,
            basePrice, actualBarrier, 0.1, "EUR", "EUR"
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
                true, 120.0, targetLeverage, targetBarrier, 1.0, 1.0, 12.0,
                targetLeverage, currencyCode("EUR"), currencyCode("EUR"), 30.0, 25.0
            )
        )
        val existingEntry = ExistingKnockoutProductEntryCalculationResult.Success(
            1.0, 1.0, 20.0, 20.0, 12.0, actualLeverage,
            currencyCode("EUR"), currencyCode("EUR")
        )
        return KnockoutProductCandidateWithExistingEntryCalculation(targetPlan, existingEntry)
    }

    private fun currencyCode(value: String): CurrencyCode =
        (CurrencyCode.create(value) as CurrencyCodeCreationResult.Success).currencyCode
}
