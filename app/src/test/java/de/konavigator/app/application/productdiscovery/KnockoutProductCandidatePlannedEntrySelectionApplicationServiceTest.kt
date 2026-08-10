package de.konavigator.app.application.productdiscovery

import de.konavigator.app.calculator.ExistingKnockoutProductEntryCalculator
import de.konavigator.app.calculator.ExistingKnockoutProductTargetDeviationCalculator
import de.konavigator.app.calculator.ExistingKnockoutProductTargetFitCalculator
import de.konavigator.app.calculator.TradeCalculationEngine
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

class KnockoutProductCandidatePlannedEntrySelectionApplicationServiceTest {
    private val service = KnockoutProductCandidatePlannedEntrySelectionApplicationService(
        KnockoutProductCandidateTargetLeverageApplicationService(TradeCalculationEngine),
        KnockoutProductCandidateTargetLeverageGate(),
        KnockoutProductCandidateExistingEntryCalculationApplicationService(
            ExistingKnockoutProductEntryCalculator
        ),
        KnockoutProductCandidateExistingEntryCalculationGate(),
        KnockoutProductCandidateTargetSelectionApplicationService(
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
    )

    @Test
    fun emptyCandidatesReturnNoInputCandidates() {
        assertSame(
            KnockoutProductCandidatePlannedEntrySelectionApplicationResult.NoInputCandidates,
            service.execute(request(emptyList()))
        )
    }

    @Test
    fun oneSuccessfulCandidateReachesTargetSelection() {
        val input = input("A")
        val result = evaluated(request(listOf(input)))
        val selection = selected(result.targetSelectionResult)

        assertSame(input, originalInput(selection.primaryCandidate))
        assertTrue(selection.alternativeCandidates.isEmpty())
        assertTrue(result.invalidTargetLeveragePlanCandidates.isEmpty())
        assertTrue(result.existingEntryFailedCandidates.isEmpty())
    }

    @Test
    fun allInvalidTargetLeveragePlansReturnTypedResult() {
        val first = input("A", ratio = 0.0)
        val second = input("B", ratio = 0.0)
        val result = service.execute(request(listOf(first, second)))

        assertTrue(
            result is KnockoutProductCandidatePlannedEntrySelectionApplicationResult
                .NoValidTargetLeveragePlanCandidates
        )
        result as KnockoutProductCandidatePlannedEntrySelectionApplicationResult
            .NoValidTargetLeveragePlanCandidates
        assertEquals(listOf(first, second), result.invalidCandidates.map { it.input })
    }

    @Test
    fun invalidTargetLeveragePlansAreRetainedWhileValidCandidatesContinue() {
        val valid = input("A")
        val invalid = input("B", ratio = 0.0)
        val result = evaluated(request(listOf(invalid, valid)))

        assertSame(valid, originalInput(selected(result.targetSelectionResult).primaryCandidate))
        assertSame(invalid, result.invalidTargetLeveragePlanCandidates.single().input)
        assertTrue(result.existingEntryFailedCandidates.isEmpty())
    }

    @Test
    fun allExistingEntryFailuresReturnTypedResult() {
        val first = input("A", basePrice = 0.0)
        val second = input("B", basePrice = -1.0)
        val result = service.execute(request(listOf(first, second)))

        assertTrue(
            result is KnockoutProductCandidatePlannedEntrySelectionApplicationResult
                .NoSuccessfulExistingEntryCalculationCandidates
        )
        result as KnockoutProductCandidatePlannedEntrySelectionApplicationResult
            .NoSuccessfulExistingEntryCalculationCandidates
        assertTrue(result.invalidTargetLeveragePlanCandidates.isEmpty())
        assertEquals(listOf(first, second), result.failedCandidates.map(::originalInput))
    }

    @Test
    fun existingEntryFailuresAreRetainedWhileSuccessfulCandidatesContinue() {
        val successful = input("A")
        val failing = input("B", basePrice = 0.0)
        val result = evaluated(request(listOf(failing, successful)))

        assertSame(
            successful,
            originalInput(selected(result.targetSelectionResult).primaryCandidate)
        )
        assertSame(failing, originalInput(result.existingEntryFailedCandidates.single()))
        assertTrue(result.invalidTargetLeveragePlanCandidates.isEmpty())
    }

    @Test
    fun completePathRanksCandidatesAndRetainsUpstreamDiagnosticGroups() {
        val alternative = input("A", basePrice = 75.0)
        val invalidTargetLeverage = input("B", ratio = 0.0)
        val existingEntryFailure = input("C", basePrice = 0.0)
        val primary = input("D")
        val result = evaluated(
            request(
                listOf(alternative, invalidTargetLeverage, existingEntryFailure, primary),
                maxLeverageDeviationPercent = 50.0
            )
        )
        val selection = selected(result.targetSelectionResult)

        assertSame(primary, originalInput(selection.primaryCandidate))
        assertEquals(
            listOf(alternative),
            selection.alternativeCandidates.map(::originalInput)
        )
        assertSame(
            invalidTargetLeverage,
            result.invalidTargetLeveragePlanCandidates.single().input
        )
        assertSame(
            existingEntryFailure,
            originalInput(result.existingEntryFailedCandidates.single())
        )
    }

    @Test
    fun targetSelectionNoMatchResultIsTransportedWithoutFlattening() {
        val nonMatching = input("A", basePrice = 75.0, knockoutBarrier = 70.0)
        val result = evaluated(request(listOf(nonMatching)))

        assertTrue(
            result.targetSelectionResult is KnockoutProductCandidateTargetSelectionApplicationResult
                .NoCandidatesWithinTargetTolerances
        )
        val targetSelectionResult = result.targetSelectionResult as
            KnockoutProductCandidateTargetSelectionApplicationResult
                .NoCandidatesWithinTargetTolerances
        assertSame(
            nonMatching,
            originalInput(targetSelectionResult.nonMatchingCandidates.single())
        )
    }

    @Test
    fun referencesCurrencyConversionDuplicatesAndInputListRemainUnchanged() {
        val conversion = sameCurrency()
        val duplicate = input("A", currencyConversion = conversion)
        val inputs = mutableListOf(duplicate, duplicate)
        val original = inputs.toList()
        val request = request(inputs)
        val result = evaluated(request)
        val selection = selected(result.targetSelectionResult)

        assertSame(inputs, request.candidates)
        assertEquals(original, inputs)
        assertSame(duplicate, originalInput(selection.primaryCandidate))
        assertSame(duplicate, originalInput(selection.alternativeCandidates.single()))
        assertSame(conversion, originalInput(selection.primaryCandidate).currencyConversion)
        assertSame(
            conversion,
            originalInput(selection.alternativeCandidates.single()).currencyConversion
        )
    }

    private fun evaluated(
        request: KnockoutProductCandidatePlannedEntrySelectionApplicationRequest
    ): KnockoutProductCandidatePlannedEntrySelectionApplicationResult.TargetSelectionEvaluated {
        val result = service.execute(request)
        assertTrue(
            result is KnockoutProductCandidatePlannedEntrySelectionApplicationResult
                .TargetSelectionEvaluated
        )
        return result as KnockoutProductCandidatePlannedEntrySelectionApplicationResult
            .TargetSelectionEvaluated
    }

    private fun selected(
        result: KnockoutProductCandidateTargetSelectionApplicationResult
    ): KnockoutProductCandidateTargetSelectionApplicationResult.SelectedCandidates {
        assertTrue(result is KnockoutProductCandidateTargetSelectionApplicationResult.SelectedCandidates)
        return result as KnockoutProductCandidateTargetSelectionApplicationResult.SelectedCandidates
    }

    private fun originalInput(
        candidate: KnockoutProductCandidateWithTargetFit
    ) = candidate
        .candidateWithTargetDeviation
        .candidateWithExistingEntryCalculation
        .candidateWithTargetLeveragePlan
        .input

    private fun originalInput(
        candidate: KnockoutProductCandidateWithExistingEntryCalculation
    ) = candidate.candidateWithTargetLeveragePlan.input

    private fun request(
        candidates: List<KnockoutProductCandidateTargetLeverageInput>,
        underlyingPrice: Double = 100.0,
        plannedEntryPrice: Double = 100.0,
        targetLeverage: Double = 5.0,
        maxLeverageDeviationPercent: Double = 1.0,
        maxBarrierDeviationPercent: Double = 1.0
    ) = KnockoutProductCandidatePlannedEntrySelectionApplicationRequest(
        candidates = candidates,
        underlyingPrice = underlyingPrice,
        plannedEntryPrice = plannedEntryPrice,
        targetLeverage = targetLeverage,
        maxRelativeLeverageDeviationPercent = maxLeverageDeviationPercent,
        maxBarrierDeviationPercentOfPlannedEntry = maxBarrierDeviationPercent
    )

    private fun input(
        isin: String,
        basePrice: Double = 80.0,
        knockoutBarrier: Double = 80.0,
        ratio: Double = 0.1,
        currencyConversion: CurrencyConversion = sameCurrency()
    ): KnockoutProductCandidateTargetLeverageInput {
        val specification = KnockoutProductSpecification(
            productIsin = isin,
            productWkn = "SYN001",
            issuerId = "issuer",
            underlyingId = "underlying",
            direction = TradeDirection.LONG,
            basePrice = basePrice,
            knockoutBarrier = knockoutBarrier,
            ratio = ratio,
            underlyingCurrency = "EUR",
            productCurrency = "EUR"
        )
        val snapshot = KnockoutProductSpecificationSnapshot(
            specification = specification,
            sourceId = "snapshot",
            retrievedAtEpochMillis = 1000L,
            sourceTimestampEpochMillis = 900L
        )
        val marketData = KnockoutProductMarketData(
            productIsin = isin,
            bid = 1.0,
            ask = 1.2,
            bidTimestampEpochMillis = 950L,
            askTimestampEpochMillis = 950L,
            currency = "EUR",
            sourceId = "SYNTH_SOURCE"
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
        val calculationCandidate = KnockoutProductCandidateWithCalculation(
            sourceCandidate,
            KnockoutProductCandidateCalculationOutcome.Success(
                MarketDataCalculationValue.MidPrice(1.0, "EUR")
            )
        )
        return KnockoutProductCandidateTargetLeverageInput(
            calculationCandidate,
            currencyConversion
        )
    }

    private fun sameCurrency() = CurrencyConversion.SameCurrency(currencyCode("EUR"))

    private fun currencyCode(value: String): CurrencyCode =
        (CurrencyCode.create(value) as CurrencyCodeCreationResult.Success).currencyCode
}
