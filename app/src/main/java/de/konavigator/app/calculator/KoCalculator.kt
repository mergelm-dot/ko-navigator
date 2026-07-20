package de.konavigator.app.calculator

import kotlin.math.round

/**
 * Zentrale Zielkomponente für reine mathematische Fachformeln zu KO-Produkten.
 *
 * Langfristig gehören hierher klar abgegrenzte Berechnungen wie theoretische
 * KO-Barriere, gerichteter KO-Abstand, innerer Wert, Modellpreis sowie
 * tatsächlicher und zukünftiger Hebel. Der Calculator erhält alle benötigten
 * Fachwerte als Parameter und liefert unformatierte mathematische Ergebnisse.
 *
 * Nicht zu seiner Verantwortung gehören UI-State, Benutzertexte, Empfehlungen,
 * Ablaufsteuerung, Validierungsorchestrierung, Repository- oder Netzwerkzugriffe,
 * Android- oder Compose-Abhängigkeiten sowie Anzeigeformatierung.
 *
 * Ist-Zustand: [calculateCertificatePrice] enthält noch eine vereinfachte
 * Preisberechnung einschließlich Rundung. Diese bekannte Abweichung bleibt bis
 * zu einem gesondert freigegebenen Konsolidierungsschritt unverändert.
 */
object KoCalculator {

    /**
     * Berechnet den gerichteten absoluten Abstand zwischen Basiswertkurs und
     * KO-Barriere. Negative Ergebnisse bleiben erhalten.
     */
    fun calculateKnockoutDistanceAbsolute(
        underlyingPrice: Double,
        knockoutPrice: Double,
        isLong: Boolean
    ): Double {
        return if (isLong) {
            underlyingPrice - knockoutPrice
        } else {
            knockoutPrice - underlyingPrice
        }
    }

    /**
     * Berechnet den gerichteten KO-Abstand relativ zum Basiswertkurs in Prozent.
     */
    fun calculateKnockoutDistancePercent(
        underlyingPrice: Double,
        knockoutPrice: Double,
        isLong: Boolean
    ): Double {
        val absoluteDistance = calculateKnockoutDistanceAbsolute(
            underlyingPrice = underlyingPrice,
            knockoutPrice = knockoutPrice,
            isLong = isLong
        )

        return absoluteDistance / underlyingPrice * 100.0
    }

    /**
     * Berechnet den inneren Wert mit einem separaten Basispreis.
     *
     * Basispreis und KO-Barriere sind fachlich unterschiedliche Größen. Long
     * verwendet `max(underlyingPrice - basePrice, 0) * ratio`, Short verwendet
     * `max(basePrice - underlyingPrice, 0) * ratio`. Die Funktion rundet nicht;
     * der Rückgabewert liegt zunächst in der durch die Eingabedaten bestimmten
     * Preiseinheit.
     *
     * Wechselkurs, Finanzierung, Premium, Bid, Ask und Spread sind nicht
     * enthalten. Das Ergebnis ist weder ein vollständiger Modellpreis noch ein
     * realer Emittentenpreis. Die Eingabevalidierung erfolgt außerhalb dieser
     * reinen Funktion. Sie ist nicht mit der bestehenden Übergangsfunktion
     * [calculateCertificatePrice] gleichzusetzen.
     */
    fun calculateIntrinsicValue(
        underlyingPrice: Double,
        basePrice: Double,
        ratio: Double,
        isLong: Boolean
    ): Double {
        val difference = if (isLong) {
            underlyingPrice - basePrice
        } else {
            basePrice - underlyingPrice
        }

        return maxOf(difference, 0.0) * ratio
    }

    /**
     * Charakterisiert die bestehende Übergangsberechnung eines gerundeten,
     * ratio-skalierten KO-Differenzwerts.
     *
     * Long verwendet `(underlyingPrice - knockoutPrice) * ratio`, Short
     * `(knockoutPrice - underlyingPrice) * ratio`. An der KO-Barriere und auf
     * der jeweils ausgeknockten Seite wird `0.0` zurückgegeben. Das Ergebnis
     * wird innerhalb dieser Funktion auf zwei Dezimalstellen gerundet.
     *
     * Die Funktion verwendet derzeit die KO-Barriere anstelle eines getrennten
     * Basispreises unter der reinen Übergangsannahme `B = KO`. Das Ergebnis ist
     * deshalb weder ein allgemeiner innerer Wert noch ein vollständiger
     * Modellpreis oder ein realer Emittenten-, Bid- beziehungsweise Ask-Preis.
     * Währungsumrechnung, Finanzierung, Premium, Spread und Ratio-Validierung
     * sind nicht Bestandteil dieser Berechnung.
     *
     * Diese Übergangslogik ist fachlich nicht endgültig freigegeben und muss
     * langfristig durch getrennt modellierte, präzise Berechnungen ersetzt oder
     * entsprechend ihrer tatsächlichen Verantwortung weiter präzisiert werden.
     */
    fun calculateCertificatePrice(
        underlyingPrice: Double,
        knockoutPrice: Double,
        ratio: Double,
        isLong: Boolean
    ): Double {

        val price =
            if (isLong) {

                if (underlyingPrice <= knockoutPrice) {
                    return 0.0
                }

                (underlyingPrice - knockoutPrice) * ratio

            } else {

                if (underlyingPrice >= knockoutPrice) {
                    return 0.0
                }

                (knockoutPrice - underlyingPrice) * ratio
            }

        return round(price * 100) / 100
    }
}
