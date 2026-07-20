package de.konavigator.app.domain.source

import de.konavigator.app.domain.availability.MarketDataCalculationType
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MarketDataSourcePolicyTest {

    @Test
    fun ruleStoresSourceIdExactly() {
        assertEquals(" source-a ", rule(" source-a ", PURCHASE_PRICE).sourceId)
    }

    @Test
    fun ruleStoresCalculationTypesCompletely() {
        val types = setOf(PURCHASE_PRICE, SALE_PRICE, SPREAD, MID)

        assertEquals(types, rule("source-a", *types.toTypedArray()).supportedCalculationTypes)
    }

    @Test
    fun blankSourceIdIsRejected() {
        listOf("", " ", "\t", "\n").forEach { sourceId ->
            assertConfigurationFailure { rule(sourceId) }
        }
    }

    @Test
    fun emptyCapabilitySetIsAllowed() {
        assertTrue(rule("source-a").supportedCalculationTypes.isEmpty())
    }

    @Test
    fun emptyConfigIsAllowed() {
        assertTrue(config().rules.isEmpty())
    }

    @Test
    fun duplicateExactSourceIdsAreRejected() {
        assertConfigurationFailure {
            config(rule("source-a", PURCHASE_PRICE), rule("source-a", SALE_PRICE))
        }
    }

    @Test
    fun differentlyCasedSourceIdsAreNotDuplicates() {
        val config = config(rule("source-a"), rule("SOURCE-A"))

        assertEquals(2, config.rules.size)
    }

    @Test
    fun differentlySpacedSourceIdsAreNotDuplicates() {
        val config = config(rule("source-a"), rule(" source-a "))

        assertEquals(2, config.rules.size)
    }

    @Test
    fun ruleAndConfigDoNotNormalizeSourceIds() {
        val configured = config(rule(" Source-A "))

        assertEquals(" Source-A ", configured.rules.single().sourceId)
    }

    @Test
    fun ruleAndConfigHaveNoDefaultValues() {
        assertEquals(1, MarketDataSourceRule::class.java.constructors.size)
        assertEquals(2, MarketDataSourceRule::class.java.constructors.single().parameterCount)
        assertEquals(1, MarketDataSourcePolicyConfig::class.java.constructors.size)
        assertEquals(1, MarketDataSourcePolicyConfig::class.java.constructors.single().parameterCount)
    }

    @Test
    fun errorEnumContainsExactlyTwoCodes() {
        assertEquals(listOf(SOURCE_NOT_CONFIGURED, TYPE_NOT_SUPPORTED), MarketDataSourceError.entries)
    }

    @Test
    fun resultContainsExactlyAllowedAndBlocked() {
        assertEquals(
            setOf("Allowed", "Blocked"),
            MarketDataSourceResult::class.java.declaredClasses.map { it.simpleName }.toSet()
        )
    }

    @Test
    fun blockedContainsExactlyOneError() {
        val fields = instanceFields(MarketDataSourceResult.Blocked::class.java)

        assertEquals(listOf("error"), fields.map { it.name })
        assertEquals(MarketDataSourceError::class.java, fields.single().type)
    }

    @Test
    fun blockedContainsNoErrorList() {
        assertTrue(instanceFields(MarketDataSourceResult.Blocked::class.java).none { it.type == List::class.java })
    }

    @Test
    fun sourceTypesContainNoMessagesOrUiTexts() {
        val fieldNames = sourceDomainTypes().flatMap { type ->
            type.declaredFields.map { it.name.lowercase() }
        }

        assertTrue(fieldNames.none { it.contains("message") || it.contains("text") })
    }

    @Test
    fun policyExposesExactlyOnePublicEvaluateFunction() {
        val methods = MarketDataSourcePolicy::class.java.declaredMethods
            .filter { Modifier.isPublic(it.modifiers) }

        assertEquals(listOf("evaluate"), methods.map { it.name })
    }

    @Test
    fun publicApiContainsNoAndroidOrComposeTypes() {
        assertTrue(policyApiTypeNames().none { it.startsWith("android.") || it.contains("compose") })
    }

    @Test
    fun policyApiContainsNoMarketDataModelOrProductSpecification() {
        assertTrue(
            policyApiTypeNames().none {
                it.contains("KnockoutProductMarketData") ||
                    it.contains("KnockoutProductSpecification")
            }
        )
    }

    @Test
    fun emptyConfigBlocksPurchasePriceAsNotConfigured() {
        assertBlocked(policy(), PURCHASE_PRICE, "source-a", SOURCE_NOT_CONFIGURED)
    }

    @Test
    fun emptyConfigBlocksSalePriceAsNotConfigured() {
        assertBlocked(policy(), SALE_PRICE, "source-a", SOURCE_NOT_CONFIGURED)
    }

    @Test
    fun emptyConfigBlocksSpreadAsNotConfigured() {
        assertBlocked(policy(), SPREAD, "source-a", SOURCE_NOT_CONFIGURED)
    }

    @Test
    fun emptyConfigBlocksMidAsNotConfigured() {
        assertBlocked(policy(), MID, "source-a", SOURCE_NOT_CONFIGURED)
    }

    @Test
    fun unknownSourceIsBlocked() {
        val policy = policy(rule("source-a", PURCHASE_PRICE))

        assertBlocked(policy, PURCHASE_PRICE, "source-b", SOURCE_NOT_CONFIGURED)
    }

    @Test
    fun unknownSourceReturnsOnlySourceNotConfigured() {
        val result = evaluate(policy(rule("source-a", PURCHASE_PRICE)), PURCHASE_PRICE, "source-b")

        assertEquals(SOURCE_NOT_CONFIGURED, blockedError(result))
    }

    @Test
    fun similarlyNamedSourceHasNoDefaultEligibility() {
        val policy = policy(rule("source-a", PURCHASE_PRICE))

        assertBlocked(policy, PURCHASE_PRICE, "source-a-default", SOURCE_NOT_CONFIGURED)
    }

    @Test
    fun wildcardRuleDoesNotActAsFallback() {
        val policy = policy(rule("*", PURCHASE_PRICE))

        assertBlocked(policy, PURCHASE_PRICE, "source-a", SOURCE_NOT_CONFIGURED)
    }

    @Test
    fun explicitlySupportedPurchasePriceIsAllowed() {
        assertAllowed(policy(rule("source-a", PURCHASE_PRICE)), PURCHASE_PRICE)
    }

    @Test
    fun explicitlySupportedSalePriceIsAllowed() {
        assertAllowed(policy(rule("source-a", SALE_PRICE)), SALE_PRICE)
    }

    @Test
    fun explicitlySupportedSpreadIsAllowed() {
        assertAllowed(policy(rule("source-a", SPREAD)), SPREAD)
    }

    @Test
    fun explicitlySupportedMidIsAllowed() {
        assertAllowed(policy(rule("source-a", MID)), MID)
    }

    @Test
    fun unsupportedPurchasePriceIsBlocked() {
        assertBlocked(policy(rule("source-a", SALE_PRICE)), PURCHASE_PRICE, error = TYPE_NOT_SUPPORTED)
    }

    @Test
    fun unsupportedSalePriceIsBlocked() {
        assertBlocked(policy(rule("source-a", PURCHASE_PRICE)), SALE_PRICE, error = TYPE_NOT_SUPPORTED)
    }

    @Test
    fun unsupportedSpreadIsBlocked() {
        assertBlocked(policy(rule("source-a", MID)), SPREAD, error = TYPE_NOT_SUPPORTED)
    }

    @Test
    fun unsupportedMidIsBlocked() {
        assertBlocked(policy(rule("source-a", SPREAD)), MID, error = TYPE_NOT_SUPPORTED)
    }

    @Test
    fun unsupportedConfiguredTypeReturnsOnlyCalculationTypeNotSupported() {
        val result = evaluate(policy(rule("source-a", PURCHASE_PRICE)), SALE_PRICE)

        assertEquals(TYPE_NOT_SUPPORTED, blockedError(result))
    }

    @Test
    fun emptyCapabilitySetBlocksEveryTypeAsUnsupported() {
        val policy = policy(rule("source-a"))

        MarketDataCalculationType.entries.forEach { type ->
            assertBlocked(policy, type, error = TYPE_NOT_SUPPORTED)
        }
    }

    @Test
    fun purchaseAndSaleDoNotImplySpread() {
        val policy = policy(rule("source-a", PURCHASE_PRICE, SALE_PRICE))

        assertBlocked(policy, SPREAD, error = TYPE_NOT_SUPPORTED)
    }

    @Test
    fun purchaseAndSaleDoNotImplyMid() {
        val policy = policy(rule("source-a", PURCHASE_PRICE, SALE_PRICE))

        assertBlocked(policy, MID, error = TYPE_NOT_SUPPORTED)
    }

    @Test
    fun spreadDoesNotImplyMid() {
        assertBlocked(policy(rule("source-a", SPREAD)), MID, error = TYPE_NOT_SUPPORTED)
    }

    @Test
    fun midDoesNotImplySpread() {
        assertBlocked(policy(rule("source-a", MID)), SPREAD, error = TYPE_NOT_SUPPORTED)
    }

    @Test
    fun onlyExplicitMembershipControlsEveryResult() {
        val policy = policy(rule("source-a", PURCHASE_PRICE, MID))

        assertAllowed(policy, PURCHASE_PRICE)
        assertBlocked(policy, SALE_PRICE, error = TYPE_NOT_SUPPORTED)
        assertBlocked(policy, SPREAD, error = TYPE_NOT_SUPPORTED)
        assertAllowed(policy, MID)
    }

    @Test
    fun sourceIdComparisonIsCaseSensitive() {
        val policy = policy(rule("source-a", PURCHASE_PRICE))

        assertBlocked(policy, PURCHASE_PRICE, "SOURCE-A", SOURCE_NOT_CONFIGURED)
    }

    @Test
    fun sourceIdComparisonIsWhitespaceSensitive() {
        val policy = policy(rule("source-a", PURCHASE_PRICE))

        assertBlocked(policy, PURCHASE_PRICE, " source-a ", SOURCE_NOT_CONFIGURED)
    }

    @Test
    fun leadingWhitespaceIsNotTrimmed() {
        val policy = policy(rule(" source-a", PURCHASE_PRICE))

        assertAllowed(policy, PURCHASE_PRICE, " source-a")
        assertBlocked(policy, PURCHASE_PRICE, "source-a", SOURCE_NOT_CONFIGURED)
    }

    @Test
    fun trailingWhitespaceIsNotTrimmed() {
        val policy = policy(rule("source-a ", PURCHASE_PRICE))

        assertAllowed(policy, PURCHASE_PRICE, "source-a ")
        assertBlocked(policy, PURCHASE_PRICE, "source-a", SOURCE_NOT_CONFIGURED)
    }

    @Test
    fun lowercaseSourceIsNotConvertedToUppercase() {
        val policy = policy(rule("source-a", PURCHASE_PRICE))

        assertAllowed(policy, PURCHASE_PRICE, "source-a")
        assertBlocked(policy, PURCHASE_PRICE, "SOURCE-A", SOURCE_NOT_CONFIGURED)
    }

    @Test
    fun sourceAliasIsNotResolved() {
        val policy = policy(rule("source-a", PURCHASE_PRICE))

        assertBlocked(policy, PURCHASE_PRICE, "source-a-alias", SOURCE_NOT_CONFIGURED)
    }

    @Test
    fun laterMutationOfOriginalRuleListDoesNotAffectPolicy() {
        val mutableRules = mutableListOf(rule("source-a", PURCHASE_PRICE))
        val policy = MarketDataSourcePolicy(MarketDataSourcePolicyConfig(mutableRules))

        mutableRules += rule("source-b", PURCHASE_PRICE)

        assertBlocked(policy, PURCHASE_PRICE, "source-b", SOURCE_NOT_CONFIGURED)
    }

    @Test
    fun laterMutationOfOriginalCapabilitySetDoesNotAffectPolicy() {
        val mutableTypes = mutableSetOf(PURCHASE_PRICE)
        val policy = policy(MarketDataSourceRule("source-a", mutableTypes))

        mutableTypes += SALE_PRICE

        assertBlocked(policy, SALE_PRICE, error = TYPE_NOT_SUPPORTED)
    }

    @Test
    fun policyResultsRemainStableAfterExternalCollectionMutations() {
        val mutableTypes = mutableSetOf(PURCHASE_PRICE)
        val mutableRules = mutableListOf(MarketDataSourceRule("source-a", mutableTypes))
        val policy = MarketDataSourcePolicy(MarketDataSourcePolicyConfig(mutableRules))

        mutableTypes.clear()
        mutableRules.clear()

        assertAllowed(policy, PURCHASE_PRICE)
    }

    @Test
    fun policyDoesNotModifyConfigOrRules() {
        val configuredRule = rule("source-a", PURCHASE_PRICE)
        val configured = config(configuredRule)
        val originalRule = configuredRule.copy()
        val originalConfig = configured.copy()

        policy(configuredRule).evaluate(PURCHASE_PRICE, "source-a")

        assertEquals(originalRule, configuredRule)
        assertEquals(originalConfig, configured)
    }

    @Test
    fun policyApiHasNoValidatorDependency() {
        assertTrue(policyApiTypeNames().none { it.contains("Validator") || it.contains("Validation") })
    }

    @Test
    fun policyApiHasNoAvailabilityEvaluatorDependency() {
        assertTrue(policyApiTypeNames().none { it.contains("AvailabilityEvaluator") })
    }

    @Test
    fun policyApiHasNoFreshnessDependency() {
        assertTrue(policyApiTypeNames().none { it.contains("Freshness") })
    }

    @Test
    fun policyApiHasNoCalculatorDependency() {
        assertTrue(policyApiTypeNames().none { it.contains("Calculator") })
    }

    @Test
    fun policyApiContainsNoPriceInput() {
        assertTrue(publicEvaluateMethod().parameterTypes.none { it == java.lang.Double.TYPE })
    }

    @Test
    fun policyApiContainsNoTimestampInput() {
        assertTrue(publicEvaluateMethod().parameterTypes.none { it == java.lang.Long.TYPE })
    }

    @Test
    fun policyApiContainsNoIsinOrCurrencyModel() {
        assertEquals(
            listOf(MarketDataCalculationType::class.java, String::class.java),
            publicEvaluateMethod().parameterTypes.toList()
        )
    }

    @Test
    fun issuerIdIsNotPartOfPolicyApi() {
        assertEquals(2, publicEvaluateMethod().parameterCount)
        assertTrue(policyApiTypeNames().none { it.contains("Specification") || it.contains("Issuer") })
    }

    @Test
    fun policyApiContainsNoNetworkTypes() {
        assertTrue(
            policyApiTypeNames().none {
                it.contains("http", ignoreCase = true) ||
                    it.contains("network", ignoreCase = true) ||
                    it.contains("retrofit", ignoreCase = true)
            }
        )
    }

    @Test
    fun sourceDomainContainsNoSourceTypeModel() {
        assertTrue(sourceDomainTypes().none { it.simpleName == "MarketDataSourceType" })
    }

    @Test
    fun sourceDomainContainsNoLatencyModel() {
        assertTrue(sourceDomainTypes().none { it.simpleName.contains("Latency") })
    }

    @Test
    fun sourceDomainContainsNoTrustModel() {
        assertTrue(sourceDomainTypes().none { it.simpleName.contains("Trust") })
    }

    @Test
    fun testAndManualNamesReceiveNoHardcodedEligibility() {
        val policy = policy()

        listOf("test", "manual", "test-source", "manual-source").forEach { sourceId ->
            assertBlocked(policy, PURCHASE_PRICE, sourceId, SOURCE_NOT_CONFIGURED)
        }
    }

    @Test
    fun policyApiHasNoEngineUiOrRepositoryDependency() {
        assertTrue(
            policyApiTypeNames().none {
                it.contains("Engine") || it.contains("Ui") || it.contains("Repository")
            }
        )
    }

    private fun assertAllowed(
        policy: MarketDataSourcePolicy,
        calculationType: MarketDataCalculationType,
        sourceId: String = "source-a"
    ) {
        assertSame(MarketDataSourceResult.Allowed, evaluate(policy, calculationType, sourceId))
    }

    private fun assertBlocked(
        policy: MarketDataSourcePolicy,
        calculationType: MarketDataCalculationType,
        sourceId: String = "source-a",
        error: MarketDataSourceError
    ) {
        assertEquals(error, blockedError(evaluate(policy, calculationType, sourceId)))
    }

    private fun blockedError(result: MarketDataSourceResult): MarketDataSourceError {
        assertTrue(result is MarketDataSourceResult.Blocked)
        return (result as MarketDataSourceResult.Blocked).error
    }

    private fun evaluate(
        policy: MarketDataSourcePolicy,
        calculationType: MarketDataCalculationType,
        sourceId: String = "source-a"
    ): MarketDataSourceResult = policy.evaluate(calculationType, sourceId)

    private fun policy(vararg rules: MarketDataSourceRule) =
        MarketDataSourcePolicy(config(*rules))

    private fun config(vararg rules: MarketDataSourceRule) =
        MarketDataSourcePolicyConfig(rules.toList())

    private fun rule(
        sourceId: String,
        vararg supportedTypes: MarketDataCalculationType
    ) = MarketDataSourceRule(sourceId, supportedTypes.toSet())

    private fun assertConfigurationFailure(block: () -> Unit) {
        try {
            block()
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // Expected technical configuration failure.
        }
    }

    private fun instanceFields(type: Class<*>) =
        type.declaredFields.filterNot { Modifier.isStatic(it.modifiers) }

    private fun sourceDomainTypes() = listOf(
        MarketDataSourceRule::class.java,
        MarketDataSourcePolicyConfig::class.java,
        MarketDataSourceError::class.java,
        MarketDataSourceResult::class.java,
        MarketDataSourceResult.Allowed::class.java,
        MarketDataSourceResult.Blocked::class.java,
        MarketDataSourcePolicy::class.java
    )

    private fun policyApiTypeNames(): List<String> {
        val method = publicEvaluateMethod()
        return method.parameterTypes.map { it.name } + method.returnType.name
    }

    private fun publicEvaluateMethod() = MarketDataSourcePolicy::class.java
        .declaredMethods
        .single { Modifier.isPublic(it.modifiers) && it.name == "evaluate" }

    private companion object {
        val PURCHASE_PRICE = MarketDataCalculationType.PURCHASE_PRICE
        val SALE_PRICE = MarketDataCalculationType.SALE_PRICE
        val SPREAD = MarketDataCalculationType.SPREAD
        val MID = MarketDataCalculationType.MID

        val SOURCE_NOT_CONFIGURED = MarketDataSourceError.SOURCE_NOT_CONFIGURED
        val TYPE_NOT_SUPPORTED = MarketDataSourceError.CALCULATION_TYPE_NOT_SUPPORTED
    }
}
