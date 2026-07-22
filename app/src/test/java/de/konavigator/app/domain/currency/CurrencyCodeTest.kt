package de.konavigator.app.domain.currency

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyCodeTest {

    @Test
    fun validThreeLetterUppercaseCodeIsAccepted() {
        assertSuccess("EUR", "EUR")
    }

    @Test
    fun lowercaseCodeIsNormalizedToUppercase() {
        assertSuccess("usd", "USD")
    }

    @Test
    fun surroundingWhitespaceIsTrimmed() {
        assertSuccess("  jpy\t", "JPY")
    }

    @Test
    fun emptyOrBlankCodeIsRejected() {
        listOf("", "   ", "\t\n").forEach(::assertInvalid)
    }

    @Test
    fun codeWithFewerOrMoreThanThreeCharactersIsRejected() {
        listOf("EU", "EURO").forEach(::assertInvalid)
    }

    @Test
    fun digitsOrSpecialCharactersAreRejected() {
        listOf("E1R", "E-R", "€UR", "ßd").forEach(::assertInvalid)
    }

    private fun assertSuccess(input: String, expected: String) {
        assertEquals(
            CurrencyCodeCreationResult.Success(currencyCode(expected)),
            CurrencyCode.create(input)
        )
    }

    private fun assertInvalid(input: String) {
        assertEquals(
            CurrencyCodeCreationResult.Failure(CurrencyCodeCreationError.INVALID_FORMAT),
            CurrencyCode.create(input)
        )
    }

    private fun currencyCode(value: String): CurrencyCode =
        when (val result = CurrencyCode.create(value)) {
            is CurrencyCodeCreationResult.Success -> result.currencyCode
            is CurrencyCodeCreationResult.Failure ->
                error("Unexpected invalid test currency: ${result.error}")
        }
}
