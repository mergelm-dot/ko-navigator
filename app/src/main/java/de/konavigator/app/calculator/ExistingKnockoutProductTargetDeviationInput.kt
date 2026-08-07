package de.konavigator.app.calculator

/**
 * Unveraenderter Eingabevertrag fuer rein numerische Zielabweichungen eines
 * bestehenden KO-Produkts. Ziel- und Ist-Hebel sowie Ziel- und Ist-Barriere
 * bleiben getrennt; der Vertrag validiert und berechnet nichts selbst.
 */
data class ExistingKnockoutProductTargetDeviationInput(
    val plannedEntryPrice: Double,
    val targetLeverage: Double,
    val actualLeverageAtEntry: Double,
    val targetKnockoutBarrier: Double,
    val actualKnockoutBarrier: Double
)
