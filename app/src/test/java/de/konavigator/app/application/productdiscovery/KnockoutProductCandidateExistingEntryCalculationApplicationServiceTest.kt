package de.konavigator.app.application.productdiscovery

import de.konavigator.app.calculator.ExistingKnockoutProductEntryCalculationError
import de.konavigator.app.calculator.ExistingKnockoutProductEntryCalculationInput
import de.konavigator.app.calculator.ExistingKnockoutProductEntryCalculationResult
import de.konavigator.app.calculator.ExistingKnockoutProductEntryCalculator
import de.konavigator.app.calculator.TradeCalculationResult
import de.konavigator.app.domain.availability.MarketDataCalculationAvailabilityError
import de.konavigator.app.domain.availability.MarketDataCalculationAvailabilityResult
import de.konavigator.app.domain.currency.CurrencyCode
import de.konavigator.app.domain.currency.CurrencyCodeCreationResult
import de.konavigator.app.domain.currency.CurrencyConversion
import de.konavigator.app.domain.currency.CurrencyConversionCreationResult
import de.konavigator.app.domain.dataquality.DataQualityAssessment
import de.konavigator.app.domain.dataquality.DataQualityCategory
import de.konavigator.app.domain.dataquality.DataQualityComponent
import de.konavigator.app.domain.dataquality.DataQualityFinding
import de.konavigator.app.domain.dataquality.DataQualityFindingCode
import de.konavigator.app.domain.dataquality.DataQualitySeverity
import de.konavigator.app.domain.freshness.MarketDataFreshnessError
import de.konavigator.app.domain.freshness.MarketDataFreshnessResult
import de.konavigator.app.domain.model.KnockoutProductMarketData
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot
import de.konavigator.app.domain.model.TradeDirection
import de.konavigator.app.domain.orchestration.MarketDataCalculationValue
import de.konavigator.app.domain.source.MarketDataSourceError
import de.konavigator.app.domain.source.MarketDataSourceResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductCandidateExistingEntryCalculationApplicationServiceTest {
    private val service = KnockoutProductCandidateExistingEntryCalculationApplicationService(
        ExistingKnockoutProductEntryCalculator
    )

    @Test
    fun longCandidateIsCalculatedWithActualProductSpecification() {
        val candidate = candidate(basePrice = 100.0, knockoutBarrier = 90.0)
        assertEquals(direct(candidate), single(candidate).existingEntryCalculationResult)
    }

    @Test
    fun shortCandidateIsCalculatedWithActualProductSpecification() {
        val candidate = candidate(
            direction = TradeDirection.SHORT,
            basePrice = 100.0,
            knockoutBarrier = 110.0
        )
        assertEquals(direct(candidate, 80.0), single(candidate, 80.0).existingEntryCalculationResult)
    }

    @Test
    fun plannedEntryPriceIsForwardedExactly() {
        val candidate = candidate()
        assertEquals(direct(candidate, 137.25), single(candidate, 137.25).existingEntryCalculationResult)
    }

    @Test
    fun specificationBasePriceIsForwardedExactly() {
        val first = candidate(isin = "A", basePrice = 100.0)
        val second = candidate(isin = "B", basePrice = 40.0)
        val entries = result(request(listOf(first, second))).candidates
        assertEquals(direct(first), entries[0].existingEntryCalculationResult)
        assertEquals(direct(second), entries[1].existingEntryCalculationResult)
    }

    @Test
    fun specificationKnockoutBarrierIsForwardedExactly() {
        val first = candidate(isin = "A", knockoutBarrier = 90.0)
        val second = candidate(isin = "B", knockoutBarrier = 30.0)
        val entries = result(request(listOf(first, second))).candidates
        assertEquals(direct(first), entries[0].existingEntryCalculationResult)
        assertEquals(direct(second), entries[1].existingEntryCalculationResult)
    }

    @Test
    fun specificationDirectionIsForwardedExactly() {
        val long = candidate(isin = "A", direction = TradeDirection.LONG)
        val short = candidate(
            isin = "B",
            direction = TradeDirection.SHORT,
            basePrice = 100.0,
            knockoutBarrier = 110.0
        )
        val entries = result(request(listOf(long, short), 80.0)).candidates
        assertEquals(direct(long, 80.0), entries[0].existingEntryCalculationResult)
        assertEquals(direct(short, 80.0), entries[1].existingEntryCalculationResult)
    }

    @Test
    fun specificationRatioIsForwardedExactly() {
        val candidate = candidate(ratio = 0.25)
        assertEquals(direct(candidate), single(candidate).existingEntryCalculationResult)
    }

    @Test
    fun sameCurrencyConversionIsForwardedUnchanged() {
        val conversion = sameCurrency()
        val candidate = candidate(conversion = conversion)
        val entry = single(candidate)
        assertSame(conversion, entry.candidateWithTargetLeveragePlan.input.currencyConversion)
        assertEquals(direct(candidate), entry.existingEntryCalculationResult)
    }

    @Test
    fun crossCurrencyConversionIsForwardedUnchanged() {
        val conversion = crossCurrency(2.0)
        val candidate = candidate(conversion = conversion)
        val entry = single(candidate)
        assertSame(conversion, entry.candidateWithTargetLeveragePlan.input.currencyConversion)
        assertEquals(direct(candidate), entry.existingEntryCalculationResult)
    }

    @Test
    fun successfulCalculationMatchesDirectCalculatorResult() {
        val candidate = candidate()
        val calculation = single(candidate).existingEntryCalculationResult
        assertTrue(calculation is ExistingKnockoutProductEntryCalculationResult.Success)
        assertEquals(direct(candidate), calculation)
    }

    @Test
    fun invalidBasePricePreservesCalculatorFailure() {
        val calculation = single(candidate(basePrice = 0.0)).existingEntryCalculationResult
        assertFailure(calculation, ExistingKnockoutProductEntryCalculationError.INVALID_BASE_PRICE)
    }

    @Test
    fun invalidKnockoutBarrierPreservesCalculatorFailure() {
        val calculation = single(candidate(knockoutBarrier = 0.0)).existingEntryCalculationResult
        assertFailure(calculation, ExistingKnockoutProductEntryCalculationError.INVALID_KNOCKOUT_BARRIER)
    }

    @Test
    fun candidateAtKnockoutPreservesInvalidKnockoutDistance() {
        val calculation = single(candidate(basePrice = 80.0, knockoutBarrier = 120.0)).existingEntryCalculationResult
        assertFailure(calculation, ExistingKnockoutProductEntryCalculationError.INVALID_KNOCKOUT_DISTANCE)
    }

    @Test
    fun emptyInputReturnsNoInputCandidates() {
        assertSame(
            KnockoutProductCandidateExistingEntryCalculationResult.NoInputCandidates,
            service.execute(request(emptyList()))
        )
    }

    @Test
    fun inputOrderIsPreserved() {
        val first = candidate(isin = "A")
        val second = candidate(isin = "B")
        val third = candidate(isin = "C")
        val entries = result(request(listOf(first, second, third))).candidates
        assertEquals(listOf(first, second, third), entries.map { it.candidateWithTargetLeveragePlan })
    }

    @Test
    fun successfulAndFailedCalculationsRemainTogether() {
        val successful = candidate(isin = "A")
        val failed = candidate(isin = "B", basePrice = 0.0)
        val entries = result(request(listOf(successful, failed))).candidates
        assertEquals(2, entries.size)
        assertSame(successful, entries[0].candidateWithTargetLeveragePlan)
        assertTrue(entries[0].existingEntryCalculationResult is ExistingKnockoutProductEntryCalculationResult.Success)
        assertSame(failed, entries[1].candidateWithTargetLeveragePlan)
        assertFailure(
            entries[1].existingEntryCalculationResult,
            ExistingKnockoutProductEntryCalculationError.INVALID_BASE_PRICE
        )
    }

    @Test
    fun duplicateCandidateRemainsDuplicated() {
        val candidate = candidate()
        val entries = result(request(listOf(candidate, candidate))).candidates
        assertEquals(2, entries.size)
        assertSame(candidate, entries[0].candidateWithTargetLeveragePlan)
        assertSame(candidate, entries[1].candidateWithTargetLeveragePlan)
    }

    @Test
    fun equalIsinsRemainSeparateCandidateEntries() {
        val first = candidate(isin = "DE000SYNTH01")
        val second = candidate(isin = "DE000SYNTH01")
        val entries = result(request(listOf(first, second))).candidates
        assertEquals(2, entries.size)
        assertSame(first, entries[0].candidateWithTargetLeveragePlan)
        assertSame(second, entries[1].candidateWithTargetLeveragePlan)
    }

    @Test
    fun originalCandidateTargetPlanAndPreviousInstancesRemainUnchanged() {
        val firstFinding = finding(DataQualityFindingCode.MARKET_DATA_MISSING_SOURCE_ID)
        val secondFinding = finding(DataQualityFindingCode.MARKET_DATA_MISSING_BID_TIMESTAMP)
        val assessment = DataQualityAssessment.warning(listOf(firstFinding, secondFinding))
        val availability = MarketDataCalculationAvailabilityResult.StructurallyUnavailable(
            listOf(MarketDataCalculationAvailabilityError.MISSING_BID)
        )
        val freshness = MarketDataFreshnessResult.NotFresh(
            listOf(MarketDataFreshnessError.STALE_ASK)
        )
        val source = MarketDataSourceResult.Blocked(MarketDataSourceError.SOURCE_NOT_CONFIGURED)
        val value = MarketDataCalculationValue.MidPrice(1.0, "EUR")
        val outcome = KnockoutProductCandidateCalculationOutcome.Success(value)
        val conversion = sameCurrency()
        val candidate = candidate(
            assessment = assessment,
            availability = availability,
            freshness = freshness,
            source = source,
            outcome = outcome,
            conversion = conversion
        )
        val inputCandidates = mutableListOf(candidate, candidate)
        val originalCandidates = inputCandidates.toList()
        val request = request(inputCandidates)
        val preserved = result(request).candidates.first().candidateWithTargetLeveragePlan
        val targetInput = preserved.input
        val calculation = targetInput.candidateWithCalculation
        val sourceCandidate = calculation.candidateWithSourceEvaluation
        val freshnessCandidate = sourceCandidate.candidateWithFreshness
        val availabilityCandidate = freshnessCandidate.candidateWithCalculationAvailability
        val qualityCandidate = availabilityCandidate.candidateWithDataQuality
        val marketCandidate = qualityCandidate.candidateWithMarketData

        assertSame(inputCandidates, request.candidates)
        assertEquals(originalCandidates, inputCandidates)
        assertSame(candidate, preserved)
        assertSame(targetInput, preserved.input)
        assertSame(candidate.tradeCalculationResult, preserved.tradeCalculationResult)
        assertSame(calculation, targetInput.candidateWithCalculation)
        assertSame(sourceCandidate, calculation.candidateWithSourceEvaluation)
        assertSame(freshnessCandidate, sourceCandidate.candidateWithFreshness)
        assertSame(availabilityCandidate, freshnessCandidate.candidateWithCalculationAvailability)
        assertSame(qualityCandidate, availabilityCandidate.candidateWithDataQuality)
        assertSame(marketCandidate, qualityCandidate.candidateWithMarketData)
        assertSame(marketCandidate.specificationSnapshot, marketCandidate.specificationSnapshot)
        assertSame(marketCandidate.specificationSnapshot.specification, marketCandidate.specificationSnapshot.specification)
        assertSame(marketCandidate.marketData, marketCandidate.marketData)
        assertSame(conversion, targetInput.currencyConversion)
        assertSame(assessment, qualityCandidate.dataQualityAssessment)
        assertEquals(listOf(firstFinding, secondFinding), assessment.findings)
        assertSame(availability, availabilityCandidate.availabilityResult)
        assertEquals(availability.errors, (availabilityCandidate.availabilityResult as MarketDataCalculationAvailabilityResult.StructurallyUnavailable).errors)
        assertSame(freshness, freshnessCandidate.freshnessResult)
        assertEquals(freshness.errors, (freshnessCandidate.freshnessResult as MarketDataFreshnessResult.NotFresh).errors)
        assertSame(source, sourceCandidate.sourceResult)
        assertSame(outcome, calculation.calculationOutcome)
        assertSame(value, (calculation.calculationOutcome as KnockoutProductCandidateCalculationOutcome.Success).value)
        assertEquals(120.0, request.plannedEntryPrice, 0.0)
    }

    @Test
    fun repeatedExecutionUsesActualSpecificationAndContainsNoLaterStageOutput() {
        val targetPlan = TradeCalculationResult(
            isValid = true,
            underlyingPrice = 100.0,
            targetLeverage = 5.0,
            knockoutPrice = 11.0,
            theoreticalValueInUnderlyingCurrency = 999.0,
            theoreticalProductValue = 888.0,
            underlyingExposureInProductCurrency = 10.0,
            calculatedTheoreticalLeverageAtEntry = 5.0,
            underlyingCurrency = currencyCode("EUR"),
            productCurrency = currencyCode("EUR"),
            distanceToKnockoutAbsolute = 89.0,
            distanceToKnockoutPercent = 89.0
        )
        val candidate = candidate(
            basePrice = 100.0,
            knockoutBarrier = 90.0,
            targetPlan = targetPlan
        )
        val request = request(listOf(candidate))
        val first = result(request)
        val second = result(request)

        assertEquals(first, second)
        assertEquals(direct(candidate), first.candidates.single().existingEntryCalculationResult)
        assertEquals(direct(candidate), second.candidates.single().existingEntryCalculationResult)
        assertSame(candidate, first.candidates.single().candidateWithTargetLeveragePlan)
        assertSame(targetPlan, first.candidates.single().candidateWithTargetLeveragePlan.tradeCalculationResult)
        assertTrue(targetPlan.knockoutPrice != specification(candidate).knockoutBarrier)
        assertTrue(targetPlan.theoreticalProductValue != (first.candidates.single().existingEntryCalculationResult as ExistingKnockoutProductEntryCalculationResult.Success).theoreticalProductValue)
    }

    private fun single(
        candidate: KnockoutProductCandidateWithTargetLeveragePlan,
        plannedEntryPrice: Double = 120.0
    ) = result(request(listOf(candidate), plannedEntryPrice)).candidates.single()

    private fun result(
        request: KnockoutProductCandidateExistingEntryCalculationRequest
    ): KnockoutProductCandidateExistingEntryCalculationResult.CandidatesWithExistingEntryCalculation {
        val result = service.execute(request)
        assertTrue(result is KnockoutProductCandidateExistingEntryCalculationResult.CandidatesWithExistingEntryCalculation)
        return result as KnockoutProductCandidateExistingEntryCalculationResult.CandidatesWithExistingEntryCalculation
    }

    private fun direct(
        candidate: KnockoutProductCandidateWithTargetLeveragePlan,
        plannedEntryPrice: Double = 120.0
    ): ExistingKnockoutProductEntryCalculationResult {
        val specification = specification(candidate)
        return ExistingKnockoutProductEntryCalculator.calculate(
            ExistingKnockoutProductEntryCalculationInput(
                plannedEntryPrice = plannedEntryPrice,
                basePrice = specification.basePrice,
                knockoutBarrier = specification.knockoutBarrier,
                direction = specification.direction,
                ratio = specification.ratio,
                currencyConversion = candidate.input.currencyConversion
            )
        )
    }

    private fun assertFailure(
        result: ExistingKnockoutProductEntryCalculationResult,
        error: ExistingKnockoutProductEntryCalculationError
    ) {
        assertTrue(result is ExistingKnockoutProductEntryCalculationResult.Failure)
        assertEquals(error, (result as ExistingKnockoutProductEntryCalculationResult.Failure).error)
    }

    private fun request(
        candidates: List<KnockoutProductCandidateWithTargetLeveragePlan>,
        plannedEntryPrice: Double = 120.0
    ) = KnockoutProductCandidateExistingEntryCalculationRequest(candidates, plannedEntryPrice)

    private fun specification(candidate: KnockoutProductCandidateWithTargetLeveragePlan) = candidate
        .input
        .candidateWithCalculation
        .candidateWithSourceEvaluation
        .candidateWithFreshness
        .candidateWithCalculationAvailability
        .candidateWithDataQuality
        .candidateWithMarketData
        .specificationSnapshot
        .specification

    private fun candidate(
        isin: String = "DE000SYNTH01",
        direction: TradeDirection = TradeDirection.LONG,
        basePrice: Double = 100.0,
        knockoutBarrier: Double = 90.0,
        ratio: Double = 0.1,
        conversion: CurrencyConversion = sameCurrency(),
        assessment: DataQualityAssessment = DataQualityAssessment.passed(),
        availability: MarketDataCalculationAvailabilityResult = MarketDataCalculationAvailabilityResult.StructurallyAvailable,
        freshness: MarketDataFreshnessResult = MarketDataFreshnessResult.Fresh,
        source: MarketDataSourceResult = MarketDataSourceResult.Allowed,
        outcome: KnockoutProductCandidateCalculationOutcome = KnockoutProductCandidateCalculationOutcome.Success(
            MarketDataCalculationValue.MidPrice(1.0, "EUR")
        ),
        targetPlan: TradeCalculationResult = defaultTargetPlan()
    ): KnockoutProductCandidateWithTargetLeveragePlan {
        val specification = KnockoutProductSpecification(
            productIsin = isin,
            productWkn = "SYN001",
            issuerId = "issuer",
            underlyingId = "underlying",
            direction = direction,
            basePrice = basePrice,
            knockoutBarrier = knockoutBarrier,
            ratio = ratio,
            underlyingCurrency = "EUR",
            productCurrency = "EUR"
        )
        val snapshot = KnockoutProductSpecificationSnapshot(specification, "snapshot", 1000L, 900L)
        val marketData = KnockoutProductMarketData(isin, 1.0, 1.2, 950L, 950L, "EUR", "SYNTH_SOURCE")
        val marketCandidate = KnockoutProductCandidateWithMarketData(snapshot, marketData)
        val qualityCandidate = KnockoutProductCandidateWithDataQuality(marketCandidate, assessment)
        val availabilityCandidate = KnockoutProductCandidateWithCalculationAvailability(qualityCandidate, availability)
        val freshnessCandidate = KnockoutProductCandidateWithFreshness(availabilityCandidate, freshness)
        val sourceCandidate = KnockoutProductCandidateWithSourceEvaluation(freshnessCandidate, source)
        val calculationCandidate = KnockoutProductCandidateWithCalculation(sourceCandidate, outcome)
        val targetInput = KnockoutProductCandidateTargetLeverageInput(calculationCandidate, conversion)
        return KnockoutProductCandidateWithTargetLeveragePlan(targetInput, targetPlan)
    }

    private fun defaultTargetPlan() = TradeCalculationResult(
        isValid = true,
        underlyingPrice = 100.0,
        targetLeverage = 5.0,
        knockoutPrice = 80.0,
        theoreticalValueInUnderlyingCurrency = 2.0,
        theoreticalProductValue = 2.0,
        underlyingExposureInProductCurrency = 12.0,
        calculatedTheoreticalLeverageAtEntry = 6.0,
        underlyingCurrency = currencyCode("EUR"),
        productCurrency = currencyCode("EUR"),
        distanceToKnockoutAbsolute = 20.0,
        distanceToKnockoutPercent = 20.0
    )

    private fun sameCurrency() = CurrencyConversion.SameCurrency(currencyCode("EUR"))

    private fun crossCurrency(rate: Double): CurrencyConversion.CrossCurrency =
        (CurrencyConversion.CrossCurrency.create(currencyCode("USD"), currencyCode("EUR"), rate)
            as CurrencyConversionCreationResult.Success).conversion

    private fun currencyCode(value: String): CurrencyCode =
        (CurrencyCode.create(value) as CurrencyCodeCreationResult.Success).currencyCode

    private fun finding(code: DataQualityFindingCode) = DataQualityFinding(
        DataQualityCategory.MISSING_REQUIRED_DATA,
        DataQualitySeverity.WARNING,
        code,
        DataQualityComponent.PRODUCT_MARKET_DATA
    )
}
