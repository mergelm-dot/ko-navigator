package de.konavigator.app.application.productdiscovery

import de.konavigator.app.domain.availability.MarketDataCalculationType

/**
 * Providerneutraler Application-Auftrag zur Quellenfreigabe von Kandidaten aus
 * dem Freshness-Gate. Die Kandidatenliste und der Berechnungstyp werden exakt
 * übernommen; der Request bewertet selbst keine Quelle.
 *
 * Die Liste enthält ausschließlich bereits Data-Quality-freigegebene,
 * calculation-available und frische Kandidaten. Sie darf leer sein; Reihenfolge,
 * Duplikate und dieselbe Listenreferenz bleiben erhalten. Der Auftrag enthält
 * weder Evaluierungszeit, Freshness-Schwellen, separate Quellen- oder ISIN-Listen,
 * Broker- oder Emittentenauswahl, Quellenregeln, Preis-, Hebel- oder
 * Rankingparameter. Er normalisiert keine Werte und besitzt keine Android-,
 * Compose-, Provider-, DTO- oder Infrastrukturabhängigkeit.
 */
data class KnockoutProductCandidateSourceEvaluationRequest(
    val candidates: List<KnockoutProductCandidateWithFreshness>,
    val calculationType: MarketDataCalculationType
)
