package de.konavigator.app.application.productdiscovery

import de.konavigator.app.calculator.ExistingKnockoutProductEntryCalculationResult
import de.konavigator.app.calculator.ExistingKnockoutProductTargetDeviationResult
import de.konavigator.app.calculator.ExistingKnockoutProductTargetFitCalculator
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductCandidateTargetFitApplicationServiceTest {
    private val service = KnockoutProductCandidateTargetFitApplicationService(
        ExistingKnockoutProductTargetFitCalculator
    )

    @Test
    fun emptyCandidatesReturnNoInputCandidates() {
        assertSame(
            KnockoutProductCandidateTargetFitResult.NoInputCandidates,
            service.execute(request(emptyList()))
        )
    }

    @Test
    fun oneCandidateProducesOneTargetFitResultUsingItsDeviationSuccess() {
        val deviation = deviation(relativeLeverageDeviationPercent = 2.0, barrierDeviationPercent = 3.0)
        val candidate = candidate(deviation = deviation)
        val result = result(request(listOf(candidate), 2.0, 3.0)).candidates.single()

        assertSame(candidate, result.candidateWithTargetDeviation)
        assertTrue(result.targetFitResult is ExistingKnockoutProductTargetFitResult.Success)
        val targetFit = result.targetFitResult as ExistingKnockoutProductTargetFitResult.Success
        assertTrue(targetFit.leverageWithinTolerance)
        assertTrue(targetFit.barrierWithinTolerance)
        assertTrue(targetFit.withinAllTargetTolerances)
    }

    @Test
    fun leverageToleranceComesFromRequest() {
        val result = single(
            candidate(deviation = deviation(relativeLeverageDeviationPercent = 2.0)),
            1.0,
            5.0
        )
        assertFalse(success(result.targetFitResult).leverageWithinTolerance)
    }

    @Test
    fun barrierToleranceComesFromRequest() {
        val result = single(
            candidate(deviation = deviation(barrierDeviationPercent = 2.0)),
            5.0,
            1.0
        )
        assertFalse(success(result.targetFitResult).barrierWithinTolerance)
    }

    @Test
    fun leverageOutsideTargetFitIsRetained() {
        val targetFitCandidate =
            single(
                candidate(deviation = deviation(relativeLeverageDeviationPercent = 6.0)),
                5.0,
                5.0
            )
        assertFalse(success(targetFitCandidate.targetFitResult).leverageWithinTolerance)
        assertEquals(
            1,
            result(request(listOf(targetFitCandidate.candidateWithTargetDeviation))).candidates.size
        )
    }

    @Test
    fun barrierOutsideTargetFitIsRetained() {
        val result = single(
            candidate(deviation = deviation(barrierDeviationPercent = 6.0)),
            5.0,
            5.0
        )
        assertFalse(success(result.targetFitResult).barrierWithinTolerance)
    }

    @Test
    fun bothOutsideTargetFitIsRetained() {
        val result = single(candidate(deviation = deviation(6.0, 7.0)), 5.0, 5.0)
        val targetFit = success(result.targetFitResult)
        assertFalse(targetFit.leverageWithinTolerance)
        assertFalse(targetFit.barrierWithinTolerance)
        assertFalse(targetFit.withinAllTargetTolerances)
    }

    @Test
    fun invalidLeverageToleranceIsRetainedAsTypedFailure() {
        val targetFit = single(candidate(), -1.0, 5.0).targetFitResult
        assertFailure(
            targetFit,
            ExistingKnockoutProductTargetFitError.INVALID_MAX_RELATIVE_LEVERAGE_DEVIATION_PERCENT
        )
    }

    @Test
    fun invalidBarrierToleranceIsRetainedAsTypedFailure() {
        val targetFit = single(candidate(), 5.0, Double.NaN).targetFitResult
        assertFailure(
            targetFit,
            ExistingKnockoutProductTargetFitError.INVALID_MAX_BARRIER_DEVIATION_PERCENT_OF_PLANNED_ENTRY
        )
    }

    @Test
    fun orderDuplicatesReferencesAndInputListArePreserved() {
        val first = candidate(isin = "A")
        val second = candidate(isin = "B")
        val inputCandidates = mutableListOf(first, second, first)
        val original = inputCandidates.toList()
        val request = request(inputCandidates, 5.0, 5.0)

        val result = result(request)

        assertSame(inputCandidates, request.candidates)
        assertEquals(original, inputCandidates)
        assertEquals(listOf(first, second, first), result.candidates.map { it.candidateWithTargetDeviation })
        assertSame(first, result.candidates[0].candidateWithTargetDeviation)
        assertSame(second, result.candidates[1].candidateWithTargetDeviation)
        assertSame(first, result.candidates[2].candidateWithTargetDeviation)
    }

    private fun single(
        candidate: KnockoutProductCandidateWithTargetDeviation,
        maxLeverageDeviationPercent: Double,
        maxBarrierDeviationPercent: Double
    ) = result(
        request(listOf(candidate), maxLeverageDeviationPercent, maxBarrierDeviationPercent)
    ).candidates.single()

    private fun result(
        request: KnockoutProductCandidateTargetFitRequest
    ): KnockoutProductCandidateTargetFitResult.CandidatesWithTargetFit {
        val result = service.execute(request)
        assertTrue(result is KnockoutProductCandidateTargetFitResult.CandidatesWithTargetFit)
        return result as KnockoutProductCandidateTargetFitResult.CandidatesWithTargetFit
    }

    private fun success(
        result: ExistingKnockoutProductTargetFitResult
    ): ExistingKnockoutProductTargetFitResult.Success {
        assertTrue(result is ExistingKnockoutProductTargetFitResult.Success)
        return result as ExistingKnockoutProductTargetFitResult.Success
    }

    private fun assertFailure(
        result: ExistingKnockoutProductTargetFitResult,
        error: ExistingKnockoutProductTargetFitError
    ) {
        assertTrue(result is ExistingKnockoutProductTargetFitResult.Failure)
        assertEquals(error, (result as ExistingKnockoutProductTargetFitResult.Failure).error)
    }

    private fun request(
        candidates: List<KnockoutProductCandidateWithTargetDeviation>,
        maxLeverageDeviationPercent: Double = 5.0,
        maxBarrierDeviationPercent: Double = 5.0
    ) = KnockoutProductCandidateTargetFitRequest(
        candidates,
        maxLeverageDeviationPercent,
        maxBarrierDeviationPercent
    )

    private fun deviation(
        relativeLeverageDeviationPercent: Double = 1.0,
        barrierDeviationPercent: Double = 1.0
    ) = ExistingKnockoutProductTargetDeviationResult.Success(
        leverageDifference = 1.0,
        absoluteLeverageDeviation = 1.0,
        relativeLeverageDeviationPercent = relativeLeverageDeviationPercent,
        barrierDifference = 1.0,
        absoluteBarrierDeviation = 1.0,
        barrierDeviationPercentOfPlannedEntry = barrierDeviationPercent
    )

    private fun candidate(
        isin: String = "DE000SYNTH01",
        deviation: ExistingKnockoutProductTargetDeviationResult.Success = deviation()
    ): KnockoutProductCandidateWithTargetDeviation {
        val specification = KnockoutProductSpecification(
            isin, "SYN001", "issuer", "underlying", TradeDirection.LONG,
            100.0, 100.0, 0.1, "EUR", "EUR"
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
        val existingEntry = ExistingKnockoutProductEntryCalculationResult.Success(
            1.0, 1.0, 20.0, 20.0, 12.0, 5.0,
            currencyCode("EUR"), currencyCode("EUR")
        )
        return KnockoutProductCandidateWithTargetDeviation(
            KnockoutProductCandidateWithExistingEntryCalculation(targetPlan, existingEntry),
            deviation
        )
    }

    private fun currencyCode(value: String): CurrencyCode =
        (CurrencyCode.create(value) as CurrencyCodeCreationResult.Success).currencyCode
}
