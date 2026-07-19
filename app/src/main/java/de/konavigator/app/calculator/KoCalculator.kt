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
 * Preisberechnung einschließlich Rundung. Diese bekannte Abweichung bleibt in
 * diesem rein dokumentarischen Entwicklungsschritt unverändert.
 */
object KoCalculator {

    /**
     * Berechnet im aktuellen vereinfachten Ist-Modell den theoretischen Preis
     * eines Long- oder Short-KO-Zertifikats.
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
