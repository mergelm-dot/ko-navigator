package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.availability.MarketDataCalculationAvailabilityError
import de.konavigator.app.domain.availability.MarketDataCalculationAvailabilityResult
import de.konavigator.app.domain.dataquality.DataQualityAssessment
import de.konavigator.app.domain.dataquality.DataQualityCategory
import de.konavigator.app.domain.dataquality.DataQualityComponent
import de.konavigator.app.domain.dataquality.DataQualityFinding
import de.konavigator.app.domain.dataquality.DataQualityFindingCode
import de.konavigator.app.domain.dataquality.DataQualitySeverity
import de.konavigator.app.domain.model.KnockoutProductMarketData
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot
import de.konavigator.app.domain.model.TradeDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductCandidateCalculationAvailabilityGateTest {

    private val gate = KnockoutProductCandidateCalculationAvailabilityGate()

    @Test
    fun structurallyAvailableCandidateIsAvailableForFreshness() {
        val candidate = candidate("DE000SYNTH01", available())

        val result = availableResult(listOf(candidate))

        assertEquals(listOf(candidate), result.availableCandidates)
        assertTrue(result.unavailableCandidates.isEmpty())
    }

    @Test
    fun structurallyUnavailableCandidateReturnsNoAvailableCandidates() {
        val availability = unavailable(MarketDataCalculationAvailabilityError.MISSING_ASK)
        val candidate = candidate("DE000SYNTH01", availability)

        val result = gate.filter(request(listOf(candidate)))

        assertTrue(result is KnockoutProductCandidateCalculationAvailabilityGateResult.NoCalculationAvailableCandidates)
        val unavailableCandidates =
            (result as KnockoutProductCandidateCalculationAvailabilityGateResult.NoCalculationAvailableCandidates)
                .unavailableCandidates
        assertSame(candidate, unavailableCandidates.single())
        assertSame(availability, unavailableCandidates.single().availabilityResult)
        assertEquals(
            listOf(MarketDataCalculationAvailabilityError.MISSING_ASK),
            asUnavailable(availability).errors
        )
    }

    @Test
    fun emptyInputReturnsNoInputCandidates() {
        val result = gate.filter(request(emptyList()))

        assertSame(KnockoutProductCandidateCalculationAvailabilityGateResult.NoInputCandidates, result)
    }

    @Test
    fun availableAndUnavailableCandidatesArePartitioned() {
        val firstAvailable = candidate("DE000SYNTH01", available())
        val unavailable = candidate("DE000SYNTH02", unavailable(MarketDataCalculationAvailabilityError.MISSING_BID))
        val secondAvailable = candidate("DE000SYNTH03", available())

        val result = availableResult(listOf(firstAvailable, unavailable, secondAvailable))

        assertEquals(listOf(firstAvailable, secondAvailable), result.availableCandidates)
        assertEquals(listOf(unavailable), result.unavailableCandidates)
        assertEquals(3, result.availableCandidates.size + result.unavailableCandidates.size)
        assertFalse(result.availableCandidates.any { it === unavailable })
        assertFalse(result.unavailableCandidates.any { it === firstAvailable || it === secondAvailable })
    }

    @Test
    fun allAvailableCandidatesProduceEmptyUnavailableList() {
        val first = candidate("DE000SYNTH01", available())
        val second = candidate("DE000SYNTH02", available())

        val result = availableResult(listOf(first, second))

        assertEquals(listOf(first, second), result.availableCandidates)
        assertTrue(result.unavailableCandidates.isEmpty())
    }

    @Test
    fun allUnavailableCandidatesRemainInNoAvailableResult() {
        val first = candidate("DE000SYNTH01", unavailable(MarketDataCalculationAvailabilityError.MISSING_BID))
        val second = candidate("DE000SYNTH02", unavailable(MarketDataCalculationAvailabilityError.MISSING_ASK))

        val result = gate.filter(request(listOf(first, second)))

        assertTrue(result is KnockoutProductCandidateCalculationAvailabilityGateResult.NoCalculationAvailableCandidates)
        assertEquals(
            listOf(first, second),
            (result as KnockoutProductCandidateCalculationAvailabilityGateResult.NoCalculationAvailableCandidates)
                .unavailableCandidates
        )
    }

    @Test
    fun availableCandidateOrderIsPreserved() {
        val secondAvailable = candidate("DE000SYNTH02", available())
        val unavailable = candidate("DE000SYNTH01", unavailable(MarketDataCalculationAvailabilityError.MISSING_BID))
        val firstAvailable = candidate("DE000SYNTH03", available())

        val result = availableResult(listOf(secondAvailable, unavailable, firstAvailable))

        assertEquals(listOf(secondAvailable, firstAvailable), result.availableCandidates)
    }

    @Test
    fun unavailableCandidateOrderIsPreserved() {
        val firstUnavailable = candidate("DE000SYNTH03", unavailable(MarketDataCalculationAvailabilityError.MISSING_BID))
        val available = candidate("DE000SYNTH01", available())
        val secondUnavailable = candidate("DE000SYNTH02", unavailable(MarketDataCalculationAvailabilityError.MISSING_ASK))

        val result = availableResult(listOf(firstUnavailable, available, secondUnavailable))

        assertEquals(listOf(firstUnavailable, secondUnavailable), result.unavailableCandidates)
    }

    @Test
    fun duplicateAvailableCandidateRemainsDuplicated() {
        val candidate = candidate("DE000SYNTH01", available())

        val result = availableResult(listOf(candidate, candidate))

        assertEquals(2, result.availableCandidates.size)
        assertSame(candidate, result.availableCandidates[0])
        assertSame(candidate, result.availableCandidates[1])
    }

    @Test
    fun duplicateUnavailableCandidateRemainsDuplicated() {
        val candidate = candidate("DE000SYNTH01", unavailable(MarketDataCalculationAvailabilityError.MISSING_BID))

        val result = gate.filter(request(listOf(candidate, candidate)))

        assertTrue(result is KnockoutProductCandidateCalculationAvailabilityGateResult.NoCalculationAvailableCandidates)
        val unavailableCandidates =
            (result as KnockoutProductCandidateCalculationAvailabilityGateResult.NoCalculationAvailableCandidates)
                .unavailableCandidates
        assertEquals(2, unavailableCandidates.size)
        assertSame(candidate, unavailableCandidates[0])
        assertSame(candidate, unavailableCandidates[1])
    }

    @Test
    fun equalIsinsRemainSeparateCandidateEntries() {
        val available = candidate("DE000SYNTH01", available())
        val unavailable = candidate("DE000SYNTH01", unavailable(MarketDataCalculationAvailabilityError.MISSING_ASK))

        val result = availableResult(listOf(available, unavailable))

        assertSame(available, result.availableCandidates.single())
        assertSame(unavailable, result.unavailableCandidates.single())
    }

    @Test
    fun differentExactIsinSpellingsRemainSeparate() {
        val upper = candidate("DE000SYNTH01", available())
        val lower = candidate("de000synth01", available())
        val spaced = candidate(" DE000SYNTH01 ", available())

        val result = availableResult(listOf(upper, lower, spaced))

        assertEquals(
            listOf("DE000SYNTH01", "de000synth01", " DE000SYNTH01 "),
            result.availableCandidates.map {
                it.candidateWithDataQuality.candidateWithMarketData.specificationSnapshot.specification.productIsin
            }
        )
    }

    @Test
    fun differentProductsFromSameIssuerRemainSeparate() {
        val first = candidate("DE000SYNTH01", available(), issuerId = "issuer-a")
        val second = candidate("DE000SYNTH02", available(), issuerId = "issuer-a")

        val result = availableResult(listOf(first, second))

        assertEquals(listOf(first, second), result.availableCandidates)
    }

    @Test
    fun resultIsNotLimitedToThreeCandidates() {
        val candidates = (1..4).map { candidate("DE000SYNTH0$it", available()) }

        val result = availableResult(candidates)

        assertEquals(candidates, result.availableCandidates)
        assertEquals(4, result.availableCandidates.size)
    }

    @Test
    fun candidateDomainAssessmentAndAvailabilityInstancesRemainUnchanged() {
        val specification = specification("DE000SYNTH01", "issuer-synthetic")
        val snapshot = snapshot(specification)
        val marketData = marketData("DE000SYNTH01")
        val candidateWithMarketData = KnockoutProductCandidateWithMarketData(snapshot, marketData)
        val assessment = DataQualityAssessment.passed()
        val candidateWithDataQuality = KnockoutProductCandidateWithDataQuality(candidateWithMarketData, assessment)
        val availability = available()
        val candidate = KnockoutProductCandidateWithCalculationAvailability(candidateWithDataQuality, availability)

        val gated = availableResult(listOf(candidate)).availableCandidates.single()

        assertSame(candidate, gated)
        assertSame(candidateWithDataQuality, gated.candidateWithDataQuality)
        assertSame(candidateWithMarketData, gated.candidateWithDataQuality.candidateWithMarketData)
        assertSame(snapshot, gated.candidateWithDataQuality.candidateWithMarketData.specificationSnapshot)
        assertSame(specification, gated.candidateWithDataQuality.candidateWithMarketData.specificationSnapshot.specification)
        assertSame(marketData, gated.candidateWithDataQuality.candidateWithMarketData.marketData)
        assertSame(assessment, gated.candidateWithDataQuality.dataQualityAssessment)
        assertSame(availability, gated.availabilityResult)
    }

    @Test
    fun dataQualityFindingsRemainCompleteOrderedAndUnchanged() {
        val firstFinding = warningFinding(DataQualityFindingCode.MARKET_DATA_MISSING_SOURCE_ID)
        val secondFinding = warningFinding(DataQualityFindingCode.MARKET_DATA_MISSING_BID_TIMESTAMP)
        val assessment = DataQualityAssessment.warning(findings = listOf(firstFinding, secondFinding))
        val candidate = candidate("DE000SYNTH01", available(), assessment = assessment)

        val gated = availableResult(listOf(candidate)).availableCandidates.single()

        assertSame(assessment, gated.candidateWithDataQuality.dataQualityAssessment)
        assertEquals(listOf(firstFinding, secondFinding), gated.candidateWithDataQuality.dataQualityAssessment.findings)
        assertEquals(DataQualityFindingCode.MARKET_DATA_MISSING_SOURCE_ID, gated.candidateWithDataQuality.dataQualityAssessment.findings[0].code)
        assertEquals(DataQualityFindingCode.MARKET_DATA_MISSING_BID_TIMESTAMP, gated.candidateWithDataQuality.dataQualityAssessment.findings[1].code)
    }

    @Test
    fun availabilityErrorsRemainCompleteOrderedAndUnchanged() {
        val availability = unavailable(
            MarketDataCalculationAvailabilityError.MISSING_BID,
            MarketDataCalculationAvailabilityError.MISSING_ASK
        )
        val candidate = candidate("DE000SYNTH01", availability)

        val result = gate.filter(request(listOf(candidate)))
        val gated =
            (result as KnockoutProductCandidateCalculationAvailabilityGateResult.NoCalculationAvailableCandidates)
                .unavailableCandidates.single()

        assertSame(availability, gated.availabilityResult)
        assertEquals(
            listOf(
                MarketDataCalculationAvailabilityError.MISSING_BID,
                MarketDataCalculationAvailabilityError.MISSING_ASK
            ),
            asUnavailable(gated.availabilityResult).errors
        )
    }

    @Test
    fun gateDoesNotMutateRequestOrInputList() {
        val available = candidate("DE000SYNTH01", available())
        val unavailable = candidate("DE000SYNTH02", unavailable(MarketDataCalculationAvailabilityError.MISSING_BID))
        val input = mutableListOf(available, unavailable, available)
        val original = input.toList()
        val request = request(input)

        gate.filter(request)

        assertSame(input, request.candidates)
        assertEquals(original, input)
    }

    @Test
    fun repeatedFilterCallsDoNotShareState() {
        val available = candidate("DE000SYNTH01", available())
        val unavailable = candidate("DE000SYNTH02", unavailable(MarketDataCalculationAvailabilityError.MISSING_ASK))
        val request = request(listOf(available, unavailable))

        val first = gate.filter(request)
        val second = gate.filter(request)

        assertEquals(first, second)
        assertSame(available, availableResult(first).availableCandidates.single())
        assertSame(unavailable, availableResult(second).unavailableCandidates.single())
    }

    @Test
    fun resultContainsNoFreshnessRankingOrCalculationOutput() {
        val candidate = candidate("DE000SYNTH01", available())

        val result = availableResult(listOf(candidate))

        assertEquals(listOf(candidate), result.availableCandidates)
        assertTrue(result.unavailableCandidates.isEmpty())
    }

    private fun availableResult(
        candidates: List<KnockoutProductCandidateWithCalculationAvailability>
    ): KnockoutProductCandidateCalculationAvailabilityGateResult.CalculationAvailableCandidates {
        return availableResult(gate.filter(request(candidates)))
    }

    private fun availableResult(
        result: KnockoutProductCandidateCalculationAvailabilityGateResult
    ): KnockoutProductCandidateCalculationAvailabilityGateResult.CalculationAvailableCandidates {
        assertTrue(result is KnockoutProductCandidateCalculationAvailabilityGateResult.CalculationAvailableCandidates)
        return result as KnockoutProductCandidateCalculationAvailabilityGateResult.CalculationAvailableCandidates
    }

    private fun request(
        candidates: List<KnockoutProductCandidateWithCalculationAvailability>
    ): KnockoutProductCandidateCalculationAvailabilityGateRequest {
        return KnockoutProductCandidateCalculationAvailabilityGateRequest(candidates)
    }

    private fun candidate(
        productIsin: String,
        availabilityResult: MarketDataCalculationAvailabilityResult,
        issuerId: String = "issuer-synthetic",
        assessment: DataQualityAssessment = DataQualityAssessment.passed()
    ): KnockoutProductCandidateWithCalculationAvailability {
        return KnockoutProductCandidateWithCalculationAvailability(
            candidateWithDataQuality = KnockoutProductCandidateWithDataQuality(
                candidateWithMarketData = KnockoutProductCandidateWithMarketData(
                    specificationSnapshot = snapshot(specification(productIsin, issuerId)),
                    marketData = marketData(productIsin)
                ),
                dataQualityAssessment = assessment
            ),
            availabilityResult = availabilityResult
        )
    }

    private fun specification(
        productIsin: String,
        issuerId: String
    ): KnockoutProductSpecification {
        return KnockoutProductSpecification(
            productIsin = productIsin,
            productWkn = "SYN001",
            issuerId = issuerId,
            underlyingId = "underlying-synthetic",
            direction = TradeDirection.LONG,
            basePrice = 100.0,
            knockoutBarrier = 95.0,
            ratio = 0.1,
            underlyingCurrency = "EUR",
            productCurrency = "EUR"
        )
    }

    private fun snapshot(
        specification: KnockoutProductSpecification
    ): KnockoutProductSpecificationSnapshot {
        return KnockoutProductSpecificationSnapshot(
            specification = specification,
            sourceId = "snapshot-synthetic",
            retrievedAtEpochMillis = 1_000L,
            sourceTimestampEpochMillis = 900L
        )
    }

    private fun marketData(productIsin: String): KnockoutProductMarketData {
        return KnockoutProductMarketData(
            productIsin = productIsin,
            bid = 1.0,
            ask = 1.2,
            bidTimestampEpochMillis = 1_100L,
            askTimestampEpochMillis = 1_200L,
            currency = "EUR",
            sourceId = "market-synthetic"
        )
    }

    private fun available(): MarketDataCalculationAvailabilityResult {
        return MarketDataCalculationAvailabilityResult.StructurallyAvailable
    }

    private fun unavailable(
        vararg errors: MarketDataCalculationAvailabilityError
    ): MarketDataCalculationAvailabilityResult.StructurallyUnavailable {
        return MarketDataCalculationAvailabilityResult.StructurallyUnavailable(errors.toList())
    }

    private fun asUnavailable(
        result: MarketDataCalculationAvailabilityResult
    ): MarketDataCalculationAvailabilityResult.StructurallyUnavailable {
        assertTrue(result is MarketDataCalculationAvailabilityResult.StructurallyUnavailable)
        return result as MarketDataCalculationAvailabilityResult.StructurallyUnavailable
    }

    private fun warningFinding(code: DataQualityFindingCode): DataQualityFinding {
        return DataQualityFinding(
            category = DataQualityCategory.INCONSISTENT_TIMESTAMPS,
            severity = DataQualitySeverity.WARNING,
            code = code,
            component = DataQualityComponent.PRODUCT_MARKET_DATA
        )
    }
}
