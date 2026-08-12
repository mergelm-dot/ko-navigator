# KO Navigator Roadmap

## Aktueller Entwicklungsstand

### Stand nach 26A.23e

Der providerneutrale Kandidaten- und Selection-Pfad ist technisch vollständig
bis zu einer separaten Android-UI verdrahtet. Er umfasst Produktkatalog,
Broker-Verfügbarkeit, Emittentenauswahl, Marktdaten, Data Quality,
Calculation Availability, Freshness und Source-Freigabe jeweils mit Gate,
Kandidatenberechnung, Zielhebelplanung, Einstiegsberechnung bestehender Produkte,
Target Deviation, Target Fit, Ranking sowie Primary und bis zu zwei
Alternativen. Auf dieser Basis sind Calculation-, Planned-Entry- und vollständige
Selection-Pipeline, Presentation Mapping, Selection ViewModel, UI und
Composition vorhanden.

Aktueller letzter abgeschlossener Schritt:

`26A.23e – physischer Selection-Demo-Launcher`

Der neue Selection-Pfad wurde mit kontrollierten synthetischen Debug-Daten auf
einem physischen Android-Gerät sichtbar bestätigt. Er ist weiterhin parallel
zum normalen App-Pfad; `MainActivity` verwendet ihn noch nicht.

Aktueller Debug-JVM-Teststand: `2294/2294 erfolgreich` (0 Fehlschläge,
0 Fehler, 0 übersprungen).

Bereits fertig sind die providerneutrale Architektur, Berechnungen mit
vorhandenen Produktspezifikationen, die tatsächliche KO-Barriere eines
existierenden Produkts, der tatsächlich berechnete Hebel am geplanten Einstieg,
der Vergleich mit theoretischem Zielhebel, die Abweichungsberechnung, Target Fit,
Auswahl und die technische Darstellung eines Ergebnisses im kontrollierten
Debug-Pfad.

Noch nicht vollständig fertig sind belastbare produktive Zertifikats- und
Katalogdaten, reale Broker-Verfügbarkeit, ein aktueller produktiver
Bid-/Ask-Preisfluss, ein produktiver FX-Provider für Cross-Currency,
Finanzierungskosten, Aufgeld/Premium, Tick-Size-/produktbezogene Rundung und die
finale Orderlogik. Ein vollständiger Live-Zertifikatshandelspfad besteht daher
noch nicht.

## Phase 1 – Fundament

- [x] Android-/Compose-Projektstruktur
- [x] Lokale Basiswert-Testdaten
- [x] `UnderlyingRepository` und lokale Suchmaschine
- [x] `UnderlyingSearchField`
- [x] Basiswertsuche in den aktiven `TradePlannerScreen` eingebunden
- [x] Aktiver Trade-Planner-Pfad über Composition, ViewModel, Route und Screen
- [x] Produktstammdaten und veränderliche Produktmarktdaten als getrennte Modelle
- [x] Repository-Ports und read-only In-Memory-Adapter für KO-Produktdaten
- [ ] Externe DTOs und Mapper
- [ ] Reale Produkt-, Marktpreis- und FX-Provider
- [ ] Automatischer Kursabruf

## Phase 2 – Theoretische Berechnungsengine

Für den derzeitigen theoretischen Planungsvertrag umgesetzt:

- [x] Long- und Short-Richtung typisiert bis zur UI
- [x] Theoretische KO-Barriere aus Einstieg, Zielhebel und Richtung
- [x] Absoluter und prozentualer KO-Abstand
- [x] Explizites Bezugsverhältnis
- [x] Typisierter Same-/Cross-Currency-Vertrag mit eindeutiger FX-Richtung
- [x] Ungerundeter theoretischer Produktwert
- [x] Berechneter theoretischer Hebel am geplanten Einstieg
- [x] Strukturierte Engine- und Presentation-Fehler
- [x] 25 feste Referenz-, Grenz- und Fehlerszenarien
- [x] Bestehendes KO-Produkt am geplanten Einstieg mathematisch berechnen
- [x] Tatsächlichen Produkthebel am geplanten Einstieg berechnen
- [x] Tatsächliche KO-Barriere getrennt von theoretischer Zielbarriere behandeln
- [x] Zielhebelabweichung berechnen
- [x] Barrierenabweichung berechnen
- [x] Absolute und relative Abweichungen transparent bereitstellen
- [x] Technischen Target Fit mit externen Toleranzen berechnen

Für einen realen Produktpfad weiterhin offen:

- [ ] Produktive Bid-/Ask-Ausführung
- [ ] Finanzierungskosten fachlich definieren
- [ ] Premium beziehungsweise Aufgeld fachlich definieren
- [ ] Tick Size und produktbezogene Rundung fachlich definieren
- [ ] Finale Gewinn-/Verlustrechnung im realen Produktpfad vervollständigen

## Phase 3 – Zertifikatssuche

### Providerneutraler Kandidaten- und Selection-Pfad

Status: Technischer/Synthetic-Debug-Meilenstein erreicht.

- [x] Produktspezifikationen in den Kandidatenpfad übernehmen
- [x] Broker-Verfügbarkeit berücksichtigen
- [x] Emittentenauswahl berücksichtigen
- [x] Data Quality berücksichtigen
- [x] Availability berücksichtigen
- [x] Freshness berücksichtigen
- [x] Source-Freigabe berücksichtigen
- [x] Zielhebel für Kandidaten berechnen
- [x] Bestehende Produkte am geplanten Einstieg berechnen
- [x] Ziel-/Ist-Abweichungen berechnen
- [x] Erfolgreiche Deviation-Ergebnisse freigeben
- [x] Target-Fit-Calculator bereitstellen
- [x] Target Fit auf alle freigegebenen Kandidaten anwenden und freigeben
- [x] Kandidaten deterministisch nach Target Fit ranken
- [x] Primary und bis zu zwei Alternativen aus der bestehenden Reihenfolge
  auswählen
- [x] Target-Selection- und Planned-Entry-Selection-Pfad orchestrieren
- [x] Calculation Pipeline bis zu erfolgreichen Kandidaten orchestrieren
- [x] Currency Conversion auf erfolgreiche Kandidaten anwenden
- [x] Vollständige Selection Pipeline providerneutral orchestrieren
- [x] Selection-Ergebnisse in stabile Presentation-Modelle abbilden
- [x] Paralleles Selection ViewModel, Route und Compose-UI bereitstellen
- [x] Selection Composition mit providerneutralen Abhängigkeiten verdrahten
- [x] Remote-Adapter für Produktspezifikationskatalog und Broker-Verfügbarkeit
  bereitstellen
- [x] Lokalen HSBC-Research-Katalogprovider für kontrollierte Debug-Daten
  bereitstellen
- [x] Debug-Selection-Composition und physischen Selection-Demo-Launcher mit
  kontrollierten synthetischen Daten bereitstellen und auf einem Android-Gerät
  bestätigen

Noch offen:

- [ ] Belastbare produktive Zertifikats-/Katalogversorgung anbinden
- [ ] Reale Broker-Verfügbarkeitsquelle anbinden
- [ ] Realen aktuellen Produktmarktpreisfluss anbinden
- [ ] Produktiven FX-Provider für Cross-Currency anbinden
- [ ] Neuen Selection-Pfad erst nach belastbarer Datenversorgung im normalen
  App-/`MainActivity`-Pfad aktivieren

## Ergebnisziel der ersten Version

Nach Eingabe von Basiswert, Broker, Zielhebel und gewünschtem Basiswertkurs
beziehungsweise Einstiegskurs soll die App zunächst ein bestes Zertifikat
ermitteln und anzeigen. Die geplante erste Ergebnisanzeige umfasst Emittent, WKN,
berechneten Zertifikatspreis am geplanten Einstieg, tatsächlich berechneten Hebel,
reale KO-Barriere, Abweichung zum Zielhebel und technische Target-Fit-Information.

Optional können später bis zu zwei Alternativen angezeigt werden. WKN und Preis
sollen separat schnell kopierbar sein. Die Anzeige ist keine Kaufempfehlung.

## Phase 4 – UI-Verfeinerung

### Asset-Info im TradingView-Stil

Status: Geplant

Darstellung nach Auswahl eines Basiswertes:

🟩 NVIDIA

NVDA • NASDAQ • USD

Ziele:

- kleines Firmenlogo links
- Firmenname groß
- Ticker, Börse und Währung kompakt darstellen
- professioneller TradingView-/Broker-Look
- Umsetzung erst nach Stabilisierung des realen Produkt- und Datenpfads

## Phase 5 – Verlässliche Markt- und Währungsdaten

Status: Teilbasis vorhanden, reale Anbindung offen

Priorität: **P0 – Voraussetzung für belastbare reale Produktberechnungen**

- [x] Basiswert- und Produktwährung im theoretischen Engine-Vertrag typisiert
- [x] Reiner, providerfreier Same-/Cross-Currency-Rechenvertrag
- [x] Marktdaten mit getrenntem Bid/Ask, Quelle und Zeitstempeln modelliert
- [x] Explizite blockierende Freshness- und Source-Policies als Teilbasis
- [x] Providerneutralen `FxRateProvider`-Port sowie
  `CurrencyConversionPolicy` als Freigabegrenze bereitstellen
- [x] Currency Conversion für erfolgreiche Kandidaten mit typisierten
  Policy-Ergebnissen anwenden
- [ ] Austauschbaren produktiven `FxRateProvider` anbinden
- [ ] FX-Paar, Richtung, Quelle und Zeitstempel realer Umrechnungen nachweisen
- [ ] Fachlich freigegebene Freshness-Warnstufen ergänzen
- [ ] Kontextabhängige Bid-/Ask-Policy für reale Produktabläufe ergänzen
- [ ] Konkrete Schwellenwerte und Ordertyp-Zuordnungen gesondert freigeben

Architekturgrundlage: [ADR-0001](docs/DECISIONS.md#adr-0001--currencypolicy-als-verbindliche-währungsgrenze), [ADR-0002](docs/DECISIONS.md#adr-0002--quote-freshness-mit-warn--und-blockierstufen), [ADR-0003](docs/DECISIONS.md#adr-0003--kontextabhängige-bid-ask-policy) und [ADR-0008](docs/DECISIONS.md#adr-0008--typisierter-fx-ratio-produktwertvertrag).

## Phase 6 – Datenqualität und Robustheit

Status: Struktureller V1-Vertrag im Marktdatenorchestrator aktiv; Data Quality
ist im providerneutralen Kandidaten-/Application-Pfad integriert. Die finale
vollständige UI-Darstellung und Nutzerkommunikation bleiben offen.

Priorität: **P0 – nächster fachlich-technischer Schwerpunkt**

Vorhandene Teilbasis:

- [x] Strukturelle Produktspezifikationsvalidierung
- [x] Strukturelle Marktdatenvalidierung
- [x] Produkt-/Marktdaten-Kompatibilitätsprüfung
- [x] Anwendungsbezogene Quote-Verfügbarkeit
- [x] Blockierende Freshness-Prüfung mit explizitem Bewertungszeitpunkt
- [x] Fail-closed Source-Policy
- [x] Siebenstufige Fail-Fast-Marktdatenorchestrierung
- [x] Lokaler Application-Integrationspfad über In-Memory-Repositories

Nächste Schritte:

- [x] Einheitlichen, strukturellen Data-Quality-Vertrag mit Status, Findings und
  prüfbaren Evidenzen einführen
- [x] Bestehende Regeln über einen delegierenden Validator koordinieren, ohne sie
  zu verschieben oder zu duplizieren
- [x] Blocking Findings für die bereits vorhandenen strukturellen Regeln abbilden
- [x] `WARNING` nur als Vertragsoption vorbereiten; Schwellen und Regeln separat
  fachlich freigeben
- [x] Strukturelles Assessment als erste, fail-closed Freigabestufe in den
  bestehenden Orchestrator integrieren
- [ ] Integrationstests für fehlende Quotes, inkompatible Währungen, alte Quotes,
  Quellenfehler, Handelsaussetzung und ausgelösten Knock-out erweitern
- [ ] Warnungen, Blockierungen und Teilresultate später bis zur UI nachweisen

Architekturgrundlage: [ADR-0004](docs/DECISIONS.md#adr-0004--realistische-robustheits-integrationstests) und [ADR-0009](docs/DECISIONS.md#adr-0009--einheitlicher-data-quality-vertrag-über-bestehenden-validatoren-und-policies).

## Phase 7 – Transparente Produktqualität

Status: Langfristig geplant

Priorität: **P1 – nach belastbarer Daten- und Berechnungsfreigabe**

- [ ] Zertifikats-Qualitätsscore aus fachlich freigegebenen Faktoren konzipieren
- [ ] Finanzierungskosten erst nach eigener fachlicher Definition ergänzen
- [ ] Explainable Engine mit Beiträgen, Ausschlussgründen, Datenbasis und Annahmen
  bereitstellen
- [ ] Confidence Score strikt von Data-Quality-Status, Produktqualität und
  Attraktivität trennen
- [ ] Gewichte, Normalisierung, Mindestdaten und Schwellenwerte vor
  Implementierung fachlich freigeben
- [ ] Sicherstellen, dass Scores weder Kauf- noch Verkaufsempfehlungen darstellen

Architekturgrundlage: [ADR-0005](docs/DECISIONS.md#adr-0005--mehrdimensionaler-zertifikats-qualitätsscore), [ADR-0006](docs/DECISIONS.md#adr-0006--explainable-engine-für-produktauswahl-und-berechnung) und [ADR-0007](docs/DECISIONS.md#adr-0007--confidence-score-für-die-berechnungszuverlässigkeit).

## Verbindliche Entwicklungsreihenfolge

1. Theoretische Engine – abgeschlossen und stabil.
2. Data-Quality-/Availability-/Freshness-/Source-Grundlagen – umgesetzt.
3. Providerneutraler Produktkandidatenpfad – umgesetzt.
4. Bestehende Produkte am geplanten Einstieg berechnen – umgesetzt.
5. Ziel-/Ist-Abweichungen – umgesetzt.
6. Target Fit, Kandidatenfreigabe, Ranking sowie Primary und Alternativen –
   umgesetzt.
7. Vollständige End-to-End-Selection-Pipeline inklusive Currency Conversion –
   umgesetzt.
8. Parallele Selection-UI und Debug-Composition – umgesetzt und auf einem
   physischen Android-Gerät mit synthetischen Daten bestätigt.
9. Produktive externe Datenprovider schrittweise anbinden: zuerst belastbare
   Produkt-/Katalogquelle, dann reale Broker-Verfügbarkeit, aktueller
   Marktpreisfluss und produktiver Cross-Currency-FX-Provider.
10. Erst danach den neuen Selection-Pfad im normalen App-/`MainActivity`-Pfad
    aktivieren.
11. Später Qualitätsranking, Spread, Premium, Finanzierungskosten und weitere
    Produktdetails verfeinern.

## Aktueller Entwicklungsblock

### Abgeschlossen

`26A.19b bis 26A.23e – Selection-Pfad bis zum physischen Debug-Demo-Launcher`

### Nächste Phase

Produktive externe Datenversorgung schrittweise anbinden.

Priorität:

1. Belastbare Produkt-/Katalogquelle.
2. Reale Broker-Verfügbarkeit.
3. Realer aktueller Marktpreisfluss.
4. Produktiver FX-Provider für Cross-Currency.
5. Erst danach den neuen Selection-Pfad im normalen App-Pfad aktivieren.

## Meilenstein – Technische/Synthetic-Debug-Selection

**Erreicht.** Aus `Basiswert + Broker + Zielhebel + geplantem Basiswertkurs`
erreicht die neue Selection Pipeline im separaten Debug-Pfad die sichtbare
Android-UI. Der Ablauf wurde auf einem physischen Android-Gerät mit
kontrollierten synthetischen Debug-Daten bestätigt und liefert mindestens
Emittent, WKN, berechneten Zertifikatspreis, tatsächlichen Hebel und reale
KO-Barriere.

## Meilenstein – Produktive Live-Selection

**Noch nicht erreicht.** Der technische/Synthetic-Debug-Meilenstein ist keine
produktive Live-Zertifikatsversorgung. Vor einer Aktivierung im normalen
App-/`MainActivity`-Pfad fehlen insbesondere belastbare Produkt-/Katalogdaten,
reale Broker-Verfügbarkeit, aktuelle Produktmarktpreise und ein produktiver
FX-Provider für Cross-Currency.
