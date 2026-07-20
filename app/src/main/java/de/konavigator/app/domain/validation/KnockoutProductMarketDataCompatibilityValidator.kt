package de.konavigator.app.domain.validation

import de.konavigator.app.domain.model.KnockoutProductMarketData
import de.konavigator.app.domain.model.KnockoutProductSpecification

/**
 * Prüft ausschließlich die Produktreferenz- und Währungsbeziehung zwischen
 * einer Produktspezifikation und einem Marktdatensatz.
 *
 * Beide Modelle werden als bereits intern validiert vorausgesetzt; ihre
 * Einzelvalidatoren werden nicht aufgerufen. Beide unabhängigen Fehler werden
 * vollständig in stabiler Reihenfolge gesammelt. Der Validator normalisiert
 * keine Werte und erzeugt keine UI-Texte.
 *
 * Aktualität, Quellenqualität, FX- und Quanto-Logik sowie Calculator-, Engine-,
 * UI- und Repository-Anbindung gehören nicht zu seiner Verantwortung. Eine
 * leere Fehlerliste bestätigt nur die Kompatibilität hinsichtlich ISIN und
 * Produktwährung. Sie bestätigt weder interne Modellvalidität, Bid-/Ask-
 * Verfügbarkeit, Aktualität, Quellenqualität, Handelszeit noch eine vollständige
 * Berechnungsfreigabe.
 */
object KnockoutProductMarketDataCompatibilityValidator {

    fun validate(
        specification: KnockoutProductSpecification,
        marketData: KnockoutProductMarketData
    ): List<KnockoutProductCompatibilityError> {
        val errors = mutableListOf<KnockoutProductCompatibilityError>()

        if (specification.productIsin != marketData.productIsin) {
            errors += KnockoutProductCompatibilityError.PRODUCT_ISIN_MISMATCH
        }

        if (specification.productCurrency != marketData.currency) {
            errors += KnockoutProductCompatibilityError.PRODUCT_CURRENCY_MISMATCH
        }

        return errors
    }
}
