package de.konavigator.app.presentation.tradeplanner

import androidx.lifecycle.ViewModel
import de.konavigator.app.application.tradeplanning.TradePlanningApplicationService
import de.konavigator.app.calculator.TradeCalculationEngine
import de.konavigator.app.domain.tradeplanning.EntryPriceRelation
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TradePlannerViewModelFactoryTest {

    @Test
    fun constructorHasOnlyTradePlanningApplicationService() {
        val parameterTypes = TradePlannerViewModelFactory::class.java
            .declaredConstructors
            .single()
            .parameterTypes
            .toList()

        assertEquals(listOf(TradePlanningApplicationService::class.java), parameterTypes)
    }

    @Test
    fun factoryCreatesTradePlannerViewModel() {
        val viewModel = factory().create(TradePlannerViewModel::class.java)

        assertEquals(TradePlannerViewModel::class.java, viewModel.javaClass)
    }

    @Test
    fun createdViewModelReceivesExactApplicationService() {
        val applicationService = applicationService()
        val viewModel = TradePlannerViewModelFactory(applicationService)
            .create(TradePlannerViewModel::class.java)

        assertSame(
            applicationService,
            readField<TradePlanningApplicationService>(viewModel, "applicationService")
        )
    }

    @Test
    fun validInputWorksThroughRealServiceAndEngine() {
        val viewModel = factory().create(TradePlannerViewModel::class.java)

        viewModel.onCalculateClicked()

        val result = completedResult(viewModel)
        assertTrue(result is TradePlannerUiResult.Success)
        assertEquals(
            EntryPriceRelation.BELOW_CURRENT,
            (result as TradePlannerUiResult.Success).relation
        )
    }

    @Test
    fun unknownViewModelTypeIsRejectedWithIllegalArgumentException() {
        val exception = try {
            factory().create(UnknownViewModel::class.java)
            null
        } catch (error: IllegalArgumentException) {
            error
        }

        assertNotNull(exception)
        assertTrue(exception?.message.orEmpty().contains(UnknownViewModel::class.java.name))
    }

    @Test
    fun twoCreateCallsProduceIndependentViewModels() {
        val factory = factory()
        val first = factory.create(TradePlannerViewModel::class.java)
        val second = factory.create(TradePlannerViewModel::class.java)

        assertNotSame(first, second)
        first.onCurrentPriceChanged("101")
        assertEquals("100,00", second.uiState.value.currentUnderlyingPriceInput)
    }

    @Test
    fun publicFactoryApiIsLimitedToRequiredCreateMethod() {
        val publicMethods = TradePlannerViewModelFactory::class.java.declaredMethods
            .filter { Modifier.isPublic(it.modifiers) && !it.isSynthetic }
        val classCreateMethods = publicMethods.filter {
            it.name == "create" &&
                it.parameterTypes.contentEquals(arrayOf(Class::class.java))
        }

        assertTrue(publicMethods.all { it.name == "create" })
        assertEquals(1, classCreateMethods.size)
        assertEquals(
            8,
            TradePlannerViewModelFactoryTest::class.java.declaredMethods
                .count { it.getAnnotation(Test::class.java) != null }
        )
    }

    @Test
    fun factoryHasNoForbiddenDependencies() {
        val dependencyTypeNames = buildSet {
            TradePlannerViewModelFactory::class.java.declaredFields.forEach {
                add(it.type.name)
            }
            TradePlannerViewModelFactory::class.java.declaredConstructors.forEach {
                it.parameterTypes.forEach { type -> add(type.name) }
            }
            TradePlannerViewModelFactory::class.java.declaredMethods.forEach { method ->
                add(method.returnType.name)
                method.parameterTypes.forEach { type -> add(type.name) }
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
            "androidx.compose."
        )

        assertTrue(
            dependencyTypeNames.none { name -> forbidden.any(name::contains) }
        )
    }

    private fun factory() = TradePlannerViewModelFactory(applicationService())

    private fun applicationService() =
        TradePlanningApplicationService(TradeCalculationEngine)

    private fun completedResult(viewModel: TradePlannerViewModel): TradePlannerUiResult =
        (viewModel.uiState.value.submission as TradePlannerUiSubmission.Completed).result

    @Suppress("UNCHECKED_CAST")
    private fun <T> readField(instance: Any, fieldName: String): T =
        instance.javaClass.getDeclaredField(fieldName)
            .apply { isAccessible = true }
            .get(instance) as T

    private class UnknownViewModel : ViewModel()
}
