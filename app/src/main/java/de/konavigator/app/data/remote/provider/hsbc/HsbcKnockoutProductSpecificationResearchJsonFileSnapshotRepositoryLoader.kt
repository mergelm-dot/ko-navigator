package de.konavigator.app.data.remote.provider.hsbc

import de.konavigator.app.data.remote.RemoteKnockoutProductSpecificationSnapshotRepository
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

sealed interface HsbcKnockoutProductSpecificationResearchJsonFileSnapshotRepositoryLoadingResult {

    data class Success(
        val repository: RemoteKnockoutProductSpecificationSnapshotRepository
    ) : HsbcKnockoutProductSpecificationResearchJsonFileSnapshotRepositoryLoadingResult

    data class Failure(
        val error: HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingError
    ) : HsbcKnockoutProductSpecificationResearchJsonFileSnapshotRepositoryLoadingResult
}

/**
 * Verbindet ausschliesslich den bestehenden lokalen Datei-bis-Provider-Loader mit dem
 * bestehenden generischen [RemoteKnockoutProductSpecificationSnapshotRepository].
 *
 * Der Loader verarbeitet nur vorab bereinigte und ausdruecklich freigegebene lokale
 * Forschungsdateien und keine originale HSBC-Webseiten- oder Vaadin-Antwort. Die Eingabe-Map
 * wird vor dem ersten suspendierenden Aufruf defensiv kopiert. Dispatcher und Abrufzeitpunkt
 * werden unveraendert weitergegeben. Fehler des Datei-bis-Provider-Pfads bleiben vollstaendig
 * typisiert erhalten; bei Fehlern entsteht kein partielles Repository.
 *
 * Das Repository-Mapping erfolgt ausschliesslich durch den bestehenden generischen
 * Snapshot-Repository-Adapter. Der Anbieterzeitpunkt stammt ausschliesslich aus dem jeweiligen
 * Forschungs-JSON; zwischen Abruf- und Anbieterzeitpunkt findet kein stiller Ersatz statt.
 *
 * Der Baustein enthaelt keine eigene Datei-, JSON-, Parser-, Mapper-, Processor-, Provider-
 * oder Repositorylogik, keine Domainvalidierung, Data-Quality-, Freshness- oder
 * Berechnungsentscheidung und keine Netzwerk-, Systemzeit- oder Zeitumrechnungsverantwortung.
 */
object HsbcKnockoutProductSpecificationResearchJsonFileSnapshotRepositoryLoader {

    suspend fun load(
        filesByProductIsin: Map<String, File>,
        retrievedAtEpochMillis: Long,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): HsbcKnockoutProductSpecificationResearchJsonFileSnapshotRepositoryLoadingResult {
        val filesByProductIsinSnapshot = filesByProductIsin.toMap()
        return when (
            val providerLoadingResult =
                HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoader.load(
                    filesByProductIsin = filesByProductIsinSnapshot,
                    retrievedAtEpochMillis = retrievedAtEpochMillis,
                    dispatcher = dispatcher
                )
        ) {
            is HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingResult
                .Failure ->
                HsbcKnockoutProductSpecificationResearchJsonFileSnapshotRepositoryLoadingResult
                    .Failure(error = providerLoadingResult.error)

            is HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingResult
                .Success ->
                HsbcKnockoutProductSpecificationResearchJsonFileSnapshotRepositoryLoadingResult
                    .Success(
                        repository = RemoteKnockoutProductSpecificationSnapshotRepository(
                            providerLoadingResult.provider
                        )
                    )
        }
    }
}
