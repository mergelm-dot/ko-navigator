package de.konavigator.app.data.remote.provider.hsbc

import de.konavigator.app.application.repository.adapter.SnapshotBackedKnockoutProductSpecificationRepository
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

sealed interface HsbcKnockoutProductSpecificationResearchJsonFileSpecificationRepositoryLoadingResult {

    data class Success(
        val repository: SnapshotBackedKnockoutProductSpecificationRepository
    ) : HsbcKnockoutProductSpecificationResearchJsonFileSpecificationRepositoryLoadingResult

    data class Failure(
        val error: HsbcKnockoutProductSpecificationResearchJsonFileSnapshotProviderLoadingError
    ) : HsbcKnockoutProductSpecificationResearchJsonFileSpecificationRepositoryLoadingResult
}

/**
 * Verbindet ausschliesslich den bestehenden lokalen Snapshot-Repository-Loader mit dem
 * bestehenden [SnapshotBackedKnockoutProductSpecificationRepository] als
 * Kompatibilitaetsbruecke fuer bestehende Application-Services.
 *
 * Der Loader verarbeitet nur vorab bereinigte und ausdruecklich freigegebene lokale
 * Forschungsdateien und keine originale HSBC-Webseiten- oder Vaadin-Antwort. Die Eingabe-Map
 * wird vor dem ersten suspendierenden Aufruf defensiv kopiert. Dispatcher und Abrufzeitpunkt
 * werden unveraendert weitergegeben. Fehler des vollstaendigen Datei-bis-Snapshot-Repository-
 * Pfads bleiben vollstaendig typisiert erhalten; bei Fehlern entsteht kein partielles
 * Repository.
 *
 * Domainmapping und Ergebnisuebersetzung erfolgen ausschliesslich durch die bestehenden
 * Adapter. `NotFound`, `DataAccessFailure` und `InvalidData` werden durch die bestehende
 * Kompatibilitaetsbruecke unveraendert weitergegeben. Quelle und Zeitwerte bleiben im
 * zugrunde liegenden Snapshot-Pfad erhalten, koennen ueber den aelteren
 * Produktspezifikations-Port konstruktionsbedingt jedoch nicht transportiert werden. Der
 * Anbieterzeitpunkt stammt ausschliesslich aus dem Forschungs-JSON; zwischen Abruf- und
 * Anbieterzeitpunkt findet kein stiller Ersatz statt.
 *
 * Der Baustein enthaelt keine eigene Datei-, JSON-, Parser-, Mapper-, Processor-, Provider-
 * oder Repositorylogik, keine Domainvalidierung, Data-Quality-, Freshness- oder
 * Berechnungsentscheidung und keine Netzwerk-, Systemzeit- oder Zeitumrechnungsverantwortung.
 */
object HsbcKnockoutProductSpecificationResearchJsonFileSpecificationRepositoryLoader {

    suspend fun load(
        filesByProductIsin: Map<String, File>,
        retrievedAtEpochMillis: Long,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): HsbcKnockoutProductSpecificationResearchJsonFileSpecificationRepositoryLoadingResult {
        val filesByProductIsinSnapshot = filesByProductIsin.toMap()
        return when (
            val snapshotRepositoryLoadingResult =
                HsbcKnockoutProductSpecificationResearchJsonFileSnapshotRepositoryLoader.load(
                    filesByProductIsin = filesByProductIsinSnapshot,
                    retrievedAtEpochMillis = retrievedAtEpochMillis,
                    dispatcher = dispatcher
                )
        ) {
            is HsbcKnockoutProductSpecificationResearchJsonFileSnapshotRepositoryLoadingResult
                .Failure ->
                HsbcKnockoutProductSpecificationResearchJsonFileSpecificationRepositoryLoadingResult
                    .Failure(error = snapshotRepositoryLoadingResult.error)

            is HsbcKnockoutProductSpecificationResearchJsonFileSnapshotRepositoryLoadingResult
                .Success ->
                HsbcKnockoutProductSpecificationResearchJsonFileSpecificationRepositoryLoadingResult
                    .Success(
                        repository = SnapshotBackedKnockoutProductSpecificationRepository(
                            snapshotRepositoryLoadingResult.repository
                        )
                    )
        }
    }
}
