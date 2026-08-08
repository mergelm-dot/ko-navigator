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

class KnockoutProductCandidateTargetFitRankerTest {
    private val ranker = KnockoutProductCandidateTargetFitRanker()

    @Test
    fun emptyCandidatesReturnNoInputCandidates() {
        assertSame(
            KnockoutProductCandidateTargetFitRankingResult.NoInputCandidates,
            ranker.rank(request(emptyList()))
        )
    }

    @Test
    fun oneCandidateIsReturnedUnchanged() {
        val candidate = candidate(relativeLeverageDeviationPercent = 2.0)
        val ranked = ranked(listOf(candidate)).candidates.single()
        assertSame(candidate, ranked)
    }

    @Test
    fun smallerRelativeLeverageDeviationRanksFirstEvenWithWorseBarrierDeviation() {
        val lowerLeverageDeviation = candidate(1.0, 5.0, isin = "A")
        val lowerBarrierDeviation = candidate(2.0, 0.1, isin = "B")
        assertEquals(
            listOf(lowerLeverageDeviation, lowerBarrierDeviation),
            ranked(listOf(lowerBarrierDeviation, lowerLeverageDeviation)).candidates
        )
    }

    @Test
    fun barrierDeviationRanksSecondWhenRelativeLeverageDeviationIsEqual() {
        val lowerBarrierDeviation = candidate(1.0, 0.5, isin = "A")
        val higherBarrierDeviation = candidate(1.0, 0.8, isin = "B")
        assertEquals(
            listOf(lowerBarrierDeviation, higherBarrierDeviation),
            ranked(listOf(higherBarrierDeviation, lowerBarrierDeviation)).candidates
        )
    }

    @Test
    fun threeCandidatesAreSortedLexicographically() {
        val first = candidate(1.0, 0.5, isin = "A")
        val second = candidate(1.0, 0.8, isin = "B")
        val third = candidate(2.0, 0.1, isin = "C")
        assertEquals(listOf(first, second, third), ranked(listOf(third, second, first)).candidates)
    }

    @Test
    fun completeTiesPreserveInputOrderDespiteDifferentOtherDeviationValues() {
        val first = candidate(
            relativeLeverageDeviationPercent = 1.0,
            barrierDeviationPercent = 2.0,
            leverageDifference = -100.0,
            absoluteLeverageDeviation = 999.0,
            barrierDifference = 500.0,
            absoluteBarrierDeviation = 777.0,
            isin = "A"
        )
        val second = candidate(
            relativeLeverageDeviationPercent = 1.0,
            barrierDeviationPercent = 2.0,
            leverageDifference = 100.0,
            absoluteLeverageDeviation = 0.001,
            barrierDifference = -500.0,
            absoluteBarrierDeviation = 0.002,
            isin = "B"
        )
        assertEquals(listOf(second, first), ranked(listOf(second, first)).candidates)
    }

    @Test
    fun multipleCompleteTiesAndDuplicatesRemainStable() {
        val first = candidate(1.0, 1.0, isin = "A")
        val second = candidate(1.0, 1.0, isin = "B")
        val third = candidate(1.0, 1.0, isin = "C")
        val ranked = ranked(listOf(second, first, second, third)).candidates
        assertEquals(listOf(second, first, second, third), ranked)
        assertSame(second, ranked[0])
        assertSame(second, ranked[2])
    }

    @Test
    fun nonRankingDeviationFieldsDoNotInfluenceRanking() {
        val lowerMetrics = candidate(
            relativeLeverageDeviationPercent = 1.0,
            barrierDeviationPercent = 1.0,
            leverageDifference = -10_000.0,
            absoluteLeverageDeviation = 0.0,
            barrierDifference = -10_000.0,
            absoluteBarrierDeviation = 0.0,
            isin = "A"
        )
        val higherMetrics = candidate(
            relativeLeverageDeviationPercent = 2.0,
            barrierDeviationPercent = 0.0,
            leverageDifference = 10_000.0,
            absoluteLeverageDeviation = 10_000.0,
            barrierDifference = 10_000.0,
            absoluteBarrierDeviation = 10_000.0,
            isin = "B"
        )
        assertEquals(listOf(lowerMetrics, higherMetrics), ranked(listOf(higherMetrics, lowerMetrics)).candidates)
    }

    @Test
    fun targetFitResultIsNotReinterpretedOrFiltered() {
        val first = candidate(
            relativeLeverageDeviationPercent = 1.0,
            barrierDeviationPercent = 1.0,
            targetFitResult = ExistingKnockoutProductTargetFitResult.Failure(
                ExistingKnockoutProductTargetFitError.INVALID_MAX_RELATIVE_LEVERAGE_DEVIATION_PERCENT
            ),
            isin = "A"
        )
        val second = candidate(
            relativeLeverageDeviationPercent = 2.0,
            barrierDeviationPercent = 0.0,
            targetFitResult = ExistingKnockoutProductTargetFitResult.Success(
                false,
                false,
                false
            ),
            isin = "B"
        )
        assertEquals(listOf(first, second), ranked(listOf(second, first)).candidates)
    }

    @Test
    fun inputListReferencesAndCandidateObjectsAreNotMutated() {
        val first = candidate(2.0, 1.0, isin = "A")
        val second = candidate(1.0, 2.0, isin = "B")
        val input = mutableListOf(first, second, first)
        val original = input.toList()
        val request = request(input)

        val ranked = ranked(request).candidates

        assertSame(input, request.candidates)
        assertEquals(original, input)
        assertEquals(listOf(second, first, first), ranked)
        assertSame(second, ranked[0])
        assertSame(first, ranked[1])
        assertSame(first, ranked[2])
    }

    private fun ranked(
        candidates: List<KnockoutProductCandidateWithTargetFit>
    ) = ranked(request(candidates))

    private fun ranked(
        request: KnockoutProductCandidateTargetFitRankingRequest
    ): KnockoutProductCandidateTargetFitRankingResult.RankedCandidates {
        val result = ranker.rank(request)
        assertTrue(result is KnockoutProductCandidateTargetFitRankingResult.RankedCandidates)
        return result as KnockoutProductCandidateTargetFitRankingResult.RankedCandidates
    }

    private fun request(candidates: List<KnockoutProductCandidateWithTargetFit>) =
        KnockoutProductCandidateTargetFitRankingRequest(candidates)

    private fun candidate(
        relativeLeverageDeviationPercent: Double,
        barrierDeviationPercent: Double = 1.0,
        leverageDifference: Double = 1.0,
        absoluteLeverageDeviation: Double = 1.0,
        barrierDifference: Double = 1.0,
        absoluteBarrierDeviation: Double = 1.0,
        targetFitResult: ExistingKnockoutProductTargetFitResult =
            ExistingKnockoutProductTargetFitResult.Success(true, true, true),
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
                leverageDifference,
                absoluteLeverageDeviation,
                relativeLeverageDeviationPercent,
                barrierDifference,
                absoluteBarrierDeviation,
                barrierDeviationPercent
            )
        )
        return KnockoutProductCandidateWithTargetFit(targetDeviation, targetFitResult)
    }

    private fun currencyCode(value: String): CurrencyCode =
        (CurrencyCode.create(value) as CurrencyCodeCreationResult.Success).currencyCode
}
