package de.konavigator.app.composition

import de.konavigator.app.application.tradeplanning.TradePlanningApplicationService
import de.konavigator.app.calculator.TradeCalculationEngine
import de.konavigator.app.presentation.tradeplanner.TradePlannerUiResult
import de.konavigator.app.presentation.tradeplanner.TradePlannerUiSubmission
import de.konavigator.app.presentation.tradeplanner.TradePlannerViewModel
import de.konavigator.app.presentation.tradeplanner.TradePlannerViewModelFactory
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TradePlannerCompositionTest {

    @Test
    fun compositionIsObjectWithExactCreateViewModelFactoryFunction() {
        val type = TradePlannerComposition::class.java
        val instanceField = type.getDeclaredField("INSTANCE")
        val publicMethods = type.declaredMethods
            .filter { Modifier.isPublic(it.modifiers) && !it.isSynthetic }

        assertTrue(Modifier.isStatic(instanceField.modifiers))
        assertSame(TradePlannerComposition, instanceField.get(null))
        assertTrue(
            type.declaredConstructors.single().let { Modifier.isPrivate(it.modifiers) }
        )
        assertEquals(1, publicMethods.size)
        assertEquals("createViewModelFactory", publicMethods.single().name)
        assertEquals(0, publicMethods.single().parameterCount)
        assertEquals(
            7,
            TradePlannerCompositionTest::class.java.declaredMethods
                .count { it.getAnnotation(Test::class.java) != null }
        )
    }

    @Test
    fun compositionReturnsTradePlannerViewModelFactory() {
        val method = TradePlannerComposition::class.java
            .getDeclaredMethod("createViewModelFactory")

        assertEquals(TradePlannerViewModelFactory::class.java, method.returnType)
        assertEquals(
            TradePlannerViewModelFactory::class.java,
            TradePlannerComposition.createViewModelFactory().javaClass
        )
    }

    @Test
    fun factoryCreatesFunctionalTradePlannerViewModel() {
        val viewModel = createViewModel()

        viewModel.onCalculateClicked()

        assertTrue(completedResult(viewModel) is TradePlannerUiResult.Success)
    }

    @Test
    fun generatedServiceUsesExistingTradeCalculationEngineObject() {
        val factory = TradePlannerComposition.createViewModelFactory()
        val service = readField<TradePlanningApplicationService>(
            factory,
            "applicationService"
        )

        assertSame(
            TradeCalculationEngine,
            readField<TradeCalculationEngine>(service, "tradeCalculationEngine")
        )
    }

    @Test
    fun repeatedCallsCreateSeparateFactoriesServicesAndViewModels() {
        val firstFactory = TradePlannerComposition.createViewModelFactory()
        val secondFactory = TradePlannerComposition.createViewModelFactory()
        val firstService = readField<TradePlanningApplicationService>(
            firstFactory,
            "applicationService"
        )
        val secondService = readField<TradePlanningApplicationService>(
            secondFactory,
            "applicationService"
        )
        val firstViewModel = firstFactory.create(TradePlannerViewModel::class.java)
        val secondViewModel = secondFactory.create(TradePlannerViewModel::class.java)

        assertNotSame(firstFactory, secondFactory)
        assertNotSame(firstService, secondService)
        assertNotSame(firstViewModel, secondViewModel)
    }

    @Test
    fun equalInputsProduceDeterministicResultsAcrossObjectGraphs() {
        val first = createViewModel()
        val second = createViewModel()

        first.onCalculateClicked()
        second.onCalculateClicked()

        assertEquals(first.uiState.value, second.uiState.value)
    }

    @Test
    fun compositionHasNoForbiddenDependenciesOrState() {
        val type = TradePlannerComposition::class.java
        val instanceFields = type.declaredFields.filterNot { Modifier.isStatic(it.modifiers) }
        val dependencyTypeNames = buildSet {
            type.declaredFields.forEach { add(it.type.name) }
            type.declaredConstructors.forEach { constructor ->
                constructor.parameterTypes.forEach { add(it.name) }
            }
            type.declaredMethods.forEach { method ->
                add(method.returnType.name)
                method.parameterTypes.forEach { add(it.name) }
            }
        }
        val forbidden = listOf(
            "android.content.Context",
            "android.content.res.Resources",
            "androidx.lifecycle.SavedStateHandle",
            ".repository.",
            ".marketdata.",
            ".debug.",
            "android.view.",
            "android.widget.",
            "androidx.compose.",
            "kotlinx.coroutines.",
            "java.time.",
            "java.util.Date",
            "java.util.Calendar"
        )

        assertTrue(instanceFields.isEmpty())
        assertTrue(
            dependencyTypeNames.none { name -> forbidden.any(name::contains) }
        )
        assertTrue(
            type.declaredMethods.none {
                it.name.contains("time", ignoreCase = true) ||
                    it.name.contains("now", ignoreCase = true)
            }
        )
    }

    private fun createViewModel(): TradePlannerViewModel =
        TradePlannerComposition.createViewModelFactory()
            .create(TradePlannerViewModel::class.java)

    private fun completedResult(viewModel: TradePlannerViewModel): TradePlannerUiResult =
        (viewModel.uiState.value.submission as TradePlannerUiSubmission.Completed).result

    @Suppress("UNCHECKED_CAST")
    private fun <T> readField(instance: Any, fieldName: String): T =
        instance.javaClass.getDeclaredField(fieldName)
            .apply { isAccessible = true }
            .get(instance) as T

}
