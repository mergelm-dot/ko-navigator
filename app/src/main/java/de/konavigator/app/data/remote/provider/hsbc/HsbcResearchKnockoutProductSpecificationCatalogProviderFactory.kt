package de.konavigator.app.data.remote.provider.hsbc

import de.konavigator.app.data.remote.dto.KnockoutProductSpecificationSnapshotDto

sealed interface HsbcResearchKnockoutProductSpecificationCatalogProviderCreationError {

    data class ProcessingFailure(
        val productIsinKey: String,
        val error: HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingError
    ) : HsbcResearchKnockoutProductSpecificationCatalogProviderCreationError

    data class ProductIsinMismatch(
        val productIsinKey: String,
        val snapshotProductIsin: String?
    ) : HsbcResearchKnockoutProductSpecificationCatalogProviderCreationError
}

sealed interface HsbcResearchKnockoutProductSpecificationCatalogProviderCreationResult {

    data class Success(
        val provider: HsbcResearchKnockoutProductSpecificationCatalogProvider
    ) : HsbcResearchKnockoutProductSpecificationCatalogProviderCreationResult

    data class Failure(
        val errors: List<HsbcResearchKnockoutProductSpecificationCatalogProviderCreationError>
    ) : HsbcResearchKnockoutProductSpecificationCatalogProviderCreationResult
}

/**
 * Erstellt einen Katalogprovider aus bereinigten, kontrollierten lokalen HSBC-Forschungs-JSONs.
 *
 * Jeder Eintrag wird ausschließlich durch den bestehenden
 * [HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessor] verarbeitet. Die Eingabe-Map
 * wird defensiv kopiert und in ihrer Iterationsreihenfolge verarbeitet. Map-Schlüssel und
 * eingebettete Produkt-ISIN müssen exakt übereinstimmen. Alle Fehler werden gesammelt; sobald
 * mindestens ein Fehler vorliegt, entsteht kein partieller Provider.
 *
 * Eine Map kann denselben Schlüssel technisch nicht mehrfach enthalten. Darüber hinaus findet
 * keine Deduplizierung oder Sortierung statt. Die Factory liest keine Systemzeit und besitzt
 * keine Datei-, Netzwerk-, Broker-, Marktdaten-, FX-, Berechnungs- oder UI-Verantwortung.
 */
object HsbcResearchKnockoutProductSpecificationCatalogProviderFactory {

    fun create(
        researchJsonByProductIsin: Map<String, String>,
        retrievedAtEpochMillis: Long
    ): HsbcResearchKnockoutProductSpecificationCatalogProviderCreationResult {
        val researchJsonSnapshot = researchJsonByProductIsin.toMap()
        val snapshots = mutableListOf<KnockoutProductSpecificationSnapshotDto>()
        val errors =
            mutableListOf<HsbcResearchKnockoutProductSpecificationCatalogProviderCreationError>()

        researchJsonSnapshot.forEach { (productIsinKey, json) ->
            when (
                val processingResult =
                    HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessor.process(
                        json = json,
                        retrievedAtEpochMillis = retrievedAtEpochMillis
                    )
            ) {
                is HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingResult.Failure ->
                    errors +=
                        HsbcResearchKnockoutProductSpecificationCatalogProviderCreationError
                            .ProcessingFailure(
                                productIsinKey = productIsinKey,
                                error = processingResult.error
                            )

                is HsbcKnockoutProductSpecificationResearchJsonSnapshotProcessingResult.Success -> {
                    val snapshotProductIsin =
                        processingResult.snapshotDto.specification.productIsin
                    if (snapshotProductIsin != productIsinKey) {
                        errors +=
                            HsbcResearchKnockoutProductSpecificationCatalogProviderCreationError
                                .ProductIsinMismatch(
                                    productIsinKey = productIsinKey,
                                    snapshotProductIsin = snapshotProductIsin
                                )
                    } else {
                        snapshots += processingResult.snapshotDto
                    }
                }
            }
        }

        return if (errors.isNotEmpty()) {
            HsbcResearchKnockoutProductSpecificationCatalogProviderCreationResult.Failure(
                errors = errors
            )
        } else {
            HsbcResearchKnockoutProductSpecificationCatalogProviderCreationResult.Success(
                provider = HsbcResearchKnockoutProductSpecificationCatalogProvider(snapshots)
            )
        }
    }
}
