package de.konavigator.app.data.remote.provider

import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationSnapshotDto
import de.konavigator.app.domain.model.TradeDirection

/**
 * Providerneutraler technischer Suchvertrag für KO-Produktspezifikations-Snapshots.
 *
 * Der Provider erhält die stabile Basiswert-ID und die Handelsrichtung exakt. Er beschreibt
 * weder einen konkreten Anbieter noch einen Transport, ein Dateiformat oder eine UI. Eine
 * erfolgreiche Suche ohne passende Produkte wird durch [Success] mit leerer Liste dargestellt.
 */
interface KnockoutProductSpecificationCatalogProvider {

    suspend fun findCandidates(
        underlyingId: String,
        direction: TradeDirection
    ): KnockoutProductSpecificationCatalogProviderResult
}

/**
 * Technisches Ergebnis einer Katalogsuche.
 *
 * Es gibt bewusst keinen `NotFound`-Zustand: Eine erfolgreiche Suche ohne Kandidaten ist ein
 * [Success] mit leerer Liste. Die DTOs werden ohne Normalisierung, Sortierung oder
 * Deduplizierung transportiert.
 */
sealed interface KnockoutProductSpecificationCatalogProviderResult {

    data class Success(
        val candidates: List<KnockoutProductSpecificationSnapshotDto>
    ) : KnockoutProductSpecificationCatalogProviderResult

    data object DataAccessFailure :
        KnockoutProductSpecificationCatalogProviderResult

    data object InvalidData :
        KnockoutProductSpecificationCatalogProviderResult
}
