package de.konavigator.app.application.productdiscovery

import de.konavigator.app.calculator.ExistingKnockoutProductEntryCalculationResult
import de.konavigator.app.calculator.ExistingKnockoutProductTargetDeviationCalculator
import de.konavigator.app.calculator.ExistingKnockoutProductTargetFitCalculator
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

class KnockoutProductCandidateTargetSelectionApplicationServiceTest {
    private val service = KnockoutProductCandidateTargetSelectionApplicationService(
        KnockoutProductCandidateTargetDeviationApplicationService(
            ExistingKnockoutProductTargetDeviationCalculator
        ),
        KnockoutProductCandidateTargetDeviationGate(),
        KnockoutProductCandidateTargetFitApplicationService(
            ExistingKnockoutProductTargetFitCalculator
        ),
        KnockoutProductCandidateTargetFitGate(),
        KnockoutProductCandidateTargetFitRanker(),
        KnockoutProductCandidateTargetFitSelector()
    )

    @Test
    fun emptyCandidatesReturnNoInputCandidates() {
        assertSame(
            KnockoutProductCandidateTargetSelectionApplicationResult.NoInputCandidates,
            service.execute(request(emptyList()))
        )
    }

    @Test
    fun oneMatchingCandidateIsSelectedAsPrimary() {
        val candidate = candidate("A", actualLeverage = 5.0, actualBarrier = 90.0)
        val result = selected(request(listOf(candidate)))
        assertSame(
            candidate,
            result.primaryCandidate.candidateWithTargetDeviation
                .candidateWithExistingEntryCalculation
        )
        assertTrue(result.alternativeCandidates.isEmpty())
        assertTrue(result.targetDeviationFailedCandidates.isEmpty())
        assertTrue(result.nonMatchingCandidates.isEmpty())
        assertTrue(result.targetFitFailedCandidates.isEmpty())
    }

    @Test
    fun matchingCandidatesAreRankedBeforeSelection() {
        val worst = candidate("A", actualLeverage = 7.0, actualBarrier = 90.0)
        val best = candidate("B", actualLeverage = 5.5, actualBarrier = 90.0)
        val middle = candidate("C", actualLeverage = 6.0, actualBarrier = 90.0)
        val result = selected(request(listOf(worst, best, middle), maxLeverageDeviationPercent = 50.0))

        assertSame(best, existingEntryCandidate(result.primaryCandidate))
        assertEquals(
            listOf(middle, worst),
            result.alternativeCandidates.map(::existingEntryCandidate)
        )
    }

    @Test
    fun onlyTwoAlternativesAreSelectedFromMoreThanThreeMatches() {
        val candidates = listOf(
            candidate("A", actualLeverage = 5.1),
            candidate("B", actualLeverage = 5.2),
            candidate("C", actualLeverage = 5.3),
            candidate("D", actualLeverage = 5.4)
        )
        val result = selected(request(candidates, maxLeverageDeviationPercent = 50.0))
        assertEquals(2, result.alternativeCandidates.size)
        assertSame(candidates[0], existingEntryCandidate(result.primaryCandidate))
        assertEquals(
            listOf(candidates[1], candidates[2]),
            result.alternativeCandidates.map(::existingEntryCandidate)
        )
    }

    @Test
    fun targetDeviationFailuresAreRetainedWhileSuccessfulCandidatesContinue() {
        val matching = candidate("A", actualLeverage = 5.0, actualBarrier = 90.0)
        val failingDeviation = candidate("B", targetLeverage = 0.0, actualLeverage = 5.0)
        val result = selected(request(listOf(failingDeviation, matching)))
        assertSame(matching, existingEntryCandidate(result.primaryCandidate))
        assertSame(
            failingDeviation,
            existingEntryCandidate(result.targetDeviationFailedCandidates.single())
        )
    }

    @Test
    fun allTargetDeviationFailuresReturnTypedNoSuccessfulResult() {
        val first = candidate("A", targetLeverage = 0.0)
        val second = candidate("B", targetLeverage = 0.0)
        val result = service.execute(request(listOf(first, second)))
        assertTrue(
            result is KnockoutProductCandidateTargetSelectionApplicationResult
                .NoSuccessfulTargetDeviationCandidates
        )
        result as KnockoutProductCandidateTargetSelectionApplicationResult
            .NoSuccessfulTargetDeviationCandidates
        assertEquals(
            listOf(first, second),
            result.failedCandidates.map(::existingEntryCandidate)
        )
    }

    @Test
    fun noTargetFitMatchRetainsNonMatchingAndFitFailuresSeparately() {
        val nonMatching = candidate("A", actualLeverage = 7.0)
        val fitFailure = candidate("B", actualLeverage = 5.0)
        val result = service.execute(
            request(
                listOf(nonMatching, fitFailure),
                maxLeverageDeviationPercent = -1.0
            )
        )
        assertTrue(
            result is KnockoutProductCandidateTargetSelectionApplicationResult
                .NoCandidatesWithinTargetTolerances
        )
        result as KnockoutProductCandidateTargetSelectionApplicationResult
            .NoCandidatesWithinTargetTolerances
        assertTrue(result.nonMatchingCandidates.isEmpty())
        assertEquals(
            listOf(nonMatching, fitFailure),
            result.targetFitFailedCandidates.map(::existingEntryCandidate)
        )
    }

    @Test
    fun noTargetFitMatchRetainsNonMatchingCandidates() {
        val candidate = candidate("A", actualLeverage = 7.0)
        val result = service.execute(request(listOf(candidate), maxLeverageDeviationPercent = 1.0))
        assertTrue(
            result is KnockoutProductCandidateTargetSelectionApplicationResult
                .NoCandidatesWithinTargetTolerances
        )
        result as KnockoutProductCandidateTargetSelectionApplicationResult
            .NoCandidatesWithinTargetTolerances
        assertSame(candidate, existingEntryCandidate(result.nonMatchingCandidates.single()))
        assertTrue(result.targetFitFailedCandidates.isEmpty())
    }

    @Test
    fun inputReferencesDuplicatesAndDiagnosticGroupsRemainUnchanged() {
        val matching = candidate("A", actualLeverage = 5.0)
        val nonMatching = candidate("B", actualLeverage = 7.0)
        val input = mutableListOf(matching, nonMatching, matching)
        val original = input.toList()
        val request = request(input, maxLeverageDeviationPercent = 1.0)
        val result = selected(request)

        assertSame(input, request.candidates)
        assertEquals(original, input)
        assertSame(matching, existingEntryCandidate(result.primaryCandidate))
        assertEquals(
            listOf(matching),
            result.alternativeCandidates.map(::existingEntryCandidate)
        )
        assertSame(nonMatching, existingEntryCandidate(result.nonMatchingCandidates.single()))
    }

    private fun selected(
        request: KnockoutProductCandidateTargetSelectionApplicationRequest
    ): KnockoutProductCandidateTargetSelectionApplicationResult.SelectedCandidates {
        val result = service.execute(request)
        assertTrue(
            result is KnockoutProductCandidateTargetSelectionApplicationResult.SelectedCandidates
        )
        return result as KnockoutProductCandidateTargetSelectionApplicationResult.SelectedCandidates
    }

    private fun existingEntryCandidate(
        candidate: KnockoutProductCandidateWithTargetFit
    ) = candidate.candidateWithTargetDeviation.candidateWithExistingEntryCalculation

    private fun existingEntryCandidate(
        candidate: KnockoutProductCandidateWithTargetDeviation
    ) = candidate.candidateWithExistingEntryCalculation

    private fun request(
        candidates: List<KnockoutProductCandidateWithExistingEntryCalculation>,
        plannedEntryPrice: Double = 100.0,
        maxLeverageDeviationPercent: Double = 1.0,
        maxBarrierDeviationPercent: Double = 1.0
    ) = KnockoutProductCandidateTargetSelectionApplicationRequest(
        candidates,
        plannedEntryPrice,
        maxLeverageDeviationPercent,
        maxBarrierDeviationPercent
    )

    private fun candidate(
        isin: String,
        targetLeverage: Double = 5.0,
        actualLeverage: Double = 5.0,
        targetBarrier: Double = 90.0,
        actualBarrier: Double = 90.0
    ): KnockoutProductCandidateWithExistingEntryCalculation {
        val specification = KnockoutProductSpecification(
            isin, "SYN001", "issuer", "underlying", TradeDirection.LONG,
            100.0, actualBarrier, 0.1, "EUR", "EUR"
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
                true, 100.0, targetLeverage, targetBarrier, 1.0, 1.0, 10.0,
                targetLeverage, currencyCode("EUR"), currencyCode("EUR"), 30.0, 25.0
            )
        )
        return KnockoutProductCandidateWithExistingEntryCalculation(
            targetPlan,
            ExistingKnockoutProductEntryCalculationResult.Success(
                1.0, 1.0, 20.0, 20.0, 10.0, actualLeverage,
                currencyCode("EUR"), currencyCode("EUR")
            )
        )
    }

    private fun currencyCode(value: String): CurrencyCode =
        (CurrencyCode.create(value) as CurrencyCodeCreationResult.Success).currencyCode
}
