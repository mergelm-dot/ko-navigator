package de.konavigator.app.calculator

/**
 * Zielkomponente für eindeutig definierte Währungs-, Preis- und
 * Einheitenumrechnungen.
 *
 * Eine Umrechnung muss ihre Richtung und Einheiten fachlich eindeutig benennen.
 * Der Converter enthält langfristig keine Zertifikatspreis-, KO-Abstands- oder
 * Hebelformel und übernimmt keine Orchestrierung vollständiger Berechnungen.
 *
 * Ist-Zustand: [calculateCertificatePrice] enthält noch eine eigenständige
 * Zertifikatspreisformel. Diese bekannte Abweichung wird in diesem rein
 * dokumentarischen Entwicklungsschritt weder entfernt noch verändert.
 */
object PriceConverter {
    fun calculateCertificatePrice(
        underlyingPrice: Double,
        knockoutPrice: Double,
        ratio: Double
    ): Double {

        return (underlyingPrice - knockoutPrice) * ratio
    }

}
