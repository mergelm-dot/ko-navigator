package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.dataquality.DataQualityFindingCode
import de.konavigator.app.domain.dataquality.DataQualityStatus
import de.konavigator.app.domain.dataquality.KnockoutProductDataQualityValidator
import de.konavigator.app.domain.model.KnockoutProductMarketData
import de.konavigator.app.domain.model.KnockoutProductSpecification
import de.konavigator.app.domain.model.KnockoutProductSpecificationSnapshot
import de.konavigator.app.domain.model.TradeDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class KnockoutProductCandidateDataQualityApplicationServiceTest {

    private val service = KnockoutProductCandidateDataQualityApplicationService()

    @Test
    fun validCompatibleCandidateProducesPassedAssessment() {
        val candidate = candidate(productIsin = "SYNTH01")

        val result = successfulResult(listOf(candidate))

        assertEquals(1, result.candidates.size)
        assertEquals(DataQualityStatus.PASSED, result.candidates.single().dataQualityAssessment.status)
        assertTrue(result.candidates.single().dataQualityAssessment.findings.isEmpty())
    }

    @Test
    fun invalidSpecificationProducesBlockedAssessment() {
        val candidate = candidate(
            productIsin = "SYNTH01",
            specification = specification(productIsin = "SYNTH01", basePrice = 0.0)
        )

        val assessment = successfulResult(listOf(candidate))
            .candidates.single().dataQualityAssessment

        assertEquals(DataQualityStatus.BLOCKED, assessment.status)
        assertTrue(
            assessment.findings.any {
                it.code == DataQualityFindingCode.SPECIFICATION_INVALID_BASE_PRICE
            }
        )
    }

    @Test
    fun invalidMarketDataProducesBlockedAssessment() {
        val candidate = candidate(
            productIsin = "SYNTH01",
            marketData = marketData(productIsin = "SYNTH01", ask = 0.0)
        )

        val assessment = successfulResult(listOf(candidate))
            .candidates.single().dataQualityAssessment

        assertEquals(DataQualityStatus.BLOCKED, assessment.status)
        assertTrue(
            assessment.findings.any {
                it.code == DataQualityFindingCode.MARKET_DATA_INVALID_ASK
            }
        )
    }

    @Test
    fun productIsinMismatchProducesBlockedCompatibilityFinding() {
        val candidate = candidate(
            productIsin = "SYNTH01",
            marketData = marketData(productIsin = "SYNTH02")
        )

        val assessment = successfulResult(listOf(candidate))
            .candidates.single().dataQualityAssessment

        assertEquals(DataQualityStatus.BLOCKED, assessment.status)
        assertTrue(
            assessment.findings.any {
                it.code == DataQualityFindingCode.COMPATIBILITY_PRODUCT_ISIN_MISMATCH
            }
        )
    }

    @Test
    fun productCurrencyMismatchProducesBlockedCompatibilityFinding() {
        val candidate = candidate(
            productIsin = "SYNTH01",
            marketData = marketData(productIsin = "SYNTH01", currency = "USD")
        )

        val assessment = successfulResult(listOf(candidate))
            .candidates.single().dataQualityAssessment

        assertEquals(DataQualityStatus.BLOCKED, assessment.status)
        assertTrue(
            assessment.findings.any {
                it.code == DataQualityFindingCode.COMPATIBILITY_PRODUCT_CURRENCY_MISMATCH
            }
        )
    }

    @Test
    fun emptyInputProducesNoInputCandidatesWithoutAssessment() {
        val result = service.execute(
            KnockoutProductCandidateDataQualityRequest(candidates = emptyList())
        )

        assertSame(KnockoutProductCandidateDataQualityResult.NoInputCandidates, result)
        assertFalse(result is KnockoutProductCandidateDataQualityResult.CandidatesWithDataQuality)
    }

    @Test
    fun mixedAssessmentStatusesPreserveInputOrder() {
        val third = candidate(productIsin = "SYNTH03")
        val first = candidate(
            productIsin = "SYNTH01",
            specification = specification(productIsin = "SYNTH01", basePrice = 0.0)
        )
        val second = candidate(productIsin = "SYNTH02")

        val result = successfulResult(listOf(third, first, second))

        assertEquals(listOf(third, first, second), result.candidates.map { it.candidateWithMarketData })
        assertEquals(
            listOf(DataQualityStatus.PASSED, DataQualityStatus.BLOCKED, DataQualityStatus.PASSED),
            result.candidates.map { it.dataQualityAssessment.status }
        )
    }

    @Test
    fun blockedCandidatesAreRetainedBetweenPassedCandidates() {
        val passedBefore = candidate(productIsin = "SYNTH01")
        val blocked = candidate(
            productIsin = "SYNTH02",
            marketData = marketData(productIsin = "SYNTH02", ask = -1.0)
        )
        val passedAfter = candidate(productIsin = "SYNTH03")

        val result = successfulResult(listOf(passedBefore, blocked, passedAfter))

        assertEquals(3, result.candidates.size)
        assertSame(blocked, result.candidates[1].candidateWithMarketData)
        assertEquals(DataQualityStatus.BLOCKED, result.candidates[1].dataQualityAssessment.status)
    }

    @Test
    fun duplicateCandidateInstanceProducesSeparateResultEntries() {
        val candidate = candidate(productIsin = "SYNTH01")

        val result = successfulResult(listOf(candidate, candidate))

        assertEquals(2, result.candidates.size)
        assertSame(candidate, result.candidates[0].candidateWithMarketData)
        assertSame(candidate, result.candidates[1].candidateWithMarketData)
    }

    @Test
    fun differentCandidatesWithSameIsinAreEvaluatedFromTheirOwnModels() {
        val valid = candidate(productIsin = "SYNTH01")
        val invalid = candidate(
            productIsin = "SYNTH01",
            marketData = marketData(productIsin = "SYNTH01", bid = -1.0)
        )

        val result = successfulResult(listOf(valid, invalid))

        assertEquals(2, result.candidates.size)
        assertEquals(DataQualityStatus.PASSED, result.candidates[0].dataQualityAssessment.status)
        assertEquals(DataQualityStatus.BLOCKED, result.candidates[1].dataQualityAssessment.status)
    }

    @Test
    fun identifierSpellingVariantsRemainSeparateAndUnnormalized() {
        val upper = candidate(productIsin = "SYNTH01")
        val lower = candidate(productIsin = "synth01")
        val spaced = candidate(productIsin = " SYNTH01 ")

        val result = successfulResult(listOf(upper, lower, spaced))

        assertEquals(
            listOf("SYNTH01", "synth01", " SYNTH01 "),
            result.candidates.map {
                it.candidateWithMarketData.specificationSnapshot.specification.productIsin
            }
        )
    }

    @Test
    fun differentProductsFromSameIssuerRemainSeparate() {
        val first = candidate(productIsin = "SYNTH01", issuerId = "issuer-a")
        val second = candidate(productIsin = "SYNTH02", issuerId = "issuer-a")

        val result = successfulResult(listOf(first, second))

        assertEquals(2, result.candidates.size)
        assertSame(first, result.candidates[0].candidateWithMarketData)
        assertSame(second, result.candidates[1].candidateWithMarketData)
    }

    @Test
    fun moreThanThreeCandidatesAreNotLimited() {
        val candidates = (1..4).map { candidate(productIsin = "SYNTH0$it") }

        val result = successfulResult(candidates)

        assertEquals(4, result.candidates.size)
        assertEquals(candidates, result.candidates.map { it.candidateWithMarketData })
    }

    @Test
    fun originalCandidateAndNestedObjectInstancesArePreserved() {
        val specification = specification(productIsin = "SYNTH01")
        val snapshot = snapshot(specification = specification)
        val marketData = marketData(productIsin = "SYNTH01")
        val candidate = KnockoutProductCandidateWithMarketData(snapshot, marketData)

        val evaluated = successfulResult(listOf(candidate)).candidates.single()

        assertSame(candidate, evaluated.candidateWithMarketData)
        assertSame(snapshot, evaluated.candidateWithMarketData.specificationSnapshot)
        assertSame(specification, evaluated.candidateWithMarketData.specificationSnapshot.specification)
        assertSame(marketData, evaluated.candidateWithMarketData.marketData)
    }

    @Test
    fun sourceIdentifiersAndTimestampsRemainUnchanged() {
        val snapshot = snapshot(
            specification = specification(productIsin = "SYNTH01"),
            sourceId = " synthetic snapshot source ",
            retrievedAtEpochMillis = 123L,
            sourceTimestampEpochMillis = 456L
        )
        val marketData = marketData(
            productIsin = "SYNTH01",
            sourceId = " synthetic market source ",
            bidTimestampEpochMillis = 789L,
            askTimestampEpochMillis = 987L
        )
        val candidate = KnockoutProductCandidateWithMarketData(snapshot, marketData)

        val evaluated = successfulResult(listOf(candidate)).candidates.single().candidateWithMarketData

        assertEquals(" synthetic snapshot source ", evaluated.specificationSnapshot.sourceId)
        assertEquals(123L, evaluated.specificationSnapshot.retrievedAtEpochMillis)
        assertEquals(456L, evaluated.specificationSnapshot.sourceTimestampEpochMillis)
        assertEquals(" synthetic market source ", evaluated.marketData.sourceId)
        assertEquals(789L, evaluated.marketData.bidTimestampEpochMillis)
        assertEquals(987L, evaluated.marketData.askTimestampEpochMillis)
    }

    @Test
    fun nullableQuotesAndTimestampsRemainNullWithoutReplacement() {
        val snapshot = snapshot(
            specification = specification(productIsin = "SYNTH01"),
            sourceTimestampEpochMillis = null
        )
        val marketData = marketData(
            productIsin = "SYNTH01",
            bid = null,
            ask = null,
            bidTimestampEpochMillis = null,
            askTimestampEpochMillis = null
        )
        val candidate = KnockoutProductCandidateWithMarketData(snapshot, marketData)

        val evaluated = successfulResult(listOf(candidate)).candidates.single().candidateWithMarketData

        assertSame(candidate, evaluated)
        assertEquals(null, evaluated.specificationSnapshot.sourceTimestampEpochMillis)
        assertEquals(null, evaluated.marketData.bid)
        assertEquals(null, evaluated.marketData.ask)
        assertEquals(null, evaluated.marketData.bidTimestampEpochMillis)
        assertEquals(null, evaluated.marketData.askTimestampEpochMillis)
    }

    @Test
    fun serviceReturnsExactlyTheDirectValidatorAssessment() {
        val candidate = candidate(
            productIsin = "SYNTH01",
            specification = specification(productIsin = "SYNTH01", basePrice = 0.0),
            marketData = marketData(productIsin = "SYNTH01", ask = -1.0)
        )
        val directAssessment = KnockoutProductDataQualityValidator.assess(
            candidate.specificationSnapshot.specification,
            candidate.marketData
        )

        val serviceAssessment = successfulResult(listOf(candidate))
            .candidates.single().dataQualityAssessment

        assertEquals(directAssessment, serviceAssessment)
        assertEquals(directAssessment.findings, serviceAssessment.findings)
    }

    @Test
    fun requestAndMutableInputListRemainUnchanged() {
        val first = candidate(productIsin = "SYNTH01")
        val second = candidate(productIsin = "SYNTH02")
        val mutableCandidates = mutableListOf(first, second, first)
        val original = mutableCandidates.toList()
        val request = KnockoutProductCandidateDataQualityRequest(mutableCandidates)

        service.execute(request)

        assertSame(mutableCandidates, request.candidates)
        assertEquals(original, mutableCandidates)
    }

    @Test
    fun repeatedExecutionIsDeterministicAndDoesNotCacheOrMutate() {
        val candidate = candidate(productIsin = "SYNTH01")
        val request = KnockoutProductCandidateDataQualityRequest(listOf(candidate))

        val first = service.execute(request)
        val second = service.execute(request)

        assertEquals(first, second)
        assertSame(candidate, successful(first).candidates.single().candidateWithMarketData)
        assertSame(candidate, successful(second).candidates.single().candidateWithMarketData)
    }

    @Test
    fun publicResultCarriesOnlyCandidateAndStructuralAssessment() {
        val candidate = candidate(productIsin = "SYNTH01")

        val evaluated = successfulResult(listOf(candidate)).candidates.single()

        assertSame(candidate, evaluated.candidateWithMarketData)
        assertEquals(
            KnockoutProductDataQualityValidator.assess(
                candidate.specificationSnapshot.specification,
                candidate.marketData
            ),
            evaluated.dataQualityAssessment
        )
    }

    private fun successfulResult(
        candidates: List<KnockoutProductCandidateWithMarketData>
    ): KnockoutProductCandidateDataQualityResult.CandidatesWithDataQuality {
        return successful(
            service.execute(KnockoutProductCandidateDataQualityRequest(candidates))
        )
    }

    private fun successful(
        result: KnockoutProductCandidateDataQualityResult
    ): KnockoutProductCandidateDataQualityResult.CandidatesWithDataQuality {
        assertTrue(result is KnockoutProductCandidateDataQualityResult.CandidatesWithDataQuality)
        return result as KnockoutProductCandidateDataQualityResult.CandidatesWithDataQuality
    }

    private fun candidate(
        productIsin: String,
        issuerId: String = "issuer-synthetic",
        specification: KnockoutProductSpecification = specification(
            productIsin = productIsin,
            issuerId = issuerId
        ),
        marketData: KnockoutProductMarketData = marketData(productIsin = productIsin)
    ): KnockoutProductCandidateWithMarketData {
        return KnockoutProductCandidateWithMarketData(
            specificationSnapshot = snapshot(specification = specification),
            marketData = marketData
        )
    }

    private fun specification(
        productIsin: String,
        issuerId: String = "issuer-synthetic",
        basePrice: Double = 100.0
    ): KnockoutProductSpecification {
        return KnockoutProductSpecification(
            productIsin = productIsin,
            productWkn = "SYN001",
            issuerId = issuerId,
            underlyingId = "underlying-synthetic",
            direction = TradeDirection.LONG,
            basePrice = basePrice,
            knockoutBarrier = 95.0,
            ratio = 0.1,
            underlyingCurrency = "EUR",
            productCurrency = "EUR"
        )
    }

    private fun snapshot(
        specification: KnockoutProductSpecification,
        sourceId: String = "snapshot-synthetic",
        retrievedAtEpochMillis: Long = 1_000L,
        sourceTimestampEpochMillis: Long? = 900L
    ): KnockoutProductSpecificationSnapshot {
        return KnockoutProductSpecificationSnapshot(
            specification = specification,
            sourceId = sourceId,
            retrievedAtEpochMillis = retrievedAtEpochMillis,
            sourceTimestampEpochMillis = sourceTimestampEpochMillis
        )
    }

    private fun marketData(
        productIsin: String,
        bid: Double? = 1.0,
        ask: Double? = 1.2,
        bidTimestampEpochMillis: Long? = 1_100L,
        askTimestampEpochMillis: Long? = 1_200L,
        currency: String = "EUR",
        sourceId: String = "market-synthetic"
    ): KnockoutProductMarketData {
        return KnockoutProductMarketData(
            productIsin = productIsin,
            bid = bid,
            ask = ask,
            bidTimestampEpochMillis = bidTimestampEpochMillis,
            askTimestampEpochMillis = askTimestampEpochMillis,
            currency = currency,
            sourceId = sourceId
        )
    }
}
