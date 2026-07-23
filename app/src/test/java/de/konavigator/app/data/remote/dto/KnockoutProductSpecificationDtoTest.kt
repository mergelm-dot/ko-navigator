package de.konavigator.app.data.remote.dto

import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class KnockoutProductSpecificationDtoTest {

    @Test
    fun dtoContainsExactlyTenNullableDataFields() {
        val fields = KnockoutProductSpecificationDto::class.java.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) || it.isSynthetic }

        assertEquals(
            listOf(
                "productIsin",
                "productWkn",
                "issuerId",
                "underlyingId",
                "direction",
                "basePrice",
                "knockoutBarrier",
                "ratio",
                "underlyingCurrency",
                "productCurrency"
            ),
            fields.map { it.name }
        )
        assertEquals(
            listOf(
                String::class.java,
                String::class.java,
                String::class.java,
                String::class.java,
                String::class.java,
                java.lang.Double::class.java,
                java.lang.Double::class.java,
                java.lang.Double::class.java,
                String::class.java,
                String::class.java
            ),
            fields.map { it.type }
        )
    }

    @Test
    fun dtoHasNoDefaultsOrForbiddenLayerDependencies() {
        val type = KnockoutProductSpecificationDto::class.java
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
        assertEquals(10, constructors.single().parameterCount)
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
