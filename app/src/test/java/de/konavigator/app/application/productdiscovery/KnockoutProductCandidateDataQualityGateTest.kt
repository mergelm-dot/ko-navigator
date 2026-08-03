package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.dataquality.DataQualityAssessment
import de.konavigator.app.domain.dataquality.DataQualityCategory
import de.konavigator.app.domain.dataquality.DataQualityComponent
import de.konavigator.app.domain.dataquality.DataQualityFinding
import de.konavigator.app.domain.dataquality.DataQualityFindingCode
import de.konavigator.app.domain.dataquality.DataQualitySeverity
import de.konavigator.app.domain.dataquality.DataQualityStatus
import de.konavigator.app.domain.model.KnockoutProductMarketData
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot
import de.konavigator.app.domain.model.TradeDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductCandidateDataQualityGateTest {

    private val gate = KnockoutProductCandidateDataQualityGate()

    @Test
    fun passedCandidateIsStructurallyEligible() {
        val candidate = candidate("DE000SYNTH01", DataQualityAssessment.passed())

        val result = eligibleResult(listOf(candidate))

        assertEquals(listOf(candidate), result.eligibleCandidates)
        assertTrue(result.blockedCandidates.isEmpty())
    }

    @Test
    fun warningCandidateIsStructurallyEligible() {
        val finding = warningFinding()
        val assessment = DataQualityAssessment.warning(findings = listOf(finding))
        val candidate = candidate("DE000SYNTH01", assessment)

        val result = eligibleResult(listOf(candidate))

        assertSame(candidate, result.eligibleCandidates.single())
        assertSame(assessment, result.eligibleCandidates.single().dataQualityAssessment)
        assertEquals(listOf(finding), result.eligibleCandidates.single().dataQualityAssessment.findings)
        assertTrue(result.blockedCandidates.isEmpty())
    }

    @Test
    fun blockedCandidateReturnsNoStructurallyEligibleCandidates() {
        val candidate = candidate("DE000SYNTH01", blockedAssessment())

        val result = gate.filter(request(listOf(candidate)))

        assertTrue(result is KnockoutProductCandidateDataQualityGateResult.NoStructurallyEligibleCandidates)
        assertEquals(
            listOf(candidate),
            (result as KnockoutProductCandidateDataQualityGateResult.NoStructurallyEligibleCandidates)
                .blockedCandidates
        )
    }

    @Test
    fun emptyInputReturnsNoInputCandidates() {
        val result = gate.filter(request(emptyList()))

        assertSame(KnockoutProductCandidateDataQualityGateResult.NoInputCandidates, result)
    }

    @Test
    fun passedWarningAndBlockedCandidatesArePartitioned() {
        val passed = candidate("DE000SYNTH01", DataQualityAssessment.passed())
        val warning = candidate("DE000SYNTH02", warningAssessment())
        val blocked = candidate("DE000SYNTH03", blockedAssessment())

        val result = eligibleResult(listOf(passed, warning, blocked))

        assertEquals(listOf(passed, warning), result.eligibleCandidates)
        assertEquals(listOf(blocked), result.blockedCandidates)
        assertEquals(3, result.eligibleCandidates.size + result.blockedCandidates.size)
        assertFalse(result.eligibleCandidates.any { it === blocked })
        assertFalse(result.blockedCandidates.any { it === passed || it === warning })
    }

    @Test
    fun allPassedCandidatesProduceEmptyBlockedList() {
        val first = candidate("DE000SYNTH01", DataQualityAssessment.passed())
        val second = candidate("DE000SYNTH02", DataQualityAssessment.passed())

        val result = eligibleResult(listOf(first, second))

        assertEquals(listOf(first, second), result.eligibleCandidates)
        assertTrue(result.blockedCandidates.isEmpty())
    }

    @Test
    fun allWarningCandidatesRemainEligible() {
        val first = candidate("DE000SYNTH01", warningAssessment())
        val second = candidate("DE000SYNTH02", warningAssessment())

        val result = eligibleResult(listOf(first, second))

        assertEquals(listOf(first, second), result.eligibleCandidates)
        assertTrue(result.blockedCandidates.isEmpty())
        assertTrue(
            result.eligibleCandidates.all {
                it.dataQualityAssessment.status == DataQualityStatus.WARNING
            }
        )
    }

    @Test
    fun allBlockedCandidatesRemainInNoEligibleResult() {
        val first = candidate("DE000SYNTH01", blockedAssessment())
        val second = candidate("DE000SYNTH02", blockedAssessment())

        val result = gate.filter(request(listOf(first, second)))

        assertTrue(result is KnockoutProductCandidateDataQualityGateResult.NoStructurallyEligibleCandidates)
        assertEquals(
            listOf(first, second),
            (result as KnockoutProductCandidateDataQualityGateResult.NoStructurallyEligibleCandidates)
                .blockedCandidates
        )
    }

    @Test
    fun eligibleCandidateOrderIsPreserved() {
        val warningFirst = candidate("DE000SYNTH03", warningAssessment())
        val blocked = candidate("DE000SYNTH01", blockedAssessment())
        val passedSecond = candidate("DE000SYNTH02", DataQualityAssessment.passed())

        val result = eligibleResult(listOf(warningFirst, blocked, passedSecond))

        assertEquals(listOf(warningFirst, passedSecond), result.eligibleCandidates)
    }

    @Test
    fun blockedCandidateOrderIsPreserved() {
        val blockedFirst = candidate("DE000SYNTH03", blockedAssessment())
        val passed = candidate("DE000SYNTH01", DataQualityAssessment.passed())
        val blockedSecond = candidate("DE000SYNTH02", blockedAssessment())

        val result = eligibleResult(listOf(blockedFirst, passed, blockedSecond))

        assertEquals(listOf(blockedFirst, blockedSecond), result.blockedCandidates)
    }

    @Test
    fun duplicateEligibleCandidateRemainsDuplicated() {
        val candidate = candidate("DE000SYNTH01", DataQualityAssessment.passed())

        val result = eligibleResult(listOf(candidate, candidate))

        assertEquals(2, result.eligibleCandidates.size)
        assertSame(candidate, result.eligibleCandidates[0])
        assertSame(candidate, result.eligibleCandidates[1])
    }

    @Test
    fun duplicateBlockedCandidateRemainsDuplicated() {
        val blocked = candidate("DE000SYNTH01", blockedAssessment())

        val result = gate.filter(request(listOf(blocked, blocked)))

        assertTrue(result is KnockoutProductCandidateDataQualityGateResult.NoStructurallyEligibleCandidates)
        val blockedCandidates =
            (result as KnockoutProductCandidateDataQualityGateResult.NoStructurallyEligibleCandidates)
                .blockedCandidates
        assertEquals(2, blockedCandidates.size)
        assertSame(blocked, blockedCandidates[0])
        assertSame(blocked, blockedCandidates[1])
    }

    @Test
    fun equalIsinsRemainSeparateCandidateEntries() {
        val passed = candidate("DE000SYNTH01", DataQualityAssessment.passed())
        val blocked = candidate("DE000SYNTH01", blockedAssessment())

        val result = eligibleResult(listOf(passed, blocked))

        assertSame(passed, result.eligibleCandidates.single())
        assertSame(blocked, result.blockedCandidates.single())
    }

    @Test
    fun differentExactIsinSpellingsRemainSeparate() {
        val upper = candidate("DE000SYNTH01", DataQualityAssessment.passed())
        val lower = candidate("de000synth01", DataQualityAssessment.passed())
        val spaced = candidate(" DE000SYNTH01 ", DataQualityAssessment.passed())

        val result = eligibleResult(listOf(upper, lower, spaced))

        assertEquals(
            listOf("DE000SYNTH01", "de000synth01", " DE000SYNTH01 "),
            result.eligibleCandidates.map {
                it.candidateWithMarketData.specificationSnapshot.specification.productIsin
            }
        )
    }

    @Test
    fun differentProductsFromSameIssuerRemainSeparate() {
        val first = candidate("DE000SYNTH01", DataQualityAssessment.passed(), issuerId = "issuer-a")
        val second = candidate("DE000SYNTH02", DataQualityAssessment.passed(), issuerId = "issuer-a")

        val result = eligibleResult(listOf(first, second))

        assertEquals(listOf(first, second), result.eligibleCandidates)
    }

    @Test
    fun resultIsNotLimitedToThreeCandidates() {
        val candidates = (1..4).map {
            candidate("DE000SYNTH0$it", DataQualityAssessment.passed())
        }

        val result = eligibleResult(candidates)

        assertEquals(candidates, result.eligibleCandidates)
        assertEquals(4, result.eligibleCandidates.size)
    }

    @Test
    fun candidateDomainAndAssessmentInstancesRemainUnchanged() {
        val assessment = warningAssessment()
        val specification = specification("DE000SYNTH01", "issuer-synthetic")
        val snapshot = snapshot(specification)
        val marketData = marketData("DE000SYNTH01")
        val candidateWithMarketData = KnockoutProductCandidateWithMarketData(snapshot, marketData)
        val candidate = KnockoutProductCandidateWithDataQuality(candidateWithMarketData, assessment)

        val result = eligibleResult(listOf(candidate)).eligibleCandidates.single()

        assertSame(candidate, result)
        assertSame(candidateWithMarketData, result.candidateWithMarketData)
        assertSame(snapshot, result.candidateWithMarketData.specificationSnapshot)
        assertSame(specification, result.candidateWithMarketData.specificationSnapshot.specification)
        assertSame(marketData, result.candidateWithMarketData.marketData)
        assertSame(assessment, result.dataQualityAssessment)
    }

    @Test
    fun findingsRemainCompleteOrderedAndUnchanged() {
        val firstFinding = blockingFinding(DataQualityFindingCode.SPECIFICATION_INVALID_BASE_PRICE)
        val secondFinding = blockingFinding(DataQualityFindingCode.MARKET_DATA_INVALID_ASK)
        val assessment = DataQualityAssessment.blocked(listOf(firstFinding, secondFinding))
        val candidate = candidate("DE000SYNTH01", assessment)

        val result = gate.filter(request(listOf(candidate)))

        val gated =
            (result as KnockoutProductCandidateDataQualityGateResult.NoStructurallyEligibleCandidates)
                .blockedCandidates.single()
        assertSame(assessment, gated.dataQualityAssessment)
        assertEquals(listOf(firstFinding, secondFinding), gated.dataQualityAssessment.findings)
        assertEquals(DataQualityFindingCode.SPECIFICATION_INVALID_BASE_PRICE, gated.dataQualityAssessment.findings[0].code)
        assertEquals(DataQualityFindingCode.MARKET_DATA_INVALID_ASK, gated.dataQualityAssessment.findings[1].code)
        assertEquals(DataQualityCategory.INVALID_NUMERIC_VALUE, gated.dataQualityAssessment.findings[0].category)
        assertEquals(DataQualityCategory.INVALID_NUMERIC_VALUE, gated.dataQualityAssessment.findings[1].category)
        assertEquals(DataQualitySeverity.BLOCKING, gated.dataQualityAssessment.findings[0].severity)
        assertEquals(DataQualityComponent.PRODUCT_SPECIFICATION, gated.dataQualityAssessment.findings[0].component)
        assertEquals(DataQualityComponent.PRODUCT_MARKET_DATA, gated.dataQualityAssessment.findings[1].component)
    }

    @Test
    fun gateDoesNotMutateRequestOrInputList() {
        val passed = candidate("DE000SYNTH01", DataQualityAssessment.passed())
        val warning = candidate("DE000SYNTH02", warningAssessment())
        val blocked = candidate("DE000SYNTH03", blockedAssessment())
        val input = mutableListOf(passed, warning, blocked, passed)
        val original = input.toList()
        val request = request(input)

        gate.filter(request)

        assertSame(input, request.candidates)
        assertEquals(original, input)
    }

    @Test
    fun gateContainsNoFreshnessRankingOrCalculationOutput() {
        val candidate = candidate("DE000SYNTH01", DataQualityAssessment.passed())

        val result = eligibleResult(listOf(candidate))

        assertEquals(listOf(candidate), result.eligibleCandidates)
        assertTrue(result.blockedCandidates.isEmpty())
    }

    private fun eligibleResult(
        candidates: List<KnockoutProductCandidateWithDataQuality>
    ): KnockoutProductCandidateDataQualityGateResult.StructurallyEligibleCandidates {
        val result = gate.filter(request(candidates))
        assertTrue(result is KnockoutProductCandidateDataQualityGateResult.StructurallyEligibleCandidates)
        return result as KnockoutProductCandidateDataQualityGateResult.StructurallyEligibleCandidates
    }

    private fun request(
        candidates: List<KnockoutProductCandidateWithDataQuality>
    ): KnockoutProductCandidateDataQualityGateRequest {
        return KnockoutProductCandidateDataQualityGateRequest(candidates)
    }

    private fun candidate(
        productIsin: String,
        assessment: DataQualityAssessment,
        issuerId: String = "issuer-synthetic"
    ): KnockoutProductCandidateWithDataQuality {
        return KnockoutProductCandidateWithDataQuality(
            candidateWithMarketData = KnockoutProductCandidateWithMarketData(
                specificationSnapshot = snapshot(specification(productIsin, issuerId)),
                marketData = marketData(productIsin)
            ),
            dataQualityAssessment = assessment
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

    private fun warningAssessment(): DataQualityAssessment {
        return DataQualityAssessment.warning(findings = listOf(warningFinding()))
    }

    private fun warningFinding(): DataQualityFinding {
        return DataQualityFinding(
            category = DataQualityCategory.MISSING_REQUIRED_DATA,
            severity = DataQualitySeverity.WARNING,
            code = DataQualityFindingCode.MARKET_DATA_MISSING_SOURCE_ID,
            component = DataQualityComponent.PRODUCT_MARKET_DATA
        )
    }

    private fun blockedAssessment(): DataQualityAssessment {
        return DataQualityAssessment.blocked(
            listOf(blockingFinding(DataQualityFindingCode.SPECIFICATION_INVALID_BASE_PRICE))
        )
    }

    private fun blockingFinding(code: DataQualityFindingCode): DataQualityFinding {
        return DataQualityFinding(
            category = DataQualityCategory.INVALID_NUMERIC_VALUE,
            severity = DataQualitySeverity.BLOCKING,
            code = code,
            component = when (code) {
                DataQualityFindingCode.SPECIFICATION_INVALID_BASE_PRICE ->
                    DataQualityComponent.PRODUCT_SPECIFICATION

                else -> DataQualityComponent.PRODUCT_MARKET_DATA
            }
        )
    }
}
