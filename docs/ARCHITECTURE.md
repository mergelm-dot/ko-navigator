# KO Navigator – Technische Architektur

## 1. Zweck des Dokuments

Dieses Dokument definiert die verbindliche Zielarchitektur des KO Navigators. Es ergänzt `ROADMAP.md`, `DEVLOG.md`, `AGENTS.md`, `docs/FORMULAS.md` und das ADR-Register `docs/DECISIONS.md`. Künftige Architekturänderungen müssen auch hier und als Architecture Decision Record nachvollziehbar dokumentiert werden, damit Ist-Zustand, Zielbild und Migrationsweg nicht auseinanderlaufen.

Der KO Navigator ist ein professionelles Planungs- und Analysewerkzeug für Knock-out-Zertifikate. Er unterstützt Nutzer bei transparenten Berechnungen und Szenarien, gibt aber keine Kauf- oder Verkaufsempfehlungen und ist keine Anlageberatung.

## 2. Architekturziele

Die Architektur verfolgt folgende Ziele:

- **Mathematische Korrektheit:** Formeln, Einheiten, Richtungen und Grenzfälle müssen fachlich korrekt und durch Tests abgesichert sein.
- **Klare Trennung von UI und Fachlogik:** Die Oberfläche stellt Daten dar und nimmt Eingaben entgegen; Berechnungen erfolgen außerhalb der UI.
- **Kleine, testbare Komponenten:** Jede Komponente erhält eine klar begrenzte Verantwortung.
- **Austauschbare Datenquellen:** Lokale Testdaten, externe APIs und spätere Caches werden hinter Schnittstellen gekapselt.
- **Nachvollziehbare Fehlerbehandlung:** Fehler, fehlende Daten und Warnungen werden ausdrücklich und strukturiert abgebildet.
- **Erweiterbarkeit für reale Zertifikatsdaten:** Produktstammdaten, Kurse, Emittenteninformationen und Zeitstempel müssen später ohne grundlegenden Umbau integrierbar sein.
- **Unterstützung weiterer Plattformen:** Die Fachlogik soll unabhängig von Android und Compose bleiben, damit später Android und weitere Plattformen darauf zugreifen können.
- **Angemessene Einfachheit:** In der frühen Entwicklungsphase wird keine Infrastruktur eingeführt, die noch keinen konkreten Nutzen hat.
- **Qualität vor Geschwindigkeit:** Korrektheit, Verständlichkeit und Regressionssicherheit haben Vorrang vor schneller Erweiterung.

## 3. Aktueller Ist-Zustand

> **Bestandsaufnahme:** Dieser Abschnitt beschreibt ausschließlich den derzeitigen Stand. Er ist nicht mit der Zielarchitektur gleichzusetzen.

Der KO Navigator ist aktuell eine Android-App mit Jetpack Compose und einem App-Modul. `MainActivity` ist der Einstiegspunkt und rendert `TradePlannerRoute` als aktive Hauptoberfläche. Die Route verbindet das Activity-seitig verwaltete `TradePlannerViewModel` mit dem state-gesteuerten `TradePlannerScreen`. Die Basiswertsuche arbeitet weiterhin mit lokalen Testdaten über `UnderlyingRepository`, `UnderlyingSearchEngine` und `UnderlyingSearchField`.

Für Berechnungen existieren derzeit `TradeCalculationEngine`, `KoCalculator` und `PriceConverter`. Der theoretische Trade-Planning-Pfad ist über Composition, Factory, ViewModel, Route und Screen an die aktive `MainActivity` angebunden. Parallel bestehen weiterhin `CalculatorScreen` mit direktem `KoCalculator`-Zugriff und eine Zertifikatspreisberechnung in `PriceConverter`. Diese Überschneidungen sind schrittweise zu konsolidieren.

Der aktuelle Stand umfasst außerdem:

- einen ausschließlich im Debug-Source-Set sichtbaren Marktdatenberechnungspfad
  mit eigener Activity, Composition, Route und stateless Screen,
- getrennte Domainkomponenten für Produktspezifikations- und
  Marktdatenvalidierung, Kompatibilität, Verfügbarkeit, Freshness und
  Quellenfreigabe,
- eine siebenstufige Fail-Fast-Marktdatenorchestrierung sowie einen lokalen
  Application-Integrationspfad über read-only In-Memory-Repositories,
- 25 feste Erfolgs- und Fehlerszenarien für die reale
  `TradeCalculationEngine`,
- keine Dependency Injection,
- keine Navigation,
- keine Netzwerk- oder Persistenzschicht,
- lokale, fest hinterlegte Basiswert- und Emittentendaten,
- weiterhin lokalen UI-Zustand für Suche, Broker und Emittent im
  `TradePlannerScreen` und
- erste Berechnungsmodelle, die noch nicht vollständig dem langfristigen Zielmodell entsprechen.

Diese Ausgangslage ist bewusst klein. Die folgenden Abschnitte beschreiben das Zielbild und keine bereits abgeschlossene Umsetzung.

## 4. Zielarchitektur auf hoher Ebene

Der fachliche Berechnungsfluss lautet:

```text
UI / Compose
    ↓
ViewModel
    ↓
Use Case oder Application Service
    ↓
TradeCalculationEngine
    ↓
reine mathematische Komponenten
```

Der Datenzugriff folgt einem getrennten Fluss:

```text
UI
    ↓
ViewModel
    ↓
Repository-Interface
    ↓
lokale oder externe Datenquelle
```

Die Ebenen haben folgende Verantwortungen:

- **UI / Compose:** Zeigt den aktuellen UI-State an, erfasst Nutzeraktionen und sendet Events. Sie enthält keine Fachformeln.
- **ViewModel:** Hält und verändert den UI-State, verarbeitet UI-Events und ruft Anwendungsfälle auf.
- **Use Case oder Application Service:** Orchestriert einen vollständigen fachlichen Ablauf, etwa eine Szenarioberechnung oder Produktsuche.
- **TradeCalculationEngine:** Validiert zusammengehörige Fachdaten und koordiniert die benötigten mathematischen Komponenten.
- **Reine mathematische Komponenten:** Berechnen klar abgegrenzte Werte ohne Android-, UI-, Netzwerk- oder Persistenzabhängigkeiten.
- **Repository-Interface:** Beschreibt, welche Daten die Anwendung benötigt, ohne die konkrete Quelle festzulegen.
- **Datenquelle:** Lädt Daten lokal, aus einem Cache oder von einem externen Anbieter und liefert sie über eine Repository-Implementierung.

Abhängigkeiten zeigen grundsätzlich nach innen: Presentation kennt Application, Application kennt Domain und Repository-Abstraktionen, Data implementiert diese Abstraktionen. Domain kennt weder Presentation noch Data.

## 5. Empfohlene Schichten

### 5.1 Presentation Layer

Der Presentation Layer enthält:

- Compose Screens,
- wiederverwendbare UI Components,
- ViewModels,
- UI State,
- UI Events,
- optionale einmalige UI Effects und
- Formatierung ausschließlich für die Anzeige.

Verbindliche Regeln:

- Composables enthalten keine Fachformeln.
- Screens greifen nicht direkt auf Repositories oder Datenquellen zu.
- Fehlende Daten werden nicht durch stille Defaultwerte ersetzt.
- Nutzereingaben werden validiert und als strukturierter State dargestellt.
- Anzeigeformatierung verändert keine fachlichen Werte.
- Ein Screen rendert State und sendet Events; er entscheidet nicht über Fachabläufe.

#### 5.1.1 Presentation-Vertrag für Marktdatenberechnungen

Das Package `de.konavigator.app.presentation.marketdata` enthält den ersten
isolierten Presentation-Vertrag des neuen KO-Produktpfads. Der
`MarketDataCalculationUiState` hält die drei unveränderten UI-Eingaben und den
typisierten Submission-Zustand. Sein read-only `StateFlow` wird ausschließlich
vom `MarketDataCalculationViewModel` aktualisiert. Vier explizite Methoden
übernehmen ISIN, CalculationType und Bewertungszeitpunkt beziehungsweise
starten die Berechnung.

Die erlaubte UI-Validierung beschränkt sich auf eine nicht leere ISIN, einen
nicht leeren Bewertungszeitpunkt und dessen Long-Parsebarkeit. Sie führt keine
ISIN-Fachvalidierung und keine Normalisierung aus. Negative Epoch-Werte und die
Long-Grenzwerte bleiben zulässig. Der Bewertungszeitpunkt wird ausdrücklich
vom Benutzerfluss bereitgestellt; das ViewModel liest keine Systemzeit.

`MarketDataCalculationUiSubmission` trennt Idle, Loading, Eingabefehler und
abgeschlossene Resultate. `MarketDataCalculationUiResult` unterscheidet die
vier fachlichen Erfolgswerttypen und verdichtete, UI-nahe Fehlerkategorien.
Application- und Domain-Fehlerdetails werden dabei nicht als freie Texte oder
Fehlerlisten in den UI-State übernommen. Erfolgswerte bleiben roh und
ungerundet; ausschließlich der Debug-Screen formatiert sie für die Anzeige.

Das ViewModel hängt ausschließlich vom
`MarketDataCalculationApplicationService` sowie dessen Application- und
Domain-Vertragstypen ab. Es kennt keine Repositories, Policies oder
Orchestrator-Komponenten und führt keine Domainberechnung aus. Laufende Jobs
werden bei Eingabeänderungen abgebrochen; ein typisierter Loading-Zustand
verhindert parallele Requests und veraltete Resultate.

Der Pfad besitzt inzwischen eine allgemeine ViewModelFactory und eine
debug-exklusive Compose-Demo mit lokalen Daten. Er ist weiterhin nicht an die
produktive `MainActivity`, Navigation oder den aktiven `TradePlannerScreen`
angebunden und bleibt vom lokalen `UnderlyingRepository`-,
`UnderlyingSearchEngine`- und `UnderlyingTestData`-Pfad getrennt. Diese
Trennung ist der aktuelle Zustand; eine Release-Migration ist **OFFEN**.

#### 5.1.2 Debug-exklusive Engine-Demo

Der vorhandene Marktdaten-Presentation-Vertrag wird über eine vollständig vom
produktiven App-Einstieg getrennte Debug-Composition sichtbar ausführbar. Die
allgemeine `MarketDataCalculationViewModelFactory` liegt im Main-Package
`de.konavigator.app.presentation.marketdata` und besitzt ausschließlich den
`MarketDataCalculationApplicationService` als Konstruktorabhängigkeit. Sie
erzeugt nur das zugehörige ViewModel und führt keine Repository-Erzeugung oder
Dependency Injection durch.

Alle demospezifischen Typen liegen unter
`src/debug/java/de/konavigator/app/debug/marketdata`. Ein Debug-Manifest
registriert `MarketDataCalculationDemoActivity` als zweiten Launcher-Einstieg.
Die produktive `MainActivity` und ihr aktiver `TradePlannerScreen`
referenzieren keine Debug-Typen. Da Activity, Screen,
Composition, Ressourcen und Manifest-Erweiterung ausschließlich im
Debug-Source-Set liegen, enthält der Release-Build weder Demo-UI noch Demo-Daten.

Die Debug-Activity bildet den Composition Root: Sie erzeugt einmalig eine
Factory über `MarketDataCalculationDemoComposition`, bindet das ViewModel an
ihren Lifecycle und rendert die Route im bestehenden `KONavigatorTheme`. Die
Route sammelt den `StateFlow` lifecycle-sicher und verbindet exakt die vier
ViewModel-Eingabemethoden mit dem stateless Screen. Der Screen kennt weder
Application-Service noch Repositories, Policies, Orchestrator oder
Domainmodelle und führt keine fachliche Berechnung aus.

`MarketDataCalculationDemoComposition` erzeugt bei jedem Aufruf einen neuen,
vollständigen lokalen Objektgraphen: genau eine neutrale Produktspezifikation,
genau einen kompatiblen Quote, beide In-Memory-Repositories, explizite
Freshness- und Source-Policies, Orchestrator, Application-Service und Factory.
Der feste Bewertungszeitpunkt wird als UI-Eingabe übergeben; Systemzeit wird
nicht gelesen. Es gibt keinen global mutierbaren Zustand und keine echten
Produkt-, Emittenten- oder Marktdaten.

Sichtbare Texte und Demo-Hinweise liegen ausschließlich in Debug-Ressourcen.
Die UI formatiert rohe Resultatwerte locale-basiert nur zur Anzeige und
verändert den fachlichen State nicht. Der Debug-Pfad bleibt vollständig vom
alten `UnderlyingRepository`-, `UnderlyingSearchEngine`- und
`UnderlyingTestData`-Pfad getrennt. Eine echte Release-Composition und die
schrittweise Migration des Trade Planners bleiben **OFFEN**.

#### 5.1.3 Trade-Planner-Presentation-Vertrag

Das Package `de.konavigator.app.presentation.tradeplanner` enthält den
isolierten Presentation-Vertrag für die theoretische Trade-Planung. Der
immutable `TradePlannerUiState` bewahrt drei unveränderte Eingabestrings, die
typisierte `TradeDirection` und einen Submission-Zustand. Das
`TradePlannerViewModel` stellt diesen State als read-only `StateFlow` bereit
und besitzt ausschließlich den `TradePlanningApplicationService` als
Konstruktorabhängigkeit.

Der synchrone Datenfluss lautet:

```text
UI-Strings
→ Parsing und Presentation-Validierung
→ EntryPriceRelationEvaluator
→ TradePlanningApplicationService
→ TradeCalculationEngine
→ typisiertes TradePlannerUiResult
```

`TradePlannerUiSubmission` unterscheidet ausschließlich `Idle`,
`InvalidInput(errors)` und `Completed(result)`. Der Pfad benötigt weder
Coroutines noch Loading oder `SavedStateHandle`. Jede Eingabeänderung bewahrt
den Originalstring und setzt ein vorhandenes Ergebnis auf `Idle` zurück. Erst
der explizite Berechnungsaufruf parst lokale Kopien und sammelt UI-nahe Fehler
in stabiler Feldreihenfolge.

Nach erfolgreicher Eingabeprüfung wird die brokerneutrale Preisrelation vor
dem Serviceaufruf bestimmt. Der zentrale Adapter erzeugt den
`TradeCalculationInput` mit der ausdrücklich dokumentierten Übergangsratio
`0.01`, `CurrencyConversion.SameCurrency(CurrencyCode("XXX"))` und einem
expliziten Mapping von `TradeDirection` auf den weiterhin internen
Engine-Boolean. `XXX` behauptet keine reale Währung. Das Presentation-Result
übernimmt nur vollständige, ungerundete Rechenwerte. Engine-Fehler werden
typisiert abgebildet; Freitext ist kein Bestandteil des Domain- oder
Presentation-Resultats.

Das ViewModel kennt keine Repositories, Marktdaten, Systemzeit, Android-
Ressourcen, Compose-Komponenten oder Broker-Ordertypen. Es ist über
`TradePlannerRoute` an den state-gesteuerten `TradePlannerScreen` angebunden;
die produktive `MainActivity` verwendet diese Route als aktiven UI-Einstieg.
Underlying-Suche, Assetauswahl, Broker und Emittenten bleiben während dieser
schrittweisen Migration lokal im bestehenden Composable.

#### 5.1.4 Trade-Planner-Factory und produktive Composition

Die `TradePlannerViewModelFactory` liegt gemeinsam mit dem ViewModel im Package
`de.konavigator.app.presentation.tradeplanner`. Sie besitzt ausschließlich den
`TradePlanningApplicationService` als Konstruktorabhängigkeit und erzeugt nur
bei einem exakten Klassenvergleich ein neues `TradePlannerViewModel`.
Unbekannte ViewModel-Typen werden mit einer `IllegalArgumentException`
abgelehnt. Die Factory kennt weder Android Context oder Ressourcen noch
Repositories, MarketData, Debug-Code oder Compose.

Das Package `de.konavigator.app.composition` enthält mit
`TradePlannerComposition` den produktiven Composition-Einstieg im
Main-Source-Set. Der vollständige Objektgraph lautet:

```text
TradeCalculationEngine
→ TradePlanningApplicationService
→ TradePlannerViewModelFactory
→ TradePlannerViewModel
```

`TradePlannerComposition` ist ein zustandsloses Object mit genau der
parameterlosen Funktion `createViewModelFactory()`. Jeder Aufruf verwendet das
bestehende `TradeCalculationEngine`-Object, erzeugt aber einen neuen
`TradePlanningApplicationService` und eine neue Factory. Es werden weder
Service noch ViewModel-State global gespeichert. Der Composition-Pfad besitzt
keine Android-Context-, Repository-, MarketData- oder Debug-Abhängigkeit.

Die `MainActivity` erzeugt die Factory einmalig über ein nicht
thread-synchronisiertes `lazy` außerhalb der Recomposition. Das ViewModel wird
mit `ViewModelProvider` aus dem Activity-`ViewModelStore` bezogen und überlebt
damit Konfigurationsänderungen gemäß dem Android-Lifecycle. Route und
state-gesteuerter Screen besitzen selbst keine Composition-Verantwortung.

#### 5.1.5 Trade-Planner-Route und state-gesteuerter Screen

Die produktive Route liegt unter
`de.konavigator.app.presentation.tradeplanner.TradePlannerRoute` und besitzt
den Vertrag:

```kotlin
@Composable
fun TradePlannerRoute(
    viewModel: TradePlannerViewModel,
    modifier: Modifier = Modifier
)
```

Sie ist ein reiner Compose-Adapter: `collectAsStateWithLifecycle` sammelt den
`StateFlow` lifecycle-sicher, danach reicht die Route State, Modifier und exakt
die fünf vorhandenen Eingabemethoden an den Screen weiter. Sie besitzt keinen
eigenen Zustand, erzeugt kein ViewModel und kennt weder Factory, Composition,
Application-Service noch Engine. Dafür ist `lifecycle-runtime-compose` nun als
`implementation` im Main-Classpath verfügbar; es wurde keine weitere
Abhängigkeit ergänzt.

Der Screen-Vertrag lautet:

```kotlin
@Composable
fun TradePlannerScreen(
    state: TradePlannerUiState,
    onCurrentPriceChanged: (String) -> Unit,
    onPlannedEntryPriceChanged: (String) -> Unit,
    onTargetLeverageChanged: (String) -> Unit,
    onDirectionChanged: (TradeDirection) -> Unit,
    onCalculateClicked: () -> Unit,
    modifier: Modifier = Modifier
)
```

Aktueller Kurs, geplanter Einstieg, Zielhebel und Richtung werden ausschließlich
aus dem Presentation-State dargestellt. Das Composable enthält dafür weder
Parsing und Eingabevalidierung noch Prozentformeln, Broker-Ordertypen oder
Strategieerklärungen. Nur Basiswertsuche und Assetauswahl sowie Broker- und
Emittentenauswahl bleiben vorläufig lokale UI-Zustände. Ein fehlender Assetpreis
wird über beide Preis-Callbacks als leerer String weitergegeben und niemals als
künstlicher Nullpreis dargestellt.

`Idle` zeigt keine Submission-Box. `InvalidInput` mappt die sechs strukturierten
Eingabefehler ausschließlich feldnah. Ein abgeschlossenes Erfolgsresultat zeigt
die neutrale Einstiegskursrelation, den theoretischen Produktwert, Zielhebel,
berechneten theoretischen Hebel, KO-Barriere sowie absoluten und prozentualen
KO-Abstand locale-basiert und nur zur Anzeige formatiert. Die Box ist
ausdrücklich eine theoretische Modellrechnung und bildet weder
Produktidentität noch WKN, ISIN, Emittent, Bid/Ask, Spread, Zeitstempel oder
andere Marktdaten ab. Der theoretische Produktwert wird nicht als Kaufpreis
bezeichnet. Ein abgeschlossenes Fehlerresultat mappt einen der acht
`TradePlannerUiCalculationError`-Codes in eine neutrale Fehlerbox.

Der aktive Datenfluss lautet:

```text
MainActivity
→ TradePlannerComposition
→ TradePlannerViewModelFactory
→ TradePlannerViewModel
→ TradePlannerRoute
→ TradePlannerScreen
```

`MainActivity` erzeugt weder Application-Service noch Engine und kennt keine
Domain-Berechnungsdetails. Die Factory liegt außerhalb der Recomposition, das
ViewModel im Activity-`ViewModelStore`, und die Route sammelt dessen StateFlow
lifecycle-sicher. Der temporäre No-Argument-Wrapper ist vollständig entfernt;
produktiv existiert nur noch der state- und callback-gesteuerte Screen-Vertrag.

### 5.2 Application Layer

Der Application Layer enthält:

- Use Cases,
- die Orchestrierung von Berechnungen,
- Trade-Planungsabläufe,
- Zertifikatssuche,
- Szenarioberechnungen und
- Umwandlungen zwischen UI-Modellen und Domain-Modellen, soweit diese nicht reine Darstellung sind.

Mögliche Use Cases sind:

- `CalculateTradeScenarioUseCase`,
- `FindMatchingCertificatesUseCase`,
- `LoadUnderlyingPriceUseCase` und
- `CalculateFutureLeverageUseCase`.

Ein Use Case bildet einen für Nutzer verständlichen Ablauf ab. Er darf mehrere Domain-Funktionen und Repository-Interfaces koordinieren, enthält aber keine Compose-Logik und kennt keine konkrete API-Implementierung.

#### 5.2.1 MarketDataCalculationApplicationService

Das Package `de.konavigator.app.application.marketdata` enthält den
Application-Auftrag, die technischen Fehler- und Result-Typen sowie den
`MarketDataCalculationApplicationService`. Der Service erhält exakt drei
Konstruktorabhängigkeiten: den Spezifikations-Port, den Marktdaten-Port und
einen fertig konfigurierten `MarketDataCalculationOrchestrator`. Seine einzige
öffentliche Funktion `execute` ist `suspend`-fähig.

Der Ablauf ist sequenziell und Fail Fast: Zuerst wird die Produktspezifikation
über die exakt übergebene Produkt-ISIN geladen. Nur nach diesem Erfolg werden
die Marktdaten geladen. Erst nach beiden erfolgreichen Repository-Zugriffen
erzeugt der Service den Domainrequest mit dem unveränderten CalculationType und
Bewertungszeitpunkt und ruft den Orchestrator auf. Parallele Abrufe und eine
Aggregation technischer Fehler finden nicht statt.

`MarketDataCalculationApplicationResult` trennt technische
Datenunverfügbarkeit von der Domainauswertung. `PRODUCT_NOT_FOUND`,
`MARKET_DATA_NOT_FOUND` und `DATA_ACCESS_FAILURE` beschreiben ausschließlich
die drei vorgesehenen Application-Fehler. Das Domainresult wird dagegen ohne
Analyse, Umbenennung oder Mapping unverändert eingebettet.

Der Service enthält keine Domainregeln, liest keine Systemzeit und konstruiert
weder Policies noch Orchestrator. Er kennt keine Netzwerk-, DTO-, Mapper-,
Repository-Implementierungs-, UI-, Android- oder Compose-Details. Die
Application-Koordination verwendet ausschließlich die Ports und kann dadurch
mit den inzwischen vorhandenen In-Memory-Adaptern ebenso wie mit späteren
echten Repository-Implementierungen unverändert ausgeführt werden.

#### 5.2.2 TradePlanningApplicationService

Das Package `de.konavigator.app.application.tradeplanning` enthält den
`TradePlanningApplicationService` als dünne Application-Grenze für die
theoretische Trade-Planung. Der Datenfluss lautet zunächst:

```text
TradePlanningApplicationService
→ TradeCalculationEngine
→ TradeCalculationResult
```

Der Service besitzt exakt die bestehende `TradeCalculationEngine` als
Konstruktorabhängigkeit. Seine einzige öffentliche Fachmethode `execute` ist
synchron, übergibt den vollständigen `TradeCalculationInput` unverändert und
gibt das Engine-Resultat unverändert zurück. Der Engine-Vertrag trennt Status,
strukturierte Fehler und nullable Rechenfelder; Freitext ist nicht Bestandteil
des Resultats.

Der Service enthält keine eigene Validierung, Formel, Rundung oder
Fehlerabbildung. Für diesen vollständig durch Eingaben beschriebenen
Planungsablauf benötigt er weder Repository-Ports noch Marktdaten. Er kennt
keine UI-, Android- oder Compose-Komponenten. Er ist über
`TradePlannerViewModel`, Route und Screen an den aktiven Produktiveinstieg
angebunden.

Trade Planning und Market Data Calculation sind getrennte Use Cases: Der
Trade-Planning-Service berechnet theoretische Planungswerte aus Basiswertkurs,
Einstiegskurs, Zielhebel, Richtung, typisierter Currency-Conversion und
Bezugsverhältnis. Der
`MarketDataCalculationApplicationService` lädt dagegen ein konkretes Produkt
und dessen Quotes über die Produkt-ISIN. Beide Abläufe werden erst verbunden,
wenn dafür ein eigener, fachlich definierter Anwendungsfall vorliegt.

### 5.3 Domain Layer

Der Domain Layer enthält:

- fachliche Modelle,
- die Berechnungsengine,
- reine mathematische Funktionen,
- Validierungsregeln sowie
- fachliche Status-, Ergebnis-, Warnungs- und Fehlerobjekte.

Verbindliche Regeln:

- keine Android-Abhängigkeiten,
- keine Compose-Abhängigkeiten,
- keine Netzwerk- oder Persistenzabhängigkeiten,
- keine formatierte Anzeige,
- keine frühzeitige Rundung und
- Formeln ausschließlich gemäß `docs/FORMULAS.md`.

Die Domain verarbeitet typisierte Werte und liefert strukturierte Ergebnisse. Sie darf nicht voraussetzen, wie ein Wert in der UI eingegeben oder dargestellt wurde.

#### 5.3.1 Brokerneutrale Einstiegskursrelation

Das Package `de.konavigator.app.domain.tradeplanning` enthält die isolierte
Auswertung der Relation zwischen aktuellem Basiswertkurs und geplantem
Einstiegskurs. Der Datenfluss lautet:

```text
currentPrice + plannedEntryPrice
→ EntryPriceRelationEvaluator
→ typisierte EntryPriceRelation oder strukturierter Fehler
```

`EntryPriceRelation` unterscheidet ausschließlich `BELOW_CURRENT`,
`AT_CURRENT` und `ABOVE_CURRENT`. `EntryPriceRelationEvaluationError` enthält
die beiden Codes `INVALID_CURRENT_PRICE` und `INVALID_PLANNED_ENTRY_PRICE`.
`EntryPriceRelationEvaluationResult` trennt `Success(relation)` und
`Failure(error)` ohne Nullable-Ergebnis oder Exception als Standardfluss.

Der zustandslose `EntryPriceRelationEvaluator` erhält genau zwei `Double`-Werte
und kennt keine Handelsrichtung. Er validiert beide lokalen Operanden und
verwendet bei zwei positiven, endlichen Kursen in Version 1 exakte numerische
Gleichheit ohne Rundung, Toleranz oder Tick-Size-Annahme.

Preisrelation, Handelsrichtung, technischer Broker-Ordertyp und UI-Erklärung
sind getrennte Verantwortungen. Aus der Relation wird insbesondere keine Kauf-
oder Verkaufsorder und keine Strategie abgeleitet. Der Domainbaustein besitzt
keine Engine-, Application-, Repository-, Marktdaten-, UI-, Android- oder
Compose-Abhängigkeit. Der aktive `TradePlannerViewModel` ruft ihn vor dem
`TradePlanningApplicationService` auf und übernimmt nur sein typisiertes
Ergebnis.

### 5.4 Data Layer

Der Data Layer enthält:

- Repository-Implementierungen,
- lokale Testdaten,
- spätere APIs,
- Emittenten- und Kursdaten,
- Mapper zwischen externen Datenformaten und Domain-Modellen,
- Cache-Mechanismen und
- Zeitstempel sowie Quellenangaben.

Verbindliche Regeln:

- Die Domain kennt keine konkrete API.
- Externe DTOs werden explizit in Domainmodelle gemappt; technische
  Mappingfehler und fachliche Data-Quality-Findings bleiben unterscheidbar.
- Quelle und Zeitstempel bleiben nachvollziehbar.
- Broker und Emittenten sind Datenmerkmale und kein festes UI-Hardcoding.
- Externe DTOs gelangen nicht ungeprüft in Domain oder UI.

## 6. Empfohlene Projektstruktur

Die schrittweise Zielstruktur lautet beispielsweise:

```text
de.konavigator.app
├── presentation
│   ├── tradeplanner
│   │   ├── TradePlannerScreen.kt
│   │   ├── TradePlannerViewModel.kt
│   │   ├── TradePlannerUiState.kt
│   │   └── TradePlannerEvent.kt
│   └── components
├── application
│   └── usecase
├── domain
│   ├── model
│   ├── calculator
│   ├── availability
│   ├── compatibility
│   ├── currency
│   ├── dataquality
│   ├── freshness
│   ├── orchestration
│   ├── source
│   ├── tradeplanning
│   ├── validation
│   └── result
├── data
│   ├── repository
│   ├── local
│   ├── remote
│   └── mapper
└── core
    ├── currency
    ├── formatting
    └── error
```

Diese Struktur ist ein Zielbild. Sie wird schrittweise eingeführt und rechtfertigt kein großes einmaliges Refactoring. Neue Funktionen sollen bevorzugt bereits passend zur Zielstruktur eingeordnet werden. Bestehende Dateien bleiben während einer Migration erhalten und werden erst entfernt, wenn ihr Ersatz fachlich geprüft, getestet und vollständig angebunden ist. Eine spätere Modultrennung ist davon unabhängig und derzeit offen.

## 7. ViewModel-Konzept

Für den Trade Planner sind folgende Typen vorgesehen:

- `TradePlannerViewModel` verarbeitet Events, führt Use Cases aus und verwaltet den State.
- `TradePlannerUiState` bildet den vollständigen, anzeigbaren Zustand unveränderlich ab.
- `TradePlannerEvent` beschreibt Nutzeraktionen und fachlich relevante UI-Eingaben.
- `TradePlannerEffect` kann optional einmalige Ereignisse wie Navigation oder eine einmalige Meldung abbilden. Dauerhafte Informationen und Fehler gehören in den State, nicht in Effects.

Der `TradePlannerUiState` bildet mindestens ab:

- ausgewählten Basiswert,
- aktuellen Kurs,
- geplanten Einstieg,
- Zielhebel,
- Richtung Long oder Short,
- Broker,
- ausgewählte Emittenten,
- Ladezustand,
- Validierungsfehler,
- Berechnungsergebnis,
- Warnungen und
- Zeitstempel der verwendeten Daten.

Der Ablauf ist eindeutig: Die UI sendet Events. Das ViewModel verarbeitet sie, ruft Use Cases auf und erzeugt einen neuen State. Screens zeigen ausschließlich diesen State an. Im Screen findet keine Fachberechnung statt. Texteingaben dürfen zunächst als UI-Eingabewerte erhalten bleiben; erst eine erfolgreiche, explizite Konvertierung erzeugt typisierte Domain-Werte.

## 8. Domain-Modelle

Folgende typsichere Modelle werden empfohlen:

- `TradeDirection` als Enum mit mindestens `LONG` und `SHORT`,
- `CurrencyCode` für eindeutig bezeichnete Währungen,
- `Money` oder `Price` mit Betrag und Währung,
- `UnderlyingAsset` für Basiswertidentität und Stammdaten,
- `ProductSpecification` für unveränderliche Zertifikatsmerkmale,
- `TradeScenarioInput` für vollständige Szenarioeingaben,
- `TradeCalculationResult` für strukturierte Ergebnisse,
- `KnockoutStatus` für den fachlichen KO-Zustand,
- `ValidationError` für erwartbare ungültige Eingaben und
- `CalculationWarning` für berechenbare, aber eingeschränkte Ergebnisse.

Dabei gelten folgende Modellierungsregeln:

- Basispreis und KO-Barriere sind getrennte Felder.
- Aktueller Kurs und geplanter Einstieg sind getrennte Felder.
- Theoretischer Modellpreis, Bid und Ask sind getrennte Preisarten.
- Basiswertwährung und Produktwährung sind getrennte Angaben.
- Ein fehlender Wert wird als fehlend modelliert und niemals durch `0` ersetzt.
- Einheiten, Währung, Preisart, Quelle und Zeitbezug müssen aus Typ oder Kontext eindeutig hervorgehen.
- Rohe UI-Strings sind keine Domain-Modelle.

`KnockoutProductSpecification` ist das erste isolierte Domainmodell für ein
konkretes KO-Produkt. Es gehört nicht zum theoretischen `TradeCalculationInput`,
sondern enthält ausschließlich die statische Produktspezifikation mit getrenntem
Basispreis und getrennter KO-Barriere. Marktdaten und Produktbewertung werden in
späteren, getrennten Entwicklungsschritten modelliert und angebunden.

`TradeDirection` wird sowohl vom Produktmodell als auch durchgängig im
Presentation- und UI-Vertrag des aktiven Trade Planners verwendet. Erst der
zentrale Adapter bildet sie auf den weiterhin internen Boolean-Vertrag der
Engine ab. Die KO-Produktmodelle führen Währungen in Version 1 noch als
dokumentierte ISO-4217-Strings; der theoretische Engine-Pfad verwendet bereits
`CurrencyCode` und `CurrencyConversion`. Eine kontrollierte Migration der
Produktmodelle auf den typisierten Currency-Vertrag bleibt **OFFEN**.

Die allgemeinen Version-1-Regeln von `KnockoutProductSpecification` werden
außerhalb des Modells durch den zustandslosen
`KnockoutProductSpecificationValidator` im Domain-Package `validation`
geprüft. Das Modell bleibt eine reine Data Class ohne Konstruktorvalidierung
oder `require`-Aufrufe. Erwartbare Fehler werden als maschinenlesbare Codes ohne
UI-Texte vollständig und in stabiler Feldreihenfolge zurückgegeben; Exceptions
sind dafür kein Standardfluss.

Der Validator verändert und normalisiert seine Eingaben nicht. Spätere Mapper
oder Normalizer bleiben getrennte Komponenten. Er wird bereits vom
`MarketDataCalculationOrchestrator` aufgerufen; Engine-Mathematik, UI und
Repository-Zugriff bleiben außerhalb seiner Verantwortung. Formale ISIN- und
WKN-Prüfungen, die Prüfung gegen eine echte ISO-Währungsliste sowie
emittentenspezifische Regeln bleiben Gegenstand späterer, gesondert
freizugebender Schritte.

`KnockoutProductMarketData` ist ein separates passives Domainmodell für
veränderliche Marktdaten eines konkreten KO-Produkts. Marktdaten werden nicht
in `KnockoutProductSpecification` gespeichert; die Verbindung erfolgt zunächst
ausschließlich über `productIsin`. Bid und Ask sind unabhängig nullable, damit
auch unvollständige Quotes ohne Ersatzwerte dargestellt werden können.

Getrennte Bid- und Ask-Zeitstempel bilden getrennte Aktualisierungen ab.
`Long`-Felder mit ausdrücklicher UTC-Epoch-Millis-Semantik sind eine bewusste
Version-1-Entscheidung. Eine spätere Migration zu `Instant` oder einem
einheitlichen Zeittyp bleibt **OFFEN**. Quote-Währung und Datenquelle werden
explizit mitgeführt; es gibt keine stille EUR-Annahme.

Spread, relativer Spread, Mid-Preis, Quote-Alter und Qualitätsbewertung werden
nicht im Marktdatenmodell gespeichert, sondern später separat aus validierten
Daten abgeleitet. Das Modell bleibt eine passive Data Class ohne
Konstruktorvalidierung.

Die allgemeinen Version-1-Regeln werden außerhalb des Modells durch den
zustandslosen `KnockoutProductMarketDataValidator` geprüft. Er liefert eine
reine Liste maschinenlesbarer Fehlercodes ohne UI-Texte, sammelt alle
unabhängigen Fehler vollständig in stabiler Reihenfolge und verwendet
Exceptions nicht als Standardfluss. Vollständige Quotes, Bid-only-Quotes,
Ask-only-Quotes und vollständig leere Quotes können intern konsistent sein.
Eine leere Fehlerliste ist deshalb keine Berechnungsfreigabe.

Der Validator normalisiert keine Eingaben und prüft weder Aktualität noch die
Kompatibilität mit `KnockoutProductSpecification`. Diese Verantwortungen
liegen in bereits getrennten Compatibility-, Freshness- und
Orchestrator-Komponenten. Der Validator selbst enthält weiterhin keine
Calculator-, Engine-, UI- oder Repository-Logik.

Reine, aus Bid und Ask abgeleitete Kennzahlen liegen im Package
`de.konavigator.app.domain.calculator`. Der `MarketDataCalculator` erhält nur
die beiden numerischen Werte und kennt weder `KnockoutProductMarketData` noch
ISIN, Währung, Quelle oder Zeitstempel. Er prüft seine mathematischen
Vorbedingungen intern und liefert über einen Calculator-spezifischen Result-Typ
und ein eigenes Fehler-Enum entweder einen Wert oder genau einen blockierenden
Fehler. Das vollständige Marktdatenvalidator-Enum wird nicht wiederverwendet.

Der Calculator verwendet keine Exceptions als Standardfluss, rundet nicht und
besitzt keine Android- oder Compose-Abhängigkeit. Der
`MarketDataCalculationOrchestrator` ruft ihn erst nach Validation,
Kompatibilität, Verfügbarkeit, Freshness und Source-Freigabe auf. Calculator,
UI und Repository bleiben getrennt; weitere Marktdatenkennzahlen und
Qualitätsregeln sind eigene Folgeschritte.

Der `KnockoutProductMarketDataCompatibilityValidator` liegt im Domain-Package
`validation` und prüft ausschließlich zwei Beziehungen zwischen den weiterhin
getrennten Modellen: die exakte Produkt-ISIN und die exakte Übereinstimmung von
Produkt- und Quote-Währung. Beide Einzelmodelle werden als intern validiert
vorausgesetzt; ihre Validatoren werden weder aufgerufen noch dupliziert.

Das eigene Fehler-Enum enthält keine UI-Texte. Beide unabhängigen Fehler werden
vollständig in stabiler Reihenfolge gesammelt. Der Validator normalisiert nicht
und prüft weder Aktualität und Quellenqualität noch FX oder Quanto. Der
vorhandene `MarketDataCalculationOrchestrator` führt ihn nach den beiden
internen Validatoren aus. Calculator-, Engine-, UI- und Repository-Logik sowie
Provider-Mapping bleiben außerhalb; eine interne Produkt-ID ist **OFFEN**.

Das Domain-Package `de.konavigator.app.domain.availability` enthält den
zustandslosen `MarketDataCalculationAvailabilityEvaluator`. Er bewertet über
genau eine öffentliche Funktion `evaluate` jeweils einen der vier Typen
`PURCHASE_PRICE`, `SALE_PRICE`, `SPREAD` oder `MID`. Sein sealed Result-Typ
unterscheidet ausschließlich `StructurallyAvailable` und
`StructurallyUnavailable`; letzteres enthält eine stabile Liste der drei
Availability-Fehlercodes `MISSING_BID`, `MISSING_ASK` und
`BID_NOT_POSITIVE_FOR_SALE`.

Der Evaluator setzt eine intern gültige Produktspezifikation, intern gültige
Marktdaten und erfolgreiche Cross-Model-Kompatibilität voraus. Er ruft keine
Validatoren auf und wiederholt weder interne numerische Preisregeln noch die
durch den Marktdatenvalidator garantierte Preis-/Zeitstempel-Kohärenz. Die
einzige zusätzliche anwendungsbezogene Preisregel ist, dass Bid `0.0` für
`SALE_PRICE` nicht verfügbar ist; für `SPREAD` und `MID` bleibt dieser
strukturell gültige Wert zulässig.

Die Availability-Komponente führt keine Berechnung aus, liest keine Systemzeit
und bewertet weder Aktualität noch Quellenqualität. Sie besitzt keine Android-,
Compose-, Engine-, UI- oder Repository-Anbindung. Der vorhandene
`MarketDataCalculationOrchestrator` führt sie nach interner Validation und
Cross-Model-Kompatibilität sowie vor Freshness, Quellenpolicy und getrenntem
Calculator-Aufruf aus.

Das Domain-Package `de.konavigator.app.domain.freshness` enthält die reine
zeitliche `MarketDataFreshnessPolicy`. Ihre unveränderlichen Schwellen werden
als `MarketDataFreshnessThresholds` mit den vier expliziten Feldern
`maxBidAgeMillis`, `maxAskAgeMillis`, `maxBidAskDifferenceMillis` und
`allowedFutureSkewMillis` in den Konstruktor gegeben. Es existieren keine
Default-Schwellen; eine spätere Application- oder Composition-Schicht muss die
konfigurierten Werte bereitstellen und vor der Konstruktion an der Systemgrenze
validieren. Der Bewertungszeitpunkt wird bei jedem Aufruf explizit als UTC
Epoch Milliseconds übergeben. Die Policy liest keine Systemzeit und besitzt
genau eine öffentliche Funktion `evaluate`.

Der sealed Result-Typ unterscheidet ausschließlich `Fresh` und
`NotFresh(errors)`. Das Fehler-Enum enthält in stabiler Reihenfolge genau
`BID_TIMESTAMP_IN_FUTURE`, `STALE_BID`, `ASK_TIMESTAMP_IN_FUTURE`, `STALE_ASK`
und `BID_ASK_TIMESTAMPS_TOO_FAR_APART`. `PURCHASE_PRICE` prüft ausschließlich
den Ask-Zeitstempel, `SALE_PRICE` ausschließlich den Bid-Zeitstempel; `SPREAD`
und `MID` prüfen beide Seiten und zusätzlich deren maximale Zeitdifferenz. Alle
Grenzen sind inklusiv. Eine unzulässig zukünftige Seite unterdrückt den
paarweisen Differenzfehler, eine stale Seite dagegen nicht. Negative und
extreme Epoch-Millis bleiben zulässig; Distanzvergleiche erfolgen ohne
überlaufgefährdete Subtraktion und ohne `abs()`.

Die Policy setzt interne Validierung, Cross-Model-Kompatibilität und
strukturelle Availability voraus und ruft deren Komponenten nicht selbst auf.
Sie berechnet keine Preise und bewertet weder Quellenqualität noch
Handelszeiten. Es besteht keine Android-, Compose-, Engine-, UI- oder
Repository-Anbindung. Der vorhandene `MarketDataCalculationOrchestrator`
verbindet Validation, Compatibility, Availability, Freshness, SourcePolicy und
Calculator. `Fresh` allein ist weder eine vollständige Berechnungsfreigabe noch
eine Aussage über Handelbarkeit.

Das Domain-Package `de.konavigator.app.domain.source` enthält die
konfigurationsbasierte `MarketDataSourcePolicy`. Ein `MarketDataSourceRule`
verbindet einen exakten `sourceId` mit einem Set ausdrücklich unterstützter
`MarketDataCalculationType`-Werte. `MarketDataSourcePolicyConfig` führt diese
Regeln als Liste, erlaubt eine leere Konfiguration und lehnt doppelte exakte
Quellenschlüssel ab. Es gibt keine Default-Regeln, Wildcards, Normalisierung
oder stillschweigende Zusammenführung.

Die Policy übernimmt beim Erzeugen defensive Kopien der Regelinhalte in ein
internes Snapshot-Mapping. Nachträgliche Änderungen an externen Listen oder
Sets verändern ihr Verhalten nicht. Ihre einzige öffentliche Funktion
`evaluate` erhält genau einen CalculationType und einen `sourceId`. Eine
nicht konfigurierte Quelle erzeugt `SOURCE_NOT_CONFIGURED`; fehlt bei einer
bekannten Quelle der Typ im Set, entsteht `CALCULATION_TYPE_NOT_SUPPORTED`.
`MarketDataSourceResult` unterscheidet ausschließlich `Allowed` und
`Blocked(error)` mit genau einem Fehler.

Alle vier CalculationTypes werden unabhängig und nur durch explizite
Set-Mitgliedschaft freigegeben. Purchase und Sale implizieren weder Spread noch
Mid; Spread und Mid implizieren einander nicht. Die Policy ruft keine
Validatoren, Availability-, Freshness- oder Calculator-Komponenten auf und
prüft weder Preise, Zeitstempel, ISIN noch Währung. Quellentypen,
Latenzklassifikation, Vertrauensstufen, konkrete Provider und Provider-Mapping
bleiben außerhalb dieser Version. Es bestehen keine Netzwerk-, Android-,
Compose-, Engine-, UI- oder Repository-Abhängigkeiten.

Eine spätere Application-Schicht bezieht und validiert externe beziehungsweise
serverseitige Konfiguration, mappt sie auf die Domain-Konfiguration und erzeugt
die Policy. Netzwerktransport und Serialisierung bleiben außerhalb der
Domainpolicy. `Allowed` bestätigt nur die konfigurierte Quellenfreigabe und ist
keine vollständige Berechnungsfreigabe oder Handelbarkeitsaussage.

Das Package `de.konavigator.app.domain.orchestration` enthält die zentrale
Marktdatenorchestrierung. `MarketDataCalculationRequest` führt genau einen
CalculationType, Produktspezifikation, Marktdaten und einen expliziten
Bewertungszeitpunkt zusammen. `MarketDataCalculationValue` unterscheidet
Kaufpreis, Verkaufspreis, ein gemeinsames absolutes und relatives
Spread-Ergebnis sowie Mid-Preis. Der sealed Typ
`MarketDataCalculationOrchestrationResult` bildet die sieben blockierenden
Stufen und den Erfolg durch eigene, maschinenlesbare Untertypen ab.

Der `MarketDataCalculationOrchestrator` besitzt genau eine öffentliche Funktion
`calculate`. Seine einzigen Konstruktorabhängigkeiten sind die konfigurierbare
`MarketDataFreshnessPolicy` und `MarketDataSourcePolicy`; zustandslose
Validatoren, AvailabilityEvaluator und Calculator werden direkt aufgerufen.
Die feste Fail-Fast-Reihenfolge lautet Specification, MarketData,
Compatibility, Availability, Freshness, Source und Calculation. Bei einem
früheren Fehler wird keine spätere Stufe ausgewertet. Mehrfachfehler bleiben auf
die bestehenden Validatorstufen beschränkt.

Bestehende Fehler-Enums werden ohne neue zentrale Fehlercodes direkt in den
jeweiligen Result-Untertypen erhalten. Ein zusätzliches Stage-Enum wäre dazu
redundant und existiert nicht. Purchase und Sale übernehmen den freigegebenen
Ask beziehungsweise positiven Bid ohne Arithmetik. Spread führt die vorhandene
absolute und relative Spread-Berechnung zusammen; Mid verwendet die vorhandene
Mid-Funktion. Der Orchestrator enthält keine eigene Preis-, Spread- oder
Mid-Formel und rundet nicht zusätzlich.

Die Orchestrierung liest keine Systemzeit und hat keine Netzwerk-, Repository-,
UI-, Android- oder Compose-Abhängigkeit. `TradeCalculationEngine` und
`KoCalculator` bleiben getrennte, unveränderte Verantwortungsbereiche und
werden nicht aufgerufen. Durch explizite Konfiguration und Zeitübergabe bleibt
die Komponente lokal sowie später serverseitig nutzbar. Sie führt keine
Serialisierung durch und setzt kein Serialisierungsframework voraus.

Ob Geld- und Rechenwerte langfristig mit `Double`, `BigDecimal` oder spezialisierten Decimal-Typen umgesetzt werden, ist eine offene Architekturentscheidung. Bis dahin dürfen Typen keine fachlich falsche Genauigkeit vortäuschen.

## 9. Berechnungsengine

### `TradeCalculationEngine`

Die `TradeCalculationEngine`:

- nimmt die zusammengehörigen Eingaben eines vollständigen Berechnungsvorgangs entgegen,
- orchestriert vollständige Berechnungsvorgänge,
- validiert Eingaben und ihre fachlichen Zusammenhänge,
- legt die erforderlichen Rechenschritte fest,
- ruft reine, abgegrenzte Calculator-Komponenten auf,
- führt deren Ergebnisse zusammen,
- erzeugt fachliche Status, Warnungen und strukturierte Fehler und
- gibt ein vollständiges Berechnungsergebnis zurück und
- enthält keine UI-Formatierung.

Sie ist der zentrale fachliche Einstiegspunkt für zusammengesetzte Trade-Berechnungen. Sie enthält langfristig keine eigenen mathematischen Fachformeln und dupliziert keine Formeln ihrer Calculator-Komponenten. UI-State, Benutzertexte, Kauf- oder Verkaufsempfehlungen, Repository- und Netzwerkzugriffe sowie Android- oder Compose-Abhängigkeiten gehören nicht zu ihrer Verantwortung.

**Ist-Abweichung:** Die aktuelle `TradeCalculationEngine` berechnet die
theoretische KO-Barriere noch direkt. Absolute und prozentuale KO-Abstände
delegiert sie an `KoCalculator`; theoretischen Produktwert und theoretischen
Hebel delegiert sie an eigene reine Calculator-Komponenten. Das Resultat
verwendet strukturierte Fehler und nullable Rechenfelder, enthält aber noch
keinen eigenständigen Warnungsvertrag. Die verbleibende direkte KO-Formel und
der interne Richtungs-Boolean werden schrittweise konsolidiert.

### `KoCalculator`

Der `KoCalculator` ist die zentrale Zielkomponente für ausschließlich reine KO-spezifische Mathematik, insbesondere:

- theoretische KO-Barriere,
- absoluten KO-Abstand,
- prozentualen KO-Abstand,
- inneren Wert,
- Modellpreis,
- KO-Status,
- tatsächlichen Hebel und
- zukünftigen Hebel sowie
- weitere klar abgegrenzte mathematische KO-Produktformeln.

Einzelne Funktionen erhalten alle benötigten Werte als Parameter, verändern keinen globalen Zustand und liefern unformatierte fachliche Ergebnisse ohne vorzeitige Rundung reiner Zwischenwerte. UI-State, Benutzertexte, Kauf- oder Verkaufsempfehlungen, Ablaufsteuerung, Repository- oder Netzwerkzugriffe, Android- oder Compose-Abhängigkeiten, Anzeigeformatierung, die Umwandlung fachlicher Fehler in Benutzertexte und die Orchestrierung vollständiger Berechnungsvorgänge gehören nicht zu seiner Verantwortung.

**Ist-Abweichung:** `KoCalculator.calculateCertificatePrice` ist eine Übergangsberechnung für einen ratio-skalierten KO-Differenzwert. Sie unterstützt Long und Short, verwendet aber die KO-Barriere anstelle eines getrennten Basispreises unter der vorläufigen Annahme `B = KO` und rundet das Ergebnis innerhalb der Berechnung auf zwei Dezimalstellen. Diese interne Rundung und die Vermischung von Basispreis und KO-Barriere widersprechen der Zielarchitektur.

Der aktuelle Wert ist weder als allgemeiner innerer Wert noch als vollständiger Modellpreis oder realer Zertifikats-, Emittenten-, Bid- beziehungsweise Ask-Preis zu verstehen. Die Zielarchitektur modelliert Basispreis, KO-Barriere, inneren Wert und Modellpreis weiterhin getrennt. Die Übergangsberechnung bleibt bis zu einem gesondert freigegebenen und getesteten Ersatz unverändert.

### `PriceConverter`

Der `PriceConverter` ist langfristig ausschließlich für eindeutig definierte Währungs-, Einheiten- und gegebenenfalls klar dokumentierte preisbezogene Konvertierungen zuständig. Die Wechselkurskonvention muss im Typ oder in der API eindeutig sein; insbesondere darf die Umrechnungsrichtung nicht aus einer unbeschrifteten Zahl geraten werden. Der Converter enthält langfristig keine eigene Zertifikatspreis-, KO-Abstands- oder Hebelformel und orchestriert keine vollständigen Berechnungsvorgänge.

**Ist-Abweichung:** `PriceConverter.calculateCertificatePrice` enthält gegenwärtig noch eine eigenständige Zertifikatspreisformel. Sie wird in diesem dokumentarischen Schritt ausdrücklich weder entfernt noch verändert und ist nicht Bestandteil der langfristigen Zielverantwortung.

Für alle drei Rollen ist `docs/FORMULAS.md` die fachlich verbindliche Grundlage. Bei Widersprüchen gilt die dort dokumentierte Fachdefinition; Implementierung und Tests sind anschließend kontrolliert anzugleichen. Jede Änderung einer Formel erfordert gleichzeitig passende Tests und eine Aktualisierung von `docs/FORMULAS.md`.

## 10. Repository-Konzept

Vorgesehen sind mindestens folgende Interfaces:

- `UnderlyingRepository` liefert Basiswertstammdaten und Suchergebnisse.
- `MarketPriceRepository` liefert Kurse, Preisart, Quelle und Zeitstempel.
- `CertificateRepository` liefert reale Produktstammdaten und handelbare Produkte.
- `IssuerRepository` liefert Emittenten und zugehörige Metadaten.

Für Entwicklung und Tests kann jedes Interface zunächst eine lokale Implementierung erhalten. Spätere Remote-Implementierungen werden austauschbar hinter denselben fachlichen Schnittstellen ergänzt. ViewModels kennen keine konkrete API und greifen nicht direkt auf Data Sources zu.

Repository-Ergebnisse unterscheiden klar zwischen Erfolg, fachlichem oder technischem Fehler und gegebenenfalls Teilerfolg. Erfolgreiche Daten enthalten ihre Quelle und einen Zeitstempel. Ein leerer Suchtreffer, nicht verfügbare Daten und ein Verbindungsfehler sind unterschiedliche Zustände.

### 10.1 Application-Ports für KO-Produktdaten

Das Package `de.konavigator.app.application.repository` enthält die ersten
serverneutralen Datenzugriffsverträge für KO-Produkte. Der
`KnockoutProductSpecificationRepository` liefert ausschließlich statische
Produktspezifikationen, der `KnockoutProductMarketDataRepository`
ausschließlich die davon getrennten Marktdaten. Beide Ports besitzen genau
eine `suspend`-fähige Suche über die exakt und ohne Normalisierung übergebene
Produkt-ISIN.

Die Rückgabe erfolgt über `RepositoryResult` mit den drei Zuständen
`Success(value)`, `NotFound` und `DataAccessFailure`. Dadurch bleiben Erfolg,
nicht gefundene Daten und technische Datenzugriffsfehler ohne Nullable-
Rückgaben oder erwartbare Exceptions unterscheidbar. Die erfolgreichen
Rückgaben verwenden direkt `KnockoutProductSpecification` beziehungsweise
`KnockoutProductMarketData`; Marktdaten tragen Quelle und Zeitstempel bereits
im Domainmodell.

Die Ports definieren keine Domainregeln und kennen weder Netzwerk,
Datenbank, Provider noch konkrete Infrastruktur. Spätere lokale, remote oder
serverseitige Implementierungen sowie externe DTOs und deren Mapper liegen im
Data-Layer außerhalb dieser Interfaces.

Diese beiden Ports sind die Datenzugriffsgrundlage für den
`MarketDataCalculationApplicationService`. Das bestehende
`UnderlyingRepository` bleibt während der schrittweisen Migration ein
separater Altpfad und wird nicht für KO-Produktspezifikationen oder
KO-Produktmarktdaten wiederverwendet.

### 10.2 Read-only In-Memory-Repository-Adapter

Das Package `de.konavigator.app.data.inmemory` enthält mit
`InMemoryKnockoutProductSpecificationRepository` und
`InMemoryKnockoutProductMarketDataRepository` die ersten Implementierungen der
beiden KO-Repository-Ports. Beide Adapter erhalten bereits erzeugte
Domainmodelle als Liste und bilden beim Erzeugen einen defensiven Map-Snapshot.
Nachträgliche Änderungen an der ursprünglichen Collection verändern den
Repository-Inhalt nicht.

Der Map-Schlüssel ist jeweils die unveränderte `productIsin` des Domainmodells.
Lookups sind exakt, case- und whitespace-sensitiv und führen keine
Normalisierung, Aliasauflösung oder Fallback-Suche durch. Exakt doppelte
Produkt-ISINs werden beim Erzeugen abgelehnt, statt durch Last-write-wins
stillschweigend Daten zu verwerfen. Die Adapter sind read-only und liefern nur
`Success(value)` oder `NotFound`; einen künstlichen `DataAccessFailure`- oder
Exception-Modus gibt es nicht.

Die In-Memory-Implementierungen validieren oder korrigieren keine Domainmodelle
und enthalten keine Netzwerk-, Datenbank-, DTO-, Mapper- oder Providerlogik.
Sie enthalten keine eingebauten Produktions- oder Demodaten und werden nicht
automatisch in eine Release-, Demo- oder UI-Composition eingebunden. Sie dienen
dem deterministischen lokalen Datenfluss und ersetzen keine echte
Provideranbindung. Spätere Remote- oder persistente Repositories implementieren
dieselben Ports, ohne Application-Service oder Domain-Orchestrator zu ändern.

Der JVM-Test `MarketDataCalculationApplicationIntegrationTest` prüft den
vollständigen lokalen Pfad von den beiden realen In-Memory-Adaptern über den
`MarketDataCalculationApplicationService` und den real konfigurierten
`MarketDataCalculationOrchestrator` bis zum typisierten Applicationresult. Er
verwendet reale Validatoren, Availability-, Freshness-, Source- und
Calculator-Komponenten, aber weder UI und Android-Instrumentation noch Netzwerk
oder Datenbank. Der UI-nahe Altpfad aus `UnderlyingRepository` und
`UnderlyingTestData` bleibt davon getrennt und unverändert.

## 11. Datenquellen und API-Anbindung

Für eine spätere Anbindung gilt folgende Struktur:

```text
RemoteDataSource ─┐
                  ├─→ Repository → Domain/Application
LocalDataSource  ─┘       ↑
                         Cache

Externe Daten → Mapper → validierte Domain-Modelle
```

- `RemoteDataSource` kapselt die technische Kommunikation mit einem externen Dienst.
- `LocalDataSource` liefert Testdaten oder lokal persistierte Daten.
- Das `Repository` entscheidet abhängig vom Anwendungsfall über Quelle und Cache.
- `Mapper` übersetzen externe DTOs in interne Modelle und melden ungültige Daten.
- Der `Cache` speichert Daten nur mit Quelle, Erfassungszeit und Gültigkeitsinformation.

Die Datenmodelle müssen mindestens folgende Informationen unterstützen:

- Basiswertsuche,
- aktuelle Kurse,
- Referenzbörse,
- Währung,
- Zertifikatsstammdaten,
- Basispreis,
- KO-Barriere,
- Bezugsverhältnis,
- Emittent,
- Bid und Ask,
- Spread,
- Handelszeiten und
- Datenzeitstempel.

Folgende Entscheidungen bleiben ausdrücklich offen:

- **OFFEN:** konkrete Datenanbieter,
- **OFFEN:** rechtliche Nutzbarkeit der Daten,
- **OFFEN:** Kosten,
- **OFFEN:** Aktualisierungsfrequenz,
- **OFFEN:** eigener Server oder direkte API-Anbindung und
- **OFFEN:** Datenlizenzierung.

Vor Klärung dieser Punkte darf keine konkrete Anbieterintegration als verbindliche Architektur dargestellt werden.

## 12. Zertifikatssuche

Der Zielprozess ist:

1. Der Nutzer wählt einen Basiswert.
2. Der Nutzer definiert Long oder Short.
3. Der Nutzer gibt den geplanten Einstieg und den Zielhebel ein.
4. Die App lädt geeignete reale Produkte.
5. Für jedes Produkt wird der erwartete Preis am geplanten Einstieg berechnet.
6. Für jedes Produkt wird der zukünftige Hebel am Einstieg berechnet.
7. Die Produkte werden nach Abweichung vom Zielhebel, KO-Abstand, Spread, Liquidität und Datenqualität bewertet.
8. Die Ergebnisse werden mit Datenbasis, Annahmen und Einschränkungen transparent dargestellt.

Die Bewertung ist keine Kaufempfehlung und löst keine automatische Order aus. Kriterien und Gewichtung müssen für Nutzer nachvollziehbar sein. Reale Emittentendaten und offizielle Produktbedingungen bleiben maßgeblich; theoretische Modellwerte dürfen nicht als reale handelbare Preise ausgegeben werden.

## 13. Fehler- und Ergebnisbehandlung

Anwendungs- und Domain-Aufrufe liefern strukturierte Ergebnisse. Vorgesehen sind mindestens:

- `Success`,
- `ValidationFailure`,
- `DataUnavailable`,
- `NetworkFailure`,
- `KnockedOut` und
- `PartialResult`.

Dabei gelten folgende Regeln:

- Erwartbare Nutzereingaben führen nicht zu Exceptions.
- Fehlende Daten werden nicht als `null`, `0` oder leerer Text stillschweigend weiterverarbeitet.
- Technische Fehler und fachliche Fehler werden getrennt.
- Maschinenlesbare Fehlercodes werden gegenüber ausschließlich freien Texten bevorzugt.
- Die UI übersetzt Fehlercodes in verständliche, lokalisierbare Meldungen.
- `PartialResult` benennt genau, welche Werte belastbar sind und welche fehlen.
- `KnockedOut` ist ein fachlicher Zustand und kein normaler Preis von `0`.
- Unerwartete Programmfehler dürfen intern protokolliert werden, ohne sensible Daten offenzulegen.

## 14. Teststrategie

Die Teststrategie umfasst:

- Unit-Tests für reine Mathematik,
- Unit-Tests für Validierungsregeln,
- ViewModel-Tests für Events, State-Übergänge und Fehler,
- Repository-Tests für Mapping, Quellenwahl und Fehlerweitergabe,
- Integrationstests für den Datenfluss von Repository bis Berechnung,
- Compose-UI-Tests für Anzeige und Interaktion sowie
- Regressionstests für bereits bestätigte Formeln und Grenzfälle.

Das test-only Package `de.konavigator.app.scenarios` ergänzt die fokussierten
Unit- und Referenztests durch ein strukturiertes Mock-Daten-Szenario-Kit für
den vollständigen Vertrag der realen `TradeCalculationEngine`. Der aktuelle
Katalog umfasst 25 feste Szenarien, davon 15 Erfolgs- und 10 Fehlerfälle:

```text
Mock scenario kit
→ fixed input fixtures
→ real TradeCalculationEngine
→ fixed expected outputs
→ full result-contract verification
```

Die Fixtures enthalten feste, vorab unabhängig geprüfte Sollwerte und keine
nachgebauten Fachformeln. Weder Produktionscalculator noch ein vorgelagerter
Engine-Lauf erzeugen die Erwartungen. Das Kit verwendet keine externe
Datenquelle und keine zweite Testumgebung. Es darf ausdrücklich bestehende
Ist-Verträge charakterisieren, sofern der Szenarioname dies klar kennzeichnet;
eine solche Charakterisierung ändert oder legitimiert den Produktionsvertrag
nicht.

Vor der produktiven Nutzung externer Zertifikatsdaten kommt eine realistische
Integrationstest-Suite hinzu. Sie führt Repository-Adapter, Validierung,
Currency-, Freshness- und Bid-/Ask-Policies, Orchestrierung und strukturierte
Ergebnisse gemeinsam durch. Verbindliche Fehlerfälle sind fehlender Bid,
fehlender Ask, inkompatible Währung, großer Spread, veraltete Daten,
Handelsaussetzung, ungültige Produktdaten und ein bereits ausgelöster
Knock-out. Die Tests müssen nachweisen, dass Warnungen und Blockierungen bis zur
Presentation erhalten bleiben und keine Ersatzwerte als belastbare Ergebnisse
erscheinen.

Verbindliche Regeln:

- Jede mathematische Änderung benötigt passende Tests.
- Long und Short werden immer separat getestet.
- Grenzfälle umfassen mindestens `NaN`, positive und negative Unendlichkeit, Null- und Negativwerte, KO-Berührung, bereits erfolgten KO sowie Währungsumrechnung.
- Die Tests richten sich fachlich nach `docs/FORMULAS.md`.
- Erwartete Rundung wird nur an der vorgesehenen Anzeige- oder Tick-Size-Grenze getestet.
- Nach jedem Änderungsschritt müssen relevante Tests und der Build erfolgreich sein.

## 15. Navigation

Eine spätere Navigation kann folgende Ziele verbinden:

- Trade Planner,
- Zertifikatsergebnisse,
- Produktdetails,
- Szenarioanalyse,
- Einstellungen und
- rechtliche Hinweise.

Navigation wird erst eingeführt, wenn mindestens zwei echte Screens im aktiven Nutzerfluss benötigt werden. Im aktuellen frühen Stadium würde zusätzliche Navigation keinen ausreichenden Nutzen bieten. Die Wahl der Navigationsbibliothek und die genaue Routenstruktur erfolgen erst mit diesem Bedarf.

## 16. Dependency Injection

Dependency Injection ist derzeit nicht erforderlich. Abhängigkeiten können zunächst manuell konstruiert oder über eine kleine, nachvollziehbare Factory bereitgestellt werden. Hilt oder eine Alternative wird erst eingeführt, wenn die wachsende Anzahl von Implementierungen, Lebenszyklen oder Tests einen klaren Nutzen erkennen lässt. Ohne diesen Nutzen wird keine zusätzliche Bibliothek aufgenommen.

Unabhängig vom verwendeten Werkzeug sollen Abhängigkeiten über Konstruktoren oder explizite Parameter sichtbar bleiben. Ein späteres DI-Framework darf fachliche Abhängigkeiten nicht verbergen oder die Domain an Android binden.

## 17. Persistenz und Cache

Spätere Persistenz kann folgende Daten umfassen:

- zuletzt verwendete Basiswerte,
- Nutzereinstellungen,
- eine Watchlist,
- gespeicherte Trade-Szenarien,
- einen Kurs-Cache und
- Datenzeitstempel.

Persistenz ist noch nicht Bestandteil der aktuellen Kernphase. Vor stabilen Domain-Modellen wird keine dauerhafte Datenspeicherung eingeführt, da sonst vorläufige Strukturen verfestigt und aufwendige Migrationen erzwungen würden. Cache-Einträge müssen später Quelle, Erfassungszeit und Gültigkeit enthalten; veraltete Daten sind in der UI erkennbar zu machen.

## 18. Sicherheit und rechtliche Trennung

Für die frühe Version gelten folgende Grenzen:

- Es werden keine Broker-Zugangsdaten verarbeitet oder gespeichert.
- Es gibt keine automatische Orderausführung.
- Sensible Finanzdaten werden ohne konkrete Notwendigkeit nicht gespeichert.
- Planung und technische Analyse werden klar von Anlageberatung getrennt.
- Die UI enthält einen verständlichen rechtlichen Hinweis.
- Offizielle Produktunterlagen und Bedingungen des jeweiligen Emittenten haben Vorrang vor Modellrechnungen der App.
- Modellwerte, reale Kurse und zeitlich veraltete Daten werden sichtbar unterschieden.

Sollten später Authentifizierung, Kontodaten oder Orderfunktionen erwogen werden, benötigen sie vor der Umsetzung eine eigene Sicherheits-, Datenschutz- und Rechtsprüfung sowie eine neue ausdrückliche Architekturentscheidung.

## 19. Migrationsstrategie

Die Migration erfolgt in kleinen, überprüfbaren Schritten. Der aktuelle Stand
ist:

1. Formeln und Referenztests sind für den theoretischen Planungspfad
   stabilisiert.
2. Typsichere Domainmodelle und getrennte KO-Produkt-/Marktdatenmodelle sind
   vorhanden.
3. Einzelne Validation-, Compatibility-, Availability-, Freshness- und
   Source-Komponenten sowie die aktuelle Fail-Fast-Orchestrierung sind
   vorhanden.
4. Die theoretische Berechnungsengine verwendet den typisierten FX-/Ratio-
   Vertrag und reine Calculator-Komponenten; verbleibender Legacy-Code wird
   erst nach gesonderter Prüfung entfernt.
5. Der aktive Trade Planner ist über Composition, ViewModel, Route und
   Application Service angebunden.
6. Repository-Ports und lokale In-Memory-Adapter für KO-Produktdaten sind
   vorhanden.
7. Als nächstes entsteht der einheitliche strukturelle Data-Quality-Vertrag
   gemäß ADR-0009.
8. Externe DTOs, Mapper und reale Datenquellen werden erst auf dieser stabilen
   Grenze aufgebaut.

Es gibt keine Big-Bang-Migration. Jeder Schritt soll klein, nachvollziehbar und möglichst unabhängig prüfbar sein. Nach jedem Schritt werden Build und relevante Tests ausgeführt. Bestehende Funktionalität darf nicht verschlechtert werden. Alte Implementierungen werden erst entfernt, wenn der neue Pfad dieselbe oder eine ausdrücklich verbesserte, getestete Funktion vollständig übernimmt.

## 20. Verbindliche Architekturregeln

- UI rechnet nicht.
- Domain kennt Android nicht.
- Datenquellen sind austauschbar.
- Basispreis und KO-Barriere sind getrennt.
- Fehlende Daten sind nicht `0`.
- Es gibt keine frühzeitige Rundung.
- Jede Formeländerung aktualisiert `docs/FORMULAS.md` und die zugehörigen Tests.
- Keine neue Abhängigkeit ohne dokumentierte Begründung und klaren Nutzen.
- Keine große Refaktorierung ohne ausdrückliche Freigabe.
- Änderungen bleiben klein und nachvollziehbar.
- `ROADMAP.md` und `DEVLOG.md` bleiben aktuell.
- Kein Commit oder Push ohne ausdrückliche Freigabe.
- Reale Preise, Modellpreise, Bid und Ask werden nicht vermischt.
- Fachliche Fehler und technische Fehler bleiben unterscheidbar.
- Architekturänderungen werden in diesem Dokument nachvollziehbar aktualisiert.
- Währungsumrechnung wird ausschließlich mit explizitem Paar, Richtung, Quelle und Zeitbezug freigegeben.
- Mid ist ein analytischer Referenzwert und kein handelbarer Ersatz für fehlenden Bid oder Ask.
- Produktqualität und Berechnungszuverlässigkeit sind getrennte Konzepte.
- Jede Produktauswahl bleibt durch strukturierte Faktoren, Warnungen und Ausschlussgründe erklärbar.
- Wichtige Architekturentscheidungen werden vor Implementierung in `docs/DECISIONS.md` abgestimmt und erfasst.

## 21. Offene Architekturentscheidungen

Die folgenden Punkte sind nicht entschieden. Für jeden Punkt gilt: **OFFEN – erst entscheiden, wenn die konkrete Entwicklungsphase erreicht ist.**

| Architekturentscheidung | Status |
|---|---|
| Single-App-Modul oder spätere Modultrennung | **OFFEN – erst entscheiden, wenn die konkrete Entwicklungsphase erreicht ist.** |
| `Double` oder `BigDecimal`/Decimal-Typen | **OFFEN – erst entscheiden, wenn die konkrete Entwicklungsphase erreicht ist.** |
| `StateFlow` oder alternative State-Verwaltung | **OFFEN – erst entscheiden, wenn die konkrete Entwicklungsphase erreicht ist.** |
| Konkrete DI-Lösung | **OFFEN – erst entscheiden, wenn die konkrete Entwicklungsphase erreicht ist.** |
| Konkrete Netzwerkbibliothek | **OFFEN – erst entscheiden, wenn die konkrete Entwicklungsphase erreicht ist.** |
| Konkrete Persistenzlösung | **OFFEN – erst entscheiden, wenn die konkrete Entwicklungsphase erreicht ist.** |
| Serverarchitektur | **OFFEN – erst entscheiden, wenn die konkrete Entwicklungsphase erreicht ist.** |
| Datenprovider | **OFFEN – erst entscheiden, wenn die konkrete Entwicklungsphase erreicht ist.** |
| Caching-Strategie | **OFFEN – erst entscheiden, wenn die konkrete Entwicklungsphase erreicht ist.** |
| Synchronisation | **OFFEN – erst entscheiden, wenn die konkrete Entwicklungsphase erreicht ist.** |
| Multi-Plattform-Strategie | **OFFEN – erst entscheiden, wenn die konkrete Entwicklungsphase erreicht ist.** |
| Currency-Typ, FX-Provider, FX-Freshness und Quanto-Behandlung | **RICHTUNG AKZEPTIERT in ADR-0001; Details vor Implementierung offen.** |
| Freshness-Warnstufen und produktive Schwellen | **RICHTUNG AKZEPTIERT in ADR-0002; Details vor Implementierung offen.** |
| Zuordnung technischer Ordertypen zu Bid, Ask, Mid, Limit und Trigger | **RICHTUNG AKZEPTIERT in ADR-0003; Details vor Implementierung offen.** |
| Spread-Grenzen und Abbildung einer Handelsaussetzung | **RICHTUNG AKZEPTIERT in ADR-0004; Details vor Implementierung offen.** |
| Qualitätsscore-Skala, Faktorenormalisierung und Gewichtung | **RICHTUNG AKZEPTIERT in ADR-0005; Details vor Implementierung offen.** |
| Confidence-Skala, Aggregation und Mindestdaten | **RICHTUNG AKZEPTIERT in ADR-0007; Details vor Implementierung offen.** |

Entscheidungen werden erst getroffen, wenn konkrete Anforderungen, fachliche Grundlagen und überprüfbare Auswahlkriterien vorliegen. Eine Entscheidung wird anschließend mit Begründung, Auswirkungen, Alternativen und Migrationsbedarf in diesem Dokument sowie mit Status und Datum in `docs/DECISIONS.md` festgehalten.

## 22. Langfristige Datenqualitäts- und Explainability-Architektur

Die folgenden Bausteine sind als Architekturrichtung akzeptiert, aber noch
nicht vollständig implementiert. Sie werden nur in kleinen, separat
freigegebenen Schritten eingeführt. Die ADRs in `docs/DECISIONS.md` sind für
Status, Entscheidungsgrenze und offene Details verbindlich.

### 22.1 CurrencyPolicy

`CurrencyPolicy` bildet künftig die fachliche Grenze vor jeder
währungsübergreifenden Berechnung. Sie erhält eindeutig typisierte
Ausgangs- und Zielwährung sowie einen beschrifteten FX-Kurs und entscheidet,
ob die Konvertierung für den konkreten Bewertungszeitpunkt freigegeben ist.
Die Referenzwährung des Basiswerts und die Produktwährung bleiben getrennt.

```text
FXRateProvider → FX-Quote mit Paar, Richtung, Quelle und Zeitstempel
                                      ↓
Berechnungskontext → CurrencyPolicy → freigegebene Conversion-Anweisung
                                      ↓
                               CurrencyConverter
```

Der `FXRateProvider` ist ein Port für die Beschaffung. Der
`CurrencyConverter` ist eine reine, deterministische Rechenkomponente. Die
Policy orchestriert beide nicht zwingend selbst, definiert aber die
Voraussetzungen einer zulässigen Umrechnung. Providerfallbacks dürfen
fehlende, inkompatible oder stale FX-Daten nicht verschleiern. Siehe ADR-0001.

### 22.2 Quote-Freshness und Datenqualitätsstufen

Die vorhandene `MarketDataFreshnessPolicy` bleibt die isolierte Basis der
Zeitprüfung. Langfristig wird ihr strukturiertes Ergebnis so erweitert oder
von einer getrennten Datenqualitätspolicy eingeordnet, dass mindestens
unbeanstandete Daten, nicht blockierende Warnungen und eine blockierende Stufe
unterscheidbar sind. Jeder Zustand führt relevante Quote-Zeitstempel,
Bewertungszeitpunkt und angewandte Grenze mit.

Blockierende Freshness beendet den fachlichen Ablauf vor Quote-Auswahl,
Berechnung und Scoring. Eine Warnung darf nur dann ein Ergebnis begleiten,
wenn der fachlich freigegebene Grenzbereich dies ausdrücklich zulässt.
Konkrete Stufennamen und Schwellenwerte sind offen. Siehe ADR-0002.

### 22.3 Bid-/Ask-Policy

Die Bid-/Ask-Policy erhält einen typisierten Verwendungskontext und bereits
validierte, kompatible und frische Quotes. Sie liefert entweder eine
ausdrücklich bezeichnete Preisart mit Preis und Zeitstempel oder einen
strukturierten Blockiergrund.

```text
Kaufpreisbetrachtung    → Ask
Verkaufspreisbetrachtung → Bid
Analytischer Referenzwert → Mid aus Bid und Ask
```

Mid bleibt nicht handelbar. Die Policy verwendet weder Last noch `0` als
Fallback. Die bestehende Quote-Auswahl des
`MarketDataCalculationOrchestrator` ist die aktuelle Teilbasis; ein
vollständiges Mapping technischer Broker-Ordertypen bleibt offen. Siehe
ADR-0003.

### 22.4 Zertifikats-Qualitätsscore

Der Qualitätsscore wird erst nach erfolgreicher harter Validierung und
Datenfreigabe berechnet. Seine Zielstruktur besteht aus getrennten
Teilbeiträgen für:

- Spread,
- Emittent,
- Datenqualität,
- Aktualität,
- KO-Abstand,
- Liquidität und
- Finanzierungskosten nach späterer fachlicher Definition.

Jeder Beitrag enthält Rohwert, Normalisierungsregel, angewandtes Gewicht,
Datenstand und Wirkung auf den Gesamtscore. Blockierende Zustände werden nicht
durch einen niedrigen Score ersetzt. Skala, Gewichtung und Mindestdaten sind
offen. Siehe ADR-0005.

### 22.5 Explainable Engine

Application- und Domainresultate werden langfristig um ein strukturiertes
Erklärungsmodell ergänzt. Es wird aus denselben validierten Zwischenergebnissen
wie die Berechnung aufgebaut und enthält mindestens Hebel, Spread,
KO-Abstand, Emittent, Datenalter und Qualitätsbewertung. Zusätzlich werden
Preisart, Currency-/FX-Kontext, Warnungen, Ausschlussgründe und fehlende
Faktoren mitgeführt.

Die UI lokalisiert und formatiert diese Struktur, erzeugt aber keine eigene
fachliche Begründung. Eine Erklärung beschreibt den Rechen- und Auswahlweg und
ist keine Kauf- oder Verkaufsempfehlung. Siehe ADR-0006.

### 22.6 Confidence Score

Der Confidence Score beschreibt ausschließlich, wie belastbar eine konkrete
Berechnung anhand ihrer Eingangsdaten ist. Mögliche Beiträge sind
Vollständigkeit, Freshness, Währungs- und FX-Kompatibilität, Quellenqualität
und interne Konsistenz. Er bleibt als eigener Typ vom Zertifikats-
Qualitätsscore getrennt.

Eine blockierende Policyverletzung erzeugt kein reguläres Ergebnis mit
lediglich niedriger Confidence. Ohne ausdrückliche spätere Kalibrierung ist der
Score keine Wahrscheinlichkeit und trifft keine Aussage über Rendite,
Kursentwicklung oder Produkterfolg. Siehe ADR-0007.

### 22.7 Zielreihenfolge der fachlichen Freigabe

Der langfristige Ablauf folgt dieser Reihenfolge:

```text
Stammdatenvalidierung
→ Quote- und Currency-Kompatibilität
→ strukturelle Quote-Verfügbarkeit
→ Quote- und FX-Freshness
→ Quellen- und Handelsstatusfreigabe
→ Bid-/Ask-Auswahl
→ mathematische Berechnung
→ Confidence-Bewertung
→ Qualitätsscore
→ strukturierte Erklärung
```

Die genaue Aufteilung in Policies und Orchestratoren bleibt implementierungsnah
zu entscheiden. Die Reihenfolge drückt die verbindliche Fachregel aus, dass
blockierende Datenfehler vor Berechnung und weichem Scoring behandelt werden.

## 23. Isolierter FX-/Ratio-Produktwertvertrag

Das Package `de.konavigator.app.domain.currency` enthält als erste isolierte
Teilbasis der langfristigen Currency-Architektur zwei neue Verträge:

- `CurrencyCode` normalisiert und validiert ausschließlich syntaktische,
  dreistellige ASCII-Währungscodes. Die Prüfung behauptet nicht, dass jeder
  syntaktisch gültige Code eine offiziell existierende Währung bezeichnet.
- `CurrencyConversion` ist ein geschlossener Vertrag aus `SameCurrency` und
  `CrossCurrency`. Same-Currency führt keine numerische Rate. Cross-Currency
  verlangt verschiedene Währungen sowie eine positive, endliche Rate mit der
  eindeutigen Richtung Basiswertwährung je Produktwährung.

Der neue `TheoreticalProductValueCalculator` liegt im bestehenden
Calculator-Package. Er berechnet synchron und ohne Rundung zuerst den
ratio-skalierten Wert in Basiswertwährung und konvertiert ihn anschließend bei
Cross-Currency durch Division in die Produktwährung. Resultate und erwartbare
Fehler werden als kleine geschlossene Verträge im selben Calculator-Kontext
geführt.

Die Komponente besitzt keine Android-, Compose-, Repository-, Netzwerk-,
Coroutine-, Systemzeit-, Provider- oder MarketData-Abhängigkeit. Quelle,
Zeitstempel und Freshness einer realen FX-Quote bleiben ausdrücklich der
späteren `CurrencyPolicy` und dem `FXRateProvider` gemäß ADR-0001 vorbehalten.

Seit Schritt 23D.2 ist der Vertrag kontrolliert an den aktiven
Trade-Planning-Pfad angebunden:

```text
TradeCalculationEngine
→ KO- und Distanzberechnung
→ TheoreticalProductValueCalculator
→ typisierter FX-/Ratio-Vertrag
→ TradeCalculationResult
```

`TradeCalculationInput` verlangt Ratio und `CurrencyConversion` ausdrücklich;
es bestehen keine Engine-Defaults für diese Werte. Das Resultat führt den
ungerundeten theoretischen Wert in Basiswertwährung, den ungerundeten
theoretischen Produktwert sowie beide Ergebniswährungen. Der aktive Engine-Pfad
ruft `KoCalculator.calculateCertificatePrice` nicht mehr auf und enthält keine
Cent-Rundung.

Der Application Service bleibt ein synchroner Durchreicher. Weil der aktuelle
ViewModel-Vertrag noch keine fachlich belastbare Währungsinformation besitzt,
setzt das ViewModel zentral und sichtbar die Übergangsannahmen Ratio `0.01` und
`SameCurrency(XXX)`. `XXX` wird nicht als reale Basiswert- oder
Produktwährung ausgegeben. Die UI zeigt weiterhin ausschließlich die bereits
vorhandenen Ergebniszeilen; FX, Ratio und Währungen werden noch nicht ergänzt.

`KoCalculator.calculateCertificatePrice` und `PriceConverter` bleiben als
getrennt zu bereinigender Legacy-Code erhalten, gehören aber nicht mehr zum
aktiven Engine-Datenfluss. Siehe ADR-0008.

### 23.1 Berechneter theoretischer Hebel am geplanten Einstieg

Seit Schritt 23D.3 ergänzt der reine `TheoreticalLeverageCalculator` den
aktiven theoretischen Planungspfad. Der Ablauf lautet:

```text
plannedEntryPrice + ratio + CurrencyConversion
→ underlyingExposureInProductCurrency

underlyingExposureInProductCurrency ÷ theoreticalProductValue
→ calculatedTheoreticalLeverageAtEntry
```

Der Calculator liegt im Engine-/Calculator-Bereich und erhält den ungerundeten
Produktwert aus `TheoreticalProductValueCalculator`. Weder ViewModel noch UI
berechnen einen Hebel. Ratio und FX werden für das Exposure mit derselben
Konvention wie für den Produktwert angewendet und heben sich im idealen Modell
algebraisch auf. Es findet vor der Hebelberechnung keine Rundung statt.

`TradeCalculationResult` führt `targetLeverage`,
`underlyingExposureInProductCurrency` und
`calculatedTheoreticalLeverageAtEntry` getrennt. Der Zielhebel bleibt der
validierte Eingabewert; der berechnete theoretische Hebel wird aus Exposure und
Produktwert rückgerechnet. Ein unvollständiges Erfolgsresult wird von der
Presentation als inkonsistent abgelehnt.

Die bestehende Ergebnisbox zeigt ausschließlich Zielhebel und berechneten
theoretischen Hebel als zwei neue neutrale Zeilen. Exposure, Ratio, FX und
Währungen bleiben interne Transparenzwerte. Ein tatsächlicher Hebel eines
realen Produkts auf Basis von Bid, Ask oder anderen Marktdaten bleibt dem
späteren Produktpfad vorbehalten.

## 24. Einheitliche Data-Quality-Zielgrenze

ADR-0009 legt für die schrittweise Konsolidierung das zukünftige Domain-Package
`de.konavigator.app.domain.dataquality` fest. Es ersetzt keine vorhandene
Fachkomponente, sondern schafft einen anwendungsneutralen Vertrag für
strukturierte Bewertungen. Der langfristige Datenfluss lautet:

```text
External DTO
→ Mapper
→ Domain models
→ existing validators and policies
→ DataQualityAssessment
→ Application orchestration
→ calculators only after approval
```

Remote-Provider, externe DTOs und Mapper sind noch nicht implementiert. Die
beiden Domainmodelle, ihre strukturellen Validatoren, der
CompatibilityValidator, AvailabilityEvaluator, FreshnessPolicy, SourcePolicy,
die aktuelle Marktdatenorchestrierung und lokale In-Memory-Adapter sind dagegen
bereits vorhanden.

### 24.1 Vertrag und Zuständigkeiten

Der Data-Quality-Vertrag beschreibt:

- einen geschlossenen Gesamtstatus `PASSED`, `WARNING` oder `BLOCKED`,
- maschinenlesbare Findings mit Severity, Ursprung und bestehendem Fehlercode,
- strukturierte Evidenz über bewertete Modelle und Prüfbereiche sowie
- eine deterministische, stabile Reihenfolge.

`PASSED` bedeutet nur, dass alle im konkreten Assessment enthaltenen Prüfungen
bestanden wurden. Es ist keine Aussage über Handelbarkeit, Rendite,
Produktattraktivität oder die Vollständigkeit künftig möglicher Prüfungen.
`BLOCKED` verhindert den nachgelagerten Ablauf, soweit die jeweilige
Application-Freigabe diese Prüfung verlangt. `WARNING` bereitet eine spätere
Erweiterung vor, ist aber ohne ausdrücklich akzeptierte Fachregel niemals
implizit zu erzeugen.

Data-Quality-Status, Confidence Score und Zertifikats-Qualitätsscore sind
getrennte Konzepte:

- Data Quality bewertet konkrete Daten gegen explizite Regeln.
- Confidence bewertet später die Belastbarkeit einer freigegebenen Berechnung.
- Produktqualität bewertet später Produkte anhand gesondert freigegebener
  Faktoren.

Keines dieser Konzepte ist eine Kauf- oder Verkaufsempfehlung.

### 24.2 Bestehende Komponenten bleiben Single Source of Truth

Die bestehenden Komponenten behalten ihre Grenzen:

- `KnockoutProductSpecificationValidator` prüft ausschließlich die interne
  Struktur der Produktspezifikation.
- `KnockoutProductMarketDataValidator` prüft ausschließlich die interne
  Struktur der Marktdaten.
- `KnockoutProductMarketDataCompatibilityValidator` prüft ausschließlich die
  Beziehungen zwischen beiden Modellen.
- `MarketDataCalculationAvailabilityEvaluator` prüft die für einen
  CalculationType erforderliche Quote-Verfügbarkeit.
- `MarketDataFreshnessPolicy` bewertet Zeitstempel gegen explizite Schwellen und
  einen expliziten Bewertungszeitpunkt.
- `MarketDataSourcePolicy` bewertet Quelle und unterstützten CalculationType
  gegen explizite Konfiguration.

Das neue Package verschiebt und dupliziert keine dieser Regeln. Ein
delegierender Validator darf bestehende Resultate nur in den gemeinsamen
Vertrag abbilden. Die Data-Quality-Schicht darf vorhandene Validatoren und
Policies koordinieren, Findings typisieren und kategorisieren, Warnungen und
Blockierungen strukturiert ausgeben, relevante Evidenz mitführen und
deterministisch fail closed entscheiden.

Sie darf keine Produktwerte berechnen, KO-, Hebel- oder Spreadformeln
enthalten beziehungsweise duplizieren, DTOs mappen, Werte normalisieren oder
ersetzen, Repositories oder APIs aufrufen, Systemzeit direkt lesen, Provider
auswählen, UI-Texte erzeugen, Produkte bewerten oder ranken sowie Confidence
Score und Produktqualität vermischen. Formatierung, Scoring und stille
Defaults gehören ebenfalls nicht zu ihrer Verantwortung.

### 24.3 Aktiver struktureller Vertrag seit Schritt 25A

Das Package `de.konavigator.app.domain.dataquality` enthält den aktiven
strukturellen V1-Vertrag und den zustandslosen
`KnockoutProductDataQualityValidator`. Der Datenfluss lautet:

```text
KnockoutProductSpecification
+ KnockoutProductMarketData
→ existing structural validators
→ existing compatibility validator
→ KnockoutProductDataQualityValidator
→ DataQualityAssessment
```

Der Validator delegiert an:

1. `KnockoutProductSpecificationValidator`,
2. `KnockoutProductMarketDataValidator`,
3. `KnockoutProductMarketDataCompatibilityValidator`.

Alle neun Spezifikations-, zehn Marktdaten- und zwei Kompatibilitätsfehler
werden exhaustiv jeweils auf einen eigenen stabilen `DataQualityFindingCode`
abgebildet. Jedes Finding führt zusätzlich `DataQualitySeverity`,
`DataQualityComponent` und `DataQualityCategory`; Version 1 erzeugt
ausschließlich `BLOCKING`. Der eigene Code bewahrt die ursprüngliche
Fehleridentität ohne Freitext oder zusätzliche untypisierte `sourceCode`-
Duplikation.

Spezifikationsfindings stehen stabil vor Marktdatenfindings. Die Cross-Model-
Prüfung wird nur ausgeführt, wenn beide Einzelmodelle intern gültig sind, und
ihre Findings folgen zuletzt. Sind alle ausgeführten Prüfbereiche fehlerfrei,
entsteht `PASSED` mit leerer Findings-Liste. Mindestens ein strukturelles
Finding erzeugt `BLOCKED`. `WARNING` ist im Typvertrag erlaubt, wird in
Version 1 aber nicht produziert.

`DataQualityAssessment` sichert seine Statusinvarianten konstruktiv ab und
übernimmt einen unveränderlichen defensiven Snapshot der Findings. Gleiche
Eingaben erzeugen dasselbe Assessment. Der Validator liest keine Systemzeit,
mutiert oder normalisiert keine Eingaben, führt keine Berechnung aus und
korrigiert keine Daten.

Weiterhin nicht Bestandteil dieses strukturellen V1-Vertrags sind:

- neue Spread- oder Freshness-Schwellen,
- FX-, Broker-, Handelsstatus- oder Providerregeln,
- Application-, Repository-, DTO-, Mapper-, UI- oder Navigation-Migration,
- Confidence, Produktqualität, Scoring oder Ranking und
- Änderungen an Engine-Mathematik oder `docs/FORMULAS.md`.

### 24.4 Aktive Orchestrator-Integration seit Schritt 25B

Der `MarketDataCalculationOrchestrator` verwendet das strukturelle Assessment
als erste fachliche Freigabestufe:

```text
KnockoutProductSpecification
+ KnockoutProductMarketData
→ KnockoutProductDataQualityValidator
→ DataQualityAssessment
→ BLOCKED: stop
→ PASSED/WARNING: existing availability
→ existing freshness
→ existing source policy
→ orchestration result
```

`BLOCKED` beendet die Orchestrierung fail closed, bevor Availability,
Freshness, Source Policy oder Berechnung ausgewertet werden. `PASSED` führt in
den bisherigen Ablauf. `WARNING` wird vom aktuellen Validator nicht erzeugt;
der Orchestrator behandelt es defensiv als nicht blockierend, überspringt aber
keine der vorhandenen Folgeprüfungen.

Jeder Orchestrator-Resultattyp führt das vollständige, nicht-nullbare
`DataQualityAssessment`. Ein strukturelles Blockresultat bewahrt sämtliche
Findings unverändert. Spätere Availability-, Freshness-, Source- oder
Calculation-Blockierungen behalten das zuvor ermittelte `PASSED`- oder
`WARNING`-Assessment und überschreiben es nicht. Die 21 Finding-Codes werden
nicht als Orchestrator-Fehler dupliziert.

Der Orchestrator bleibt reiner Koordinator und erzeugt, klassifiziert oder
normalisiert keine Findings. Availability-, Freshness- und Source-Regeln
bleiben in ihren bestehenden Komponenten. Repositorys, APIs und Domainmodelle
sind unverändert; eine Weitergabe des Assessments an sichtbare UI ist noch
nicht Bestandteil dieses Schritts.
