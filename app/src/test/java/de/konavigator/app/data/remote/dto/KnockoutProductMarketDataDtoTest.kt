package de.konavigator.app.data.remote.dto

import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class KnockoutProductMarketDataDtoTest {

    @Test
    fun dtoContainsExactlySevenNullableDataFields() {
        val fields = KnockoutProductMarketDataDto::class.java.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) || it.isSynthetic }

        assertEquals(
            listOf(
                "productIsin",
                "bid",
                "ask",
                "bidTimestampEpochMillis",
                "askTimestampEpochMillis",
                "currency",
                "sourceId"
            ),
            fields.map { it.name }
        )
        assertEquals(
            listOf(
                String::class.java,
                java.lang.Double::class.java,
                java.lang.Double::class.java,
                java.lang.Long::class.java,
                java.lang.Long::class.java,
                String::class.java,
                String::class.java
            ),
            fields.map { it.type }
        )
    }

    @Test
    fun dtoHasNoDefaultsOrForbiddenLayerDependencies() {
        val type = KnockoutProductMarketDataDto::class.java
        val constructors = type.declaredConstructors.filterNot { it.isSynthetic }
        val typeNames = buildSet {
            add(type.name)
            type.declaredFields.forEach { add(it.type.name) }
            constructors.forEach { constructor ->
                constructor.parameterTypes.forEach { add(it.name) }
            }
            type.declaredMethods.forEach { method ->
                add(method.returnType.name)
                method.parameterTypes.forEach { add(it.name) }
            }
        }

        assertEquals(1, constructors.size)
        assertEquals(7, constructors.single().parameterCount)
        assertFalse(
            typeNames.any {
                it.startsWith("android.") ||
                    it.startsWith("androidx.") ||
                    it.contains(".domain.") ||
                    it.contains(".presentation.") ||
                    it.contains("retrofit", ignoreCase = true) ||
                    it.contains("okhttp", ignoreCase = true)
            }
        )
    }
}
