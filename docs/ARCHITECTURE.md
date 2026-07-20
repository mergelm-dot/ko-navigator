# KO Navigator – Technische Architektur

## 1. Zweck des Dokuments

Dieses Dokument definiert die verbindliche Zielarchitektur des KO Navigators. Es ergänzt `ROADMAP.md`, `DEVLOG.md`, `AGENTS.md` und `docs/FORMULAS.md`. Künftige Architekturänderungen müssen auch hier nachvollziehbar dokumentiert werden, damit Ist-Zustand, Zielbild und Migrationsweg nicht auseinanderlaufen.

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

Der KO Navigator ist aktuell eine Android-App mit Jetpack Compose und einem App-Modul. `MainActivity` ist der Einstiegspunkt und zeigt `TradePlannerScreen` als aktive Hauptoberfläche an. Die Basiswertsuche arbeitet mit lokalen Testdaten über `UnderlyingRepository`, `UnderlyingSearchEngine` und `UnderlyingSearchField`.

Für Berechnungen existieren derzeit `TradeCalculationEngine`, `KoCalculator` und `PriceConverter`. Die Berechnungsengine ist noch nicht vollständig an die aktive UI angebunden. Gleichzeitig bestehen mehrere teilweise parallele Rechenpfade: Die aktive Oberfläche führt einzelne Berechnungen selbst aus, `CalculatorScreen` greift direkt auf `KoCalculator` zu, `TradeCalculationEngine` orchestriert einen weiteren Ablauf, und `PriceConverter` enthält gegenwärtig ebenfalls eine Zertifikatspreisberechnung. Diese Überschneidungen sind schrittweise zu konsolidieren.

Der aktuelle Stand umfasst außerdem:

- keine ViewModels,
- keine Dependency Injection,
- keine Navigation,
- keine Netzwerk- oder Persistenzschicht,
- lokale, fest hinterlegte Basiswert- und Emittentendaten,
- UI-Zustand überwiegend direkt in Composables und
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
- Externe Daten werden vor der Übergabe an die Domain validiert.
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

**Ist-Abweichung:** Die aktuelle `TradeCalculationEngine` berechnet die theoretische KO-Barriere und den prozentualen KO-Abstand noch direkt. Außerdem liefert sie derzeit Freitextmeldungen statt vollständig strukturierter Fehler und Warnungen. Diese Abweichungen werden schrittweise konsolidiert und sind nicht Teil der hier festgelegten Zielverantwortung.

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

Die Migration erfolgt in kleinen, überprüfbaren Schritten:

1. Formeln und Tests stabilisieren.
2. Typsichere Domain-Modelle einführen.
3. Zentrale Validierung aufbauen.
4. Berechnungsengine konsolidieren.
5. ViewModel einführen.
6. Trade Planner an das ViewModel und die Use Cases anbinden.
7. Alte parallele Rechenpfade erst nach fachlicher und technischer Prüfung entfernen.
8. Repository-Interfaces einführen.
9. Lokale Zertifikatstestdaten über die Interfaces bereitstellen.
10. Später externe Datenquellen anbinden.

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

Entscheidungen werden erst getroffen, wenn konkrete Anforderungen, fachliche Grundlagen und überprüfbare Auswahlkriterien vorliegen. Eine Entscheidung wird anschließend mit Begründung, Auswirkungen, Alternativen und Migrationsbedarf in diesem Dokument festgehalten.
