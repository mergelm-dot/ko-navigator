package de.konavigator.app.domain.validation

import de.konavigator.app.domain.model.KnockoutProductSpecification

/**
 * Prüft die allgemeinen Version-1-Regeln einer [KnockoutProductSpecification].
 *
 * Der Validator enthält keine emittentenspezifischen Regeln und sammelt alle
 * erkannten Fehler in einer stabilen, fachlich dokumentierten Reihenfolge. Er
 * normalisiert oder verändert keine Eingaben und erzeugt keine UI-Texte. Das
 * validierte Domainmodell selbst bleibt eine unveränderte reine Data Class.
 *
 * Formale ISIN- und WKN-Prüfungen sowie eine Prüfung gegen eine vollständige
 * ISO-Währungsliste können in späteren Entwicklungsschritten ergänzt werden.
 */
object KnockoutProductSpecificationValidator {

    private val currencyCodePattern = Regex("[A-Z]{3}")

    fun validate(
        specification: KnockoutProductSpecification
    ): List<KnockoutProductValidationError> {
        val errors = mutableListOf<KnockoutProductValidationError>()

        if (specification.productIsin.isBlank()) {
            errors += KnockoutProductValidationError.MISSING_PRODUCT_ISIN
        }

        if (specification.productWkn?.isBlank() == true) {
            errors += KnockoutProductValidationError.INVALID_PRODUCT_WKN
        }

        if (specification.issuerId.isBlank()) {
            errors += KnockoutProductValidationError.MISSING_ISSUER_ID
        }

        if (specification.underlyingId.isBlank()) {
            errors += KnockoutProductValidationError.MISSING_UNDERLYING_ID
        }

        if (!specification.basePrice.isFinite() || specification.basePrice <= 0.0) {
            errors += KnockoutProductValidationError.INVALID_BASE_PRICE
        }

        if (!specification.knockoutBarrier.isFinite() || specification.knockoutBarrier <= 0.0) {
            errors += KnockoutProductValidationError.INVALID_KNOCKOUT_BARRIER
        }

        if (!specification.ratio.isFinite() || specification.ratio <= 0.0) {
            errors += KnockoutProductValidationError.INVALID_RATIO
        }

        if (!currencyCodePattern.matches(specification.underlyingCurrency)) {
            errors += KnockoutProductValidationError.INVALID_UNDERLYING_CURRENCY
        }

        if (!currencyCodePattern.matches(specification.productCurrency)) {
            errors += KnockoutProductValidationError.INVALID_PRODUCT_CURRENCY
        }

        return errors
    }
}
