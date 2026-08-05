package de.konavigator.app.application.productdiscovery

import de.konavigator.app.calculator.ExistingKnockoutProductEntryCalculationError
import de.konavigator.app.calculator.ExistingKnockoutProductEntryCalculationResult
import de.konavigator.app.calculator.TradeCalculationError
import de.konavigator.app.calculator.TradeCalculationResult
import de.konavigator.app.domain.availability.MarketDataCalculationAvailabilityError
import de.konavigator.app.domain.availability.MarketDataCalculationAvailabilityResult
import de.konavigator.app.domain.calculator.MarketDataCalculationError
import de.konavigator.app.domain.currency.CurrencyCode
import de.konavigator.app.domain.currency.CurrencyCodeCreationResult
import de.konavigator.app.domain.currency.CurrencyConversion
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductCandidateExistingEntryCalculationGateTest {
    private val gate = KnockoutProductCandidateExistingEntryCalculationGate()

    @Test
    fun successfulCandidateIsReleasedForSuitabilityEvaluation() {
        val candidate = candidate(success())
        val result = successfulResult(listOf(candidate))
        assertEquals(listOf(candidate), result.successfulCandidates)
        assertTrue(result.failedCandidates.isEmpty())
    }

    @Test
    fun failedCandidateReturnsNoSuccessfulExistingEntryCalculationCandidates() {
        val calculation = failure(ExistingKnockoutProductEntryCalculationError.INVALID_BASE_PRICE)
        val candidate = candidate(calculation)
        val result = gate.filter(request(listOf(candidate)))
            as KnockoutProductCandidateExistingEntryCalculationGateResult.NoSuccessfulExistingEntryCalculationCandidates
        assertSame(candidate, result.failedCandidates.single())
        assertSame(calculation, result.failedCandidates.single().existingEntryCalculationResult)
    }

    @Test
    fun emptyInputReturnsNoInputCandidates() {
        assertSame(
            KnockoutProductCandidateExistingEntryCalculationGateResult.NoInputCandidates,
            gate.filter(request(emptyList()))
        )
    }

    @Test
    fun successfulAndFailedCandidatesArePartitioned() {
        val first = candidate(success(), isin = "A")
        val second = candidate(failure(ExistingKnockoutProductEntryCalculationError.INVALID_RATIO), isin = "B")
        val third = candidate(success(), isin = "C")
        val result = successfulResult(listOf(first, second, third))
        assertEquals(listOf(first, third), result.successfulCandidates)
        assertEquals(listOf(second), result.failedCandidates)
        assertEquals(3, result.successfulCandidates.size + result.failedCandidates.size)
        assertFalse(result.successfulCandidates.any { it === second })
    }

    @Test
    fun allSuccessfulCandidatesProduceEmptyFailedList() {
        val first = candidate(success())
        val second = candidate(success(), isin = "B")
        val result = successfulResult(listOf(first, second))
        assertEquals(listOf(first, second), result.successfulCandidates)
        assertTrue(result.failedCandidates.isEmpty())
    }

    @Test
    fun allFailedCandidatesRemainInNoSuccessfulResult() {
        val first = candidate(failure(ExistingKnockoutProductEntryCalculationError.INVALID_RATIO))
        val second = candidate(failure(ExistingKnockoutProductEntryCalculationError.INVALID_EXCHANGE_RATE), isin = "B")
        val result = gate.filter(request(listOf(first, second)))
            as KnockoutProductCandidateExistingEntryCalculationGateResult.NoSuccessfulExistingEntryCalculationCandidates
        assertEquals(listOf(first, second), result.failedCandidates)
    }

    @Test
    fun successfulCandidateOrderIsPreserved() {
        val first = candidate(success(), isin = "A")
        val second = candidate(failure(ExistingKnockoutProductEntryCalculationError.INVALID_RATIO), isin = "B")
        val third = candidate(success(), isin = "C")
        assertEquals(listOf(first, third), successfulResult(listOf(first, second, third)).successfulCandidates)
    }

    @Test
    fun failedCandidateOrderIsPreserved() {
        val first = candidate(failure(ExistingKnockoutProductEntryCalculationError.INVALID_RATIO), isin = "A")
        val second = candidate(success(), isin = "B")
        val third = candidate(failure(ExistingKnockoutProductEntryCalculationError.INVALID_EXCHANGE_RATE), isin = "C")
        assertEquals(listOf(first, third), successfulResult(listOf(first, second, third)).failedCandidates)
    }

    @Test
    fun duplicateSuccessfulCandidateRemainsDuplicated() {
        val candidate = candidate(success())
        val result = successfulResult(listOf(candidate, candidate))
        assertEquals(2, result.successfulCandidates.size)
        assertSame(candidate, result.successfulCandidates[0])
        assertSame(candidate, result.successfulCandidates[1])
    }

    @Test
    fun duplicateFailedCandidateRemainsDuplicated() {
        val candidate = candidate(failure(ExistingKnockoutProductEntryCalculationError.INVALID_BASE_PRICE))
        val result = gate.filter(request(listOf(candidate, candidate)))
            as KnockoutProductCandidateExistingEntryCalculationGateResult.NoSuccessfulExistingEntryCalculationCandidates
        assertEquals(2, result.failedCandidates.size)
        assertSame(candidate, result.failedCandidates[0])
        assertSame(candidate, result.failedCandidates[1])
    }

    @Test
    fun equalIsinsRemainSeparateCandidateEntries() {
        val first = candidate(success(), isin = "DE000SYNTH01")
        val second = candidate(failure(ExistingKnockoutProductEntryCalculationError.INVALID_RATIO), isin = "DE000SYNTH01")
        val result = successfulResult(listOf(first, second))
        assertSame(first, result.successfulCandidates.single())
        assertSame(second, result.failedCandidates.single())
    }

    @Test
    fun differentExactIsinSpellingsRemainSeparate() {
        val first = candidate(success(), isin = "DE000SYNTH01")
        val second = candidate(success(), isin = "de000synth01")
        val third = candidate(success(), isin = " DE000SYNTH01 ")
        assertEquals(
            listOf("DE000SYNTH01", "de000synth01", " DE000SYNTH01 "),
            successfulResult(listOf(first, second, third)).successfulCandidates.map(::isin)
        )
    }

    @Test
    fun equalSourceIdsRemainSeparateCandidateEntries() {
        val first = candidate(success(), sourceId = "SYNTH")
        val second = candidate(success(), isin = "B", sourceId = "SYNTH")
        val result = successfulResult(listOf(first, second))
        assertEquals(2, result.successfulCandidates.size)
        assertSame(first, result.successfulCandidates[0])
        assertSame(second, result.successfulCandidates[1])
    }

    @Test
    fun differentProductsFromSameIssuerRemainSeparate() {
        val first = candidate(success(), isin = "A", issuer = "issuer")
        val second = candidate(success(), isin = "B", issuer = "issuer")
        assertEquals(listOf(first, second), successfulResult(listOf(first, second)).successfulCandidates)
    }

    @Test
    fun longAndShortCandidatesRemainSeparateEntries() {
        val long = candidate(success(), direction = TradeDirection.LONG)
        val short = candidate(success(), isin = "B", direction = TradeDirection.SHORT)
        assertEquals(listOf(long, short), successfulResult(listOf(long, short)).successfulCandidates)
    }

    @Test
    fun resultIsNotLimitedToThreeCandidates() {
        val candidates = (1..4).map { candidate(success(), isin = "S$it") }
        assertEquals(candidates, successfulResult(candidates).successfulCandidates)
    }

    @Test
    fun candidateAndPreviousInstancesRemainUnchanged() {
        val calculationResult = success()
        val value = MarketDataCalculationValue.MidPrice(1.0, "EUR")
        val outcome = KnockoutProductCandidateCalculationOutcome.Success(value)
        val conversion = CurrencyConversion.SameCurrency(currencyCode("EUR"))
        val candidate = candidate(calculationResult, outcome = outcome, conversion = conversion)
        val preserved = successfulResult(listOf(candidate)).successfulCandidates.single()
        val targetInput = preserved.candidateWithTargetLeveragePlan.input
        val calculation = targetInput.candidateWithCalculation
        val source = calculation.candidateWithSourceEvaluation
        val freshness = source.candidateWithFreshness
        val availability = freshness.candidateWithCalculationAvailability
        val quality = availability.candidateWithDataQuality
        val market = quality.candidateWithMarketData

        assertSame(candidate, preserved)
        assertSame(calculationResult, preserved.existingEntryCalculationResult)
        assertSame(preserved.candidateWithTargetLeveragePlan, candidate.candidateWithTargetLeveragePlan)
        assertSame(targetInput, preserved.candidateWithTargetLeveragePlan.input)
        assertSame(preserved.candidateWithTargetLeveragePlan.tradeCalculationResult, candidate.candidateWithTargetLeveragePlan.tradeCalculationResult)
        assertSame(calculation, targetInput.candidateWithCalculation)
        assertSame(source, calculation.candidateWithSourceEvaluation)
        assertSame(freshness, source.candidateWithFreshness)
        assertSame(availability, freshness.candidateWithCalculationAvailability)
        assertSame(quality, availability.candidateWithDataQuality)
        assertSame(market, quality.candidateWithMarketData)
        assertSame(market.specificationSnapshot, market.specificationSnapshot)
        assertSame(market.specificationSnapshot.specification, market.specificationSnapshot.specification)
        assertSame(market.marketData, market.marketData)
        assertSame(conversion, targetInput.currencyConversion)
        assertSame(quality.dataQualityAssessment, quality.dataQualityAssessment)
        assertSame(availability.availabilityResult, availability.availabilityResult)
        assertSame(freshness.freshnessResult, freshness.freshnessResult)
        assertSame(source.sourceResult, source.sourceResult)
        assertSame(outcome, calculation.calculationOutcome)
        assertSame(value, (calculation.calculationOutcome as KnockoutProductCandidateCalculationOutcome.Success).value)
    }

    @Test
    fun successFieldsFailuresAndPreviousErrorsRemainUnchanged() {
        val success = success(
            intrinsic = 1.0,
            product = 2.0,
            distanceAbsolute = 3.0,
            distancePercent = 4.0,
            exposure = 5.0,
            leverage = 6.0
        )
        val firstFinding = finding(DataQualityFindingCode.MARKET_DATA_MISSING_SOURCE_ID)
        val secondFinding = finding(DataQualityFindingCode.MARKET_DATA_MISSING_BID_TIMESTAMP)
        val assessment = DataQualityAssessment.warning(listOf(firstFinding, secondFinding))
        val availability = MarketDataCalculationAvailabilityResult.StructurallyUnavailable(
            listOf(MarketDataCalculationAvailabilityError.MISSING_BID, MarketDataCalculationAvailabilityError.MISSING_ASK)
        )
        val freshness = MarketDataFreshnessResult.NotFresh(
            listOf(MarketDataFreshnessError.STALE_BID, MarketDataFreshnessError.STALE_ASK)
        )
        val source = MarketDataSourceResult.Blocked(MarketDataSourceError.SOURCE_NOT_CONFIGURED)
        val outcome = KnockoutProductCandidateCalculationOutcome.Failure(MarketDataCalculationError.INVALID_BID)
        val trade = invalidTargetPlan(TradeCalculationError.INVALID_RATIO)
        val successful = candidate(success)
        val errors = ExistingKnockoutProductEntryCalculationError.entries
        val failed = errors.mapIndexed { index, error ->
            candidate(
                failure(error),
                isin = "F$index",
                assessment = assessment,
                availability = availability,
                freshness = freshness,
                source = source,
                outcome = outcome,
                targetPlan = trade
            )
        }
        val result = successfulResult(listOf(successful) + failed)

        assertSame(success, result.successfulCandidates.single().existingEntryCalculationResult)
        assertEquals(listOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0), successValues(success))
        assertEquals(errors.toList(), result.failedCandidates.map { (it.existingEntryCalculationResult as ExistingKnockoutProductEntryCalculationResult.Failure).error })
        assertEquals(listOf(firstFinding, secondFinding), assessment.findings)
        assertEquals(availability.errors, (failed.first().candidateWithTargetLeveragePlan.input.candidateWithCalculation.candidateWithSourceEvaluation.candidateWithFreshness.candidateWithCalculationAvailability.availabilityResult as MarketDataCalculationAvailabilityResult.StructurallyUnavailable).errors)
        assertEquals(freshness.errors, (failed.first().candidateWithTargetLeveragePlan.input.candidateWithCalculation.candidateWithSourceEvaluation.candidateWithFreshness.freshnessResult as MarketDataFreshnessResult.NotFresh).errors)
        assertSame(source, failed.first().candidateWithTargetLeveragePlan.input.candidateWithCalculation.candidateWithSourceEvaluation.sourceResult)
        assertSame(outcome, failed.first().candidateWithTargetLeveragePlan.input.candidateWithCalculation.calculationOutcome)
        assertSame(trade, failed.first().candidateWithTargetLeveragePlan.tradeCalculationResult)
    }

    @Test
    fun classificationUsesResultSubtypeOnlyAndInputIsNotMutated() {
        val unusualSuccess = success(
            intrinsic = -1.0,
            product = Double.NaN,
            distanceAbsolute = -2.0,
            distancePercent = Double.POSITIVE_INFINITY,
            exposure = -3.0,
            leverage = 0.0
        )
        val successful = candidate(unusualSuccess, isin = "A", sourceId = "source-A")
        val failed = candidate(failure(ExistingKnockoutProductEntryCalculationError.INVALID_CALCULATED_LEVERAGE), isin = "B", sourceId = "source-B")
        val inputs = mutableListOf(successful, failed, successful)
        val original = inputs.toList()
        val request = request(inputs)
        val result = successfulResult(gate.filter(request))

        assertEquals(listOf(successful, successful), result.successfulCandidates)
        assertEquals(listOf(failed), result.failedCandidates)
        assertSame(inputs, request.candidates)
        assertEquals(original, inputs)
        assertEquals(listOf("A", "B", "A"), inputs.map(::isin))
        assertEquals(listOf("source-A", "source-B", "source-A"), inputs.map(::sourceId))
        assertSame(currencyConversion(successful), currencyConversion(result.successfulCandidates.first()))
    }

    @Test
    fun repeatedFilterCallsDoNotShareStateAndContainNoLaterStageOutput() {
        val successful = candidate(success())
        val failed = candidate(failure(ExistingKnockoutProductEntryCalculationError.INVALID_BASE_PRICE), isin = "B")
        val request = request(listOf(successful, failed))
        val first = gate.filter(request)
        val second = gate.filter(request)
        assertEquals(first, second)
        assertSame(successful, successfulResult(first).successfulCandidates.single())
        assertSame(failed, successfulResult(second).failedCandidates.single())
    }

    private fun successfulResult(
        candidates: List<KnockoutProductCandidateWithExistingEntryCalculation>
    ) = successfulResult(gate.filter(request(candidates)))

    private fun successfulResult(
        result: KnockoutProductCandidateExistingEntryCalculationGateResult
    ): KnockoutProductCandidateExistingEntryCalculationGateResult.SuccessfulExistingEntryCalculationCandidates {
        assertTrue(result is KnockoutProductCandidateExistingEntryCalculationGateResult.SuccessfulExistingEntryCalculationCandidates)
        return result as KnockoutProductCandidateExistingEntryCalculationGateResult.SuccessfulExistingEntryCalculationCandidates
    }

    private fun request(candidates: List<KnockoutProductCandidateWithExistingEntryCalculation>) =
        KnockoutProductCandidateExistingEntryCalculationGateRequest(candidates)

    private fun success(
        intrinsic: Double = 2.0,
        product: Double = 2.0,
        distanceAbsolute: Double = 30.0,
        distancePercent: Double = 25.0,
        exposure: Double = 12.0,
        leverage: Double = 6.0
    ) = ExistingKnockoutProductEntryCalculationResult.Success(
        intrinsic, product, distanceAbsolute, distancePercent, exposure, leverage,
        currencyCode("EUR"), currencyCode("EUR")
    )

    private fun failure(error: ExistingKnockoutProductEntryCalculationError) =
        ExistingKnockoutProductEntryCalculationResult.Failure(error)

    private fun candidate(
        calculationResult: ExistingKnockoutProductEntryCalculationResult,
        isin: String = "DE000SYNTH01",
        issuer: String = "issuer",
        sourceId: String = "SYNTH_SOURCE",
        direction: TradeDirection = TradeDirection.LONG,
        conversion: CurrencyConversion = CurrencyConversion.SameCurrency(currencyCode("EUR")),
        assessment: DataQualityAssessment = DataQualityAssessment.passed(),
        availability: MarketDataCalculationAvailabilityResult = MarketDataCalculationAvailabilityResult.StructurallyAvailable,
        freshness: MarketDataFreshnessResult = MarketDataFreshnessResult.Fresh,
        source: MarketDataSourceResult = MarketDataSourceResult.Allowed,
        outcome: KnockoutProductCandidateCalculationOutcome = KnockoutProductCandidateCalculationOutcome.Success(MarketDataCalculationValue.MidPrice(1.0, "EUR")),
        targetPlan: TradeCalculationResult = validTargetPlan()
    ): KnockoutProductCandidateWithExistingEntryCalculation {
        val specification = KnockoutProductSpecification(isin, "SYN001", issuer, "underlying", direction, 100.0, 90.0, 0.1, "EUR", "EUR")
        val snapshot = KnockoutProductSpecificationSnapshot(specification, "snapshot", 1000L, 900L)
        val marketData = KnockoutProductMarketData(isin, 1.0, 1.2, 950L, 950L, "EUR", sourceId)
        val marketCandidate = KnockoutProductCandidateWithMarketData(snapshot, marketData)
        val qualityCandidate = KnockoutProductCandidateWithDataQuality(marketCandidate, assessment)
        val availabilityCandidate = KnockoutProductCandidateWithCalculationAvailability(qualityCandidate, availability)
        val freshnessCandidate = KnockoutProductCandidateWithFreshness(availabilityCandidate, freshness)
        val sourceCandidate = KnockoutProductCandidateWithSourceEvaluation(freshnessCandidate, source)
        val calculationCandidate = KnockoutProductCandidateWithCalculation(sourceCandidate, outcome)
        val input = KnockoutProductCandidateTargetLeverageInput(calculationCandidate, conversion)
        val targetPlanCandidate = KnockoutProductCandidateWithTargetLeveragePlan(input, targetPlan)
        return KnockoutProductCandidateWithExistingEntryCalculation(targetPlanCandidate, calculationResult)
    }

    private fun validTargetPlan() = TradeCalculationResult(
        true, 100.0, 5.0, 80.0, 2.0, 2.0, 12.0, 6.0,
        currencyCode("EUR"), currencyCode("EUR"), 20.0, 20.0, null
    )

    private fun invalidTargetPlan(error: TradeCalculationError) = TradeCalculationResult(
        false, null, null, null, null, null, null, null,
        null, null, null, null, error
    )

    private fun currencyCode(value: String): CurrencyCode =
        (CurrencyCode.create(value) as CurrencyCodeCreationResult.Success).currencyCode

    private fun finding(code: DataQualityFindingCode) = DataQualityFinding(
        DataQualityCategory.MISSING_REQUIRED_DATA,
        DataQualitySeverity.WARNING,
        code,
        DataQualityComponent.PRODUCT_MARKET_DATA
    )

    private fun successValues(result: ExistingKnockoutProductEntryCalculationResult.Success) = listOf(
        result.intrinsicValueInUnderlyingCurrency,
        result.theoreticalProductValue,
        result.knockoutDistanceAbsolute,
        result.knockoutDistancePercent,
        result.underlyingExposureInProductCurrency,
        result.calculatedLeverageAtEntry
    )

    private fun isin(candidate: KnockoutProductCandidateWithExistingEntryCalculation) = candidate
        .candidateWithTargetLeveragePlan.input.candidateWithCalculation.candidateWithSourceEvaluation
        .candidateWithFreshness.candidateWithCalculationAvailability.candidateWithDataQuality
        .candidateWithMarketData.specificationSnapshot.specification.productIsin

    private fun sourceId(candidate: KnockoutProductCandidateWithExistingEntryCalculation) = candidate
        .candidateWithTargetLeveragePlan.input.candidateWithCalculation.candidateWithSourceEvaluation
        .candidateWithFreshness.candidateWithCalculationAvailability.candidateWithDataQuality
        .candidateWithMarketData.marketData.sourceId

    private fun currencyConversion(candidate: KnockoutProductCandidateWithExistingEntryCalculation) =
        candidate.candidateWithTargetLeveragePlan.input.currencyConversion
}
