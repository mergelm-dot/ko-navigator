# KO Navigator Roadmap

## Aktueller Entwicklungsstand

### Stand nach 26A.19a

Der providerneutrale Produktberechnungspfad ist weit fortgeschritten. Bereits
vorhanden sind Produktkatalog, Broker-Verfügbarkeit, Emittentenauswahl,
Marktdaten, Data Quality mit Gate, Calculation Availability mit Gate, Freshness
mit Gate, Source-Freigabe mit Gate, Marktdatenberechnung mit Gate, theoretischer
Zielhebel-Plan mit Gate, die Einstiegsberechnung bestehender KO-Produkte mit Gate,
Ziel-/Ist-Abweichungen mit Gate sowie ein technischer Target-Fit anhand externer
Toleranzen.

Aktueller letzter abgeschlossener fachlicher Schritt:

`26A.19a – ExistingKnockoutProductTargetFitCalculator`

Der Target-Fit prüft transparent und getrennt die relative Hebelabweichung sowie
die Barrierenabweichung relativ zum geplanten Einstieg. Feste Toleranzen sind
nicht in der Engine hartcodiert.

Aktueller JVM-Teststand: `2112/2112 erfolgreich`.

Bereits fertig sind die providerneutrale Architektur, Berechnungen mit
vorhandenen Produktspezifikationen, die tatsächliche KO-Barriere eines
existierenden Produkts, der tatsächlich berechnete Hebel am geplanten Einstieg,
der Vergleich mit theoretischem Zielhebel, die Abweichungsberechnung und die
technische Target-Fit-Prüfung.

Noch nicht vollständig fertig sind echte produktive Live-Provider, die
vollständige reale Zertifikatsversorgung aller gewünschten Emittenten, ein
belastbarer produktiver Bid-/Ask-Preisfluss, die finale produktive FX-Anbindung,
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

### Providerneutraler Kandidatenpfad – weit fortgeschritten

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

Noch offen:

- [ ] Target Fit auf alle freigegebenen Kandidaten anwenden (`26A.19b`)
- [ ] Target-Fit-Ergebnisse fachlich freigeben oder filtern
- [ ] Kandidaten transparent vergleichen
- [ ] Bestes Zertifikat auswählen
- [ ] Bis zu zwei Alternativen bereitstellen
- [ ] Vollständige Produktpipeline zusammensetzen
- [ ] Reale Zertifikatsdaten aus produktiven Providern laden

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
- [ ] `CurrencyPolicy` als Freigabegrenze für reale FX-Daten vervollständigen
- [ ] Austauschbaren `FXRateProvider` anbinden
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
3. Providerneutraler Produktkandidatenpfad – weit fortgeschritten.
4. Bestehende Produkte am geplanten Einstieg berechnen – umgesetzt.
5. Ziel-/Ist-Abweichungen – umgesetzt.
6. Target Fit – Calculator umgesetzt.
7. Target Fit auf Kandidaten anwenden – nächster Schritt.
8. Kandidatenfreigabe sowie Auswahl/Ranking.
9. Vollständige End-to-End-Produktpipeline.
10. Erste UI-Ausgabe des berechneten Zertifikats.
11. Produktive externe Datenprovider schrittweise anbinden.
12. Später Qualitätsranking, Spread, Premium, Finanzierungskosten und weitere
    Produktdetails verfeinern.

## Aktueller Entwicklungsblock

### Abgeschlossen

`26A.19a – Target-Fit-Calculator`

### Als Nächstes

`26A.19b – Target Fit auf alle freigegebenen Produktkandidaten anwenden`

Danach voraussichtlich:

1. Target-Fit-Ergebnisse freigeben beziehungsweise ungeeignete Kandidaten sauber
   trennen.
2. Verbleibende Kandidaten transparent vergleichen oder ranken.
3. Bestes Zertifikat und optional zwei Alternativen auswählen.
4. Gesamtpipeline verbinden.
5. Ergebnis im Android-UI anzeigen.

## Meilenstein – Erste vollständige Zertifikatsauswahl

Der Meilenstein ist erreicht, wenn die App aus
`Basiswert + Broker + Zielhebel + geplantem Basiswertkurs` automatisch ein
vorhandenes KO-Zertifikat auswählt und mindestens Emittent, WKN, berechneten
Zertifikatspreis, tatsächlichen Hebel und KO-Barriere liefert.

Dieser Meilenstein ist noch nicht vollständig erreicht; der mathematische
Kandidatenpfad befindet sich kurz davor.
