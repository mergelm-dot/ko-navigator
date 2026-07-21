# KO Navigator – Development Log

Dieses Dokument hält wichtige technische und fachliche Entscheidungen des Projekts fest.

---

## Entwicklungsprinzip

Der KO Navigator wird in kleinen, stabilen Schritten entwickelt.

Für jeden neuen Baustein gilt:

1. Ziel definieren
2. Betroffene Dateien festlegen
3. Eine abgeschlossene Funktion umsetzen
4. Projekt bauen und testen
5. Funktionierenden Stand committen
6. Erst danach den nächsten Baustein beginnen

Jeder Commit soll einen lauffähigen Projektstand enthalten.

---

## Architektur der Berechnungsengine

Die Berechnungslogik wird in mehrere Ebenen getrennt.

### KoCalculator

Verantwortlich für reine mathematische Berechnungen rund um Knock-out-Produkte.

Beispiele:

- theoretischer Zertifikatspreis
- KO-Barriere
- Hebel
- Abstand zur KO-Barriere

### PriceConverter

Verantwortlich für Preis- und Währungsumrechnungen.

### TradeCalculationEngine

Verbindet die einzelnen Berechnungen und prüft die Eingaben.

Die Benutzeroberfläche soll keine eigene Finanzmathematik enthalten.

### CertificateMatcher

Später geplante Komponente.

Sie soll echte Emittentenprodukte mit dem geplanten Trade vergleichen und passende Zertifikate auswählen.

### Konsolidierungsschritt 1 – Verantwortlichkeiten verbindlich festgelegt

Für die schrittweise Konsolidierung der Berechnungsengine gelten verbindlich folgende Zielverantwortlichkeiten:

- `KoCalculator` enthält ausschließlich reine mathematische KO-Produktformeln. Er übernimmt keine Orchestrierung, Benutzertexte, Anzeigeformatierung, Empfehlungen oder Zugriffe auf UI, Repositories, Netzwerk, Android und Compose.
- `TradeCalculationEngine` nimmt vollständige Berechnungseingaben entgegen, validiert fachliche Zusammenhänge, legt Rechenschritte fest, ruft reine Calculator-Komponenten auf und führt Ergebnisse, Status, Warnungen und strukturierte Fehler zusammen. Sie dupliziert langfristig keine mathematischen Fachformeln.
- `PriceConverter` ist ausschließlich für eindeutig definierte Währungs-, Einheiten- und preisbezogene Konvertierungen vorgesehen. Eigene Zertifikatspreis-, KO-Abstands- oder Hebelformeln gehören nicht zu seiner Zielverantwortung.

Dieser Schritt ändert ausschließlich KDoc und Architekturdokumentation. Die vorhandenen Formeln, Funktionssignaturen und das Laufzeitverhalten bleiben unverändert. Insbesondere bleiben die aktuelle Rundung in `KoCalculator`, die direkt in `TradeCalculationEngine` enthaltenen Formeln und die Zertifikatspreisformel in `PriceConverter` als dokumentierte Ist-Abweichungen bestehen, bis ihre jeweilige Änderung gesondert freigegeben und durch Tests abgesichert wird.

### Konsolidierungsschritt 2 – Gerichteter KO-Abstand zentralisiert

Der gerichtete absolute und prozentuale KO-Abstand wird zentral durch reine Funktionen im `KoCalculator` berechnet. Long verwendet `S - KO`, Short verwendet `KO - S`; der prozentuale Abstand leitet sich jeweils aus dem gerichteten absoluten Abstand relativ zu `S` ab. Negative Werte bleiben erhalten, und innerhalb dieser Funktionen findet keine Rundung statt.

Die `TradeCalculationEngine` ruft für beide Abstände den `KoCalculator` auf und enthält keine eigene KO-Abstandsformel sowie kein `abs()` mehr für diese Berechnung. Das Ergebnis enthält zusätzlich den absoluten KO-Abstand. Unit-Tests sichern Long, Short, Barrieregleichheit, negative Abstände und den Erhalt von mehr als zwei Nachkommastellen ab.

Die vorhandene Validierung, KO-Statusmodellierung, Zertifikatspreisformel, Rundung der Zertifikatspreisberechnung und weitere bekannte Altprobleme bleiben in diesem bewusst begrenzten Schritt unverändert.

### Konsolidierungsschritt 3 – Eingaben der theoretischen KO-Barrierenberechnung validiert

Die `TradeCalculationEngine` validiert den tatsächlich verwendeten geplanten Einstiegskurs nun auf Endlichkeit und einen Wert größer als `0`. Der Zielhebel wird auf Endlichkeit und einen Wert größer als `1` geprüft. Nach der unveränderten Ableitung der theoretischen KO-Barriere wird auch dieses Ergebnis defensiv auf Endlichkeit und einen positiven Wert geprüft, bevor weitere Berechnungen ausgeführt werden.

Validierungsfehler werden zusätzlich zum vorläufig beibehaltenen Freitext über die strukturierten Codes `INVALID_PLANNED_ENTRY_PRICE`, `INVALID_TARGET_LEVERAGE` und `INVALID_DERIVED_KNOCKOUT_PRICE` ausgegeben. Die bestehenden `0.0`-Ersatzwerte ungültiger Resultate bleiben als bekannte Abweichung für einen späteren, gesonderten Konsolidierungsschritt erhalten.

Eine direkte KO-Barriere wurde nicht zum Inputmodell ergänzt: Dieser Ablauf plant eine theoretische Barriere aus Einstiegskurs, Zielhebel und Richtung. Die Bewertung bestehender Produkte mit fester KO-Barriere bleibt ein separater späterer Anwendungsfall. Die genaue Rolle von `underlyingPrice` im aktuellen Berechnungspfad bleibt **OFFEN**; das Feld wird in diesem Schritt weder umgedeutet noch zusätzlich verwendet oder validiert.

### Konsolidierungsschritt 4 – Fehlerresultate ohne Ersatznullen modelliert

Die rein berechneten Felder `certificatePrice`, `knockoutPrice`, `distanceToKnockoutAbsolute` und `distanceToKnockoutPercent` sind im `TradeCalculationResult` nun nullable. Bei einer fehlgeschlagenen Validierung oder einer ungültigen abgeleiteten KO-Barriere setzt die `TradeCalculationEngine` diese Felder auf `null`, weil kein fachlich belastbares Berechnungsergebnis vorliegt. Die zuvor verwendeten `0.0`-Ersatzwerte werden nicht mehr als scheinbar gültige Ergebnisse ausgegeben.

Übernommene Eingabe- und Kontextwerte wie `underlyingPrice` und `leverage`, der bestehende Gültigkeitsstatus, der strukturierte Fehlercode und der vorläufige Freitext bleiben unverändert erhalten. Gültige Long- und Short-Ergebnisse enthalten weiterhin alle berechneten Werte. Dieser kleine Modellierungsschritt verändert keine Formel, Validierungsreihenfolge, Rundungslogik oder UI-Anbindung.

### Konsolidierungsschritt 5 – Preisberechnung als Übergangslogik charakterisiert

`KoCalculator.calculateCertificatePrice` ist ausdrücklich als bestehende Übergangsberechnung eines gerundeten, ratio-skalierten KO-Differenzwerts eingeordnet. Sie verwendet für Long und Short die KO-Barriere anstelle eines getrennten Basispreises unter der vorläufigen Annahme `B = KO` und rundet intern auf zwei Dezimalstellen. Der Wert ist damit weder ein allgemeiner innerer Wert noch ein vollständiger Modellpreis oder ein realer Emittenten-, Bid- beziehungsweise Ask-Preis.

Direkte Charakterisierungstests sichern das bestehende Long- und Short-Verhalten, die Rückgabe von `0.0` an und hinter der jeweiligen KO-Barriere, die interne Rundung sowie die genau einmalige Anwendung der Ratio ab. Die fehlende Ratio-Validierung bleibt eine bekannte Implementierungsabweichung und wird durch diese Tests nicht fachlich legitimiert.

Formel, Funktionssignatur und Feldname `certificatePrice` bleiben in diesem Schritt unverändert. `PriceConverter` bleibt als bekannte parallele Altimplementierung bewusst ebenfalls unverändert. Die spätere Trennung von Basispreis, KO-Barriere, innerem Wert und Modellpreis ist weiterhin **OFFEN** und erfordert einen gesondert freigegebenen Entwicklungsschritt.

### Konsolidierungsschritt 6 – Ungerundeten inneren Wert isoliert ergänzt

`KoCalculator.calculateIntrinsicValue` berechnet den inneren Wert für Long und Short mit einem separaten Basispreis und wendet das Bezugsverhältnis genau einmal an. Negative Differenzen werden vor der Ratio-Anwendung auf `0` begrenzt. Die reine Funktion rundet nicht und enthält weder Währungsumrechnung noch Finanzierung, Premium, Bid, Ask oder Spread; ihr Ergebnis ist weder ein vollständiger Modellpreis noch ein realer Emittentenpreis.

Die Funktion ist bewusst nicht an die `TradeCalculationEngine` oder einen anderen bestehenden Berechnungsablauf angebunden. Der aktuelle Planungsablauf, `TradeCalculationInput`, `TradeCalculationResult`, `calculateCertificatePrice` und `PriceConverter` bleiben unverändert. Die strukturierte Validierung der vorausgesetzten positiven, endlichen Eingaben und ein getrenntes Produktmodell für Basispreis, KO-Barriere, Ratio, Richtung und Währungen bleiben **OFFEN**.

### Konsolidierungsschritt 7 – Isoliertes KO-Produktmodell eingeführt

Mit `TradeDirection` und `KnockoutProductSpecification` existiert erstmals ein isoliertes Domainmodell für konkrete KO-Produkte. Es führt Basispreis und KO-Barriere als getrennte Größen und referenziert Emittent und Basiswert ausschließlich über `issuerId` und `underlyingId`. Die Produktrichtung ist innerhalb dieses neuen Modells durch `LONG` und `SHORT` typisiert.

Das Modell ist vollständig vom bestehenden theoretischen Planungspfad getrennt und enthält weder Markt- oder Bewertungsdaten noch Android-, Compose- oder UI-Zustand. Es ist nicht an Engine, UI oder Repository angebunden. Ein gemeinsamer Currency-Typ, strukturierte Produktvalidierung und ein getrenntes Marktdatenmodell bleiben **OFFEN**.

### Konsolidierungsschritt 8 – Strukturierte KO-Produktvalidierung eingeführt

Der zustandslose `KnockoutProductSpecificationValidator` prüft die eindeutig allgemeinen Version-1-Regeln des isolierten Produktmodells. Pflichtkennungen dürfen nicht blank sein, die optionale WKN darf bei Vorhandensein nicht blank sein, Basispreis, KO-Barriere und Bezugsverhältnis müssen endlich und größer als `0` sein, und die beiden Währungsangaben müssen vorläufig dem syntaktischen Muster `[A-Z]{3}` entsprechen.

Erwartbare Fehler werden über das reine Enum `KnockoutProductValidationError` ohne Benutzertexte abgebildet. Der Validator bricht nicht beim ersten Fehler ab, sondern liefert höchstens einen Fehler je Feld in einer stabilen Reihenfolge. Er normalisiert keine Eingaben und ist weder an Modellkonstruktion, Engine, UI noch Repository angebunden.

Formale ISIN- und WKN-Prüfungen, eine echte ISO-4217-Listenprüfung, emittentenspezifische Regeln sowie Beziehungen zwischen Basispreis, KO-Barriere und Produktrichtung bleiben ausdrücklich **OFFEN**.

### Konsolidierungsschritt 9 – Passives KO-Produktmarktdatenmodell eingeführt

Mit `KnockoutProductMarketData` existiert ein vom statischen Produktmodell getrenntes Domainmodell für veränderliche KO-Produktmarktdaten. Bid und Ask sind unabhängig nullable und besitzen getrennte Zeitstempel in UTC Epoch Milliseconds. Die Quote-Währung und eine freie Datenquellen-ID werden ausdrücklich mitgeführt; eine automatische EUR-Annahme findet nicht statt. Fehlende Quotes bleiben `null` und werden nicht durch `0.0` ersetzt.

Das Modell enthält weder Last noch Bid-/Ask-Stückzahlen, Spread, relativen Spread, Mid-Preis, Quote-Alter oder Qualitätsbewertung. Es besitzt keine Validierungslogik und ist nicht an Calculator, Engine, UI oder Repository angebunden. Strukturierte Quote-Validierung und reine Berechnungen abgeleiteter Marktdaten bleiben **OFFEN** und erfordern gesonderte Entwicklungsschritte.

### Konsolidierungsschritt 10 – Strukturierte KO-Produktmarktdatenvalidierung eingeführt

Der zustandslose `KnockoutProductMarketDataValidator` prüft die allgemeinen Version-1-Regeln des passiven Marktdatenmodells und liefert zehn maschinenlesbare Fehlercodes ohne Benutzertexte. Vollständige Quotes, Bid-only-Quotes, Ask-only-Quotes und vollständig leere Quotes sind zulässige Zustände. Ein vorhandener Bid muss endlich und `>= 0` sein; Bid `0.0` bleibt strukturell gültig. Ein vorhandener Ask muss endlich und `> 0` sein; Ask `0.0` ist ungültig.

Preis und Zeitstempel müssen je Quote-Seite gemeinsam vorhanden oder gemeinsam abwesend sein. Wenn beide Preise numerisch gültig sind, wird `bid > ask` als blockierender Datenqualitätsfehler gemeldet, ohne Werte zu vertauschen oder zu korrigieren. Alle unabhängigen Fehler werden vollständig in stabiler Reihenfolge gesammelt.

Der Validator normalisiert keine Eingaben und prüft weder Aktualität noch die Kompatibilität mit einer Produktspezifikation. Eine leere Fehlerliste ist keine Berechnungsfreigabe. Es besteht keine Calculator-, Engine-, UI- oder Repository-Anbindung; entsprechende Berechnungen und Integrationen bleiben getrennten späteren Schritten vorbehalten.

### Konsolidierungsschritt 11 – Reinen MarketDataCalculator eingeführt

Der neue `MarketDataCalculator` im Domain-Package berechnet aus Bid und Ask den absoluten Spread, den relativen Spread bezogen auf Ask und den Mid-Preis. Er besitzt einen eigenen strukturierten Result-Typ sowie die drei Calculator-Fehlercodes `INVALID_BID`, `INVALID_ASK` und `BID_ABOVE_ASK` mit stabiler Priorität. Erwartbare ungültige Eingaben erzeugen keine Exceptions, Nullable-Ergebnisse oder Ersatzwerte.

Der absolute Spread wird als `ask - bid` und der relative Spread als `(ask - bid) / ask * 100` berechnet. Der Mid verwendet mit `bid + (ask - bid) / 2` eine algebraisch gleichwertige Form, die das Overflow-Risiko großer Zwischensummen reduziert. Keine Funktion rundet, formatiert, klemmt, vertauscht oder korrigiert Werte.

Der Calculator kennt kein Marktdatenmodell und prüft ausschließlich mathematische Vorbedingungen. Aktualität, Qualitätsklassen, Produktspezifikations-Kompatibilität sowie Calculator-, Engine-, UI- oder Repository-Orchestrierung bleiben getrennten Folgeschritten vorbehalten.

### Konsolidierungsschritt 12 – Cross-Model-Kompatibilität isoliert eingeführt

Der zustandslose `KnockoutProductMarketDataCompatibilityValidator` prüft ausschließlich zwei Beziehungen zwischen einer bereits intern validierten Produktspezifikation und bereits intern validierten Marktdaten. Die Produkt-ISIN wird exakt verglichen, und `productCurrency` muss exakt der Quote-Währung entsprechen. Die Fehlercodes `PRODUCT_ISIN_MISMATCH` und `PRODUCT_CURRENCY_MISMATCH` werden vollständig in stabiler Reihenfolge gesammelt.

`underlyingCurrency` wird nicht mit der Quote-Währung und `issuerId` nicht mit `sourceId` verglichen. Der Validator normalisiert nicht, verwendet keine WKN als Fallback und ruft die beiden Einzelvalidatoren nicht auf. Eine leere Fehlerliste bestätigt nur diese zwei Beziehungen und stellt keine vollständige Berechnungsfreigabe dar.

Aktualität, Quellenqualität, Provider-Mapping, interne Produkt-ID, FX und Quanto bleiben außerhalb dieses Schritts. Es besteht keine Calculator-, Engine-, UI- oder Repository-Anbindung.

### Konsolidierungsschritt 13 – Strukturelle Berechnungsverfügbarkeit eingeführt

Der zustandslose `MarketDataCalculationAvailabilityEvaluator` bewertet für
genau einen der vier Typen `PURCHASE_PRICE`, `SALE_PRICE`, `SPREAD` und `MID`,
ob die dafür benötigten Quote-Seiten strukturell vorhanden sind. Kauf benötigt
ausschließlich Ask, Verkauf einen vorhandenen Bid größer als `0.0`, und Spread
sowie Mid benötigen Bid und Ask. Bid `0.0` bleibt für Spread und Mid zulässig,
blockiert aber die Verkaufsverfügbarkeit.

Die drei maschinenlesbaren Fehlercodes `MISSING_BID`, `MISSING_ASK` und
`BID_NOT_POSITIVE_FOR_SALE` enthalten keine Benutzertexte. Der Evaluator setzt
eine intern gültige Produktspezifikation, intern gültige Marktdaten und eine
erfolgreiche Cross-Model-Kompatibilitätsprüfung voraus. Er ruft die Validatoren
nicht selbst auf und wiederholt weder numerische Preisregeln noch die bereits
durch den Marktdatenvalidator garantierte Preis-/Zeitstempel-Kohärenz.

Die Komponente berechnet keine Werte und liest keine Systemzeit. Aktualität,
Quellenqualität sowie eine Engine-, UI- oder Repository-Orchestrierung bleiben
getrennten Folgeschritten vorbehalten. `StructurallyAvailable` ist deshalb
weder eine vollständige fachliche Berechnungsfreigabe noch eine Aussage über
Handelbarkeit oder eine erfolgreiche Berechnung.

### Konsolidierungsschritt 14 – Zeitliche Marktdatenfrische eingeführt

Die neue `MarketDataFreshnessPolicy` bewertet für `PURCHASE_PRICE`,
`SALE_PRICE`, `SPREAD` und `MID` ausschließlich die zeitliche Frische der
jeweils relevanten Quote-Seiten. Vier explizite, unveränderliche Schwellen ohne
Produktionsdefaults steuern maximales Bid- und Ask-Alter, maximale
Bid-/Ask-Zeitdifferenz und zulässigen Future-Skew. Der Bewertungszeitpunkt wird
je Aufruf explizit übergeben; Systemzeit wird nicht gelesen.

Die fünf maschinenlesbaren Fehlercodes unterscheiden zukünftigen und stale Bid,
zukünftigen und stale Ask sowie eine zu große Bid-/Ask-Zeitdifferenz. Alle
Grenzen sind inklusiv. Spread und Mid prüfen zusätzlich die Zeitdifferenz:
Eine unzulässig zukünftige Seite unterdrückt diesen Differenzfehler, eine stale
Seite dagegen nicht. Negative Epoch-Werte und die Long-Grenzwerte sind zulässig;
die Vergleiche vermeiden riskante Subtraktion und `abs()` und bleiben dadurch
overflow-sicher.

Die Policy setzt Validierung, Kompatibilität und strukturelle Availability als
erfolgreiche Vorbedingungen voraus und ruft keine dieser Komponenten selbst
auf. Es besteht keine Validator-, Availability-, Source-, Engine-, UI- oder
Repository-Anbindung. Quellenqualität, Handelszeiten, Marktstatus,
Produktionsschwellen und die vollständige Orchestrierung bleiben getrennten
Folgeschritten vorbehalten.

### Konsolidierungsschritt 15 – Konfigurierte Quellenfreigabe eingeführt

Die neue `MarketDataSourcePolicy` bewertet für genau einen `sourceId` und einen
der vier CalculationTypes ausschließlich die explizit konfigurierte
Quellenfreigabe. `MarketDataSourceRule` verbindet einen exakten,
case-sensitiven Quellenbezeichner mit einem Set unterstützter CalculationTypes;
`MarketDataSourcePolicyConfig` enthält eine Liste solcher Regeln und lehnt
doppelte exakte Schlüssel ab. Nicht konfigurierte Quellen werden mit
`SOURCE_NOT_CONFIGURED`, nicht unterstützte Typen bekannter Quellen mit
`CALCULATION_TYPE_NOT_SUPPORTED` blockiert.

Es gibt keine Default-Zulässigkeit, Wildcard oder automatische
Capability-Ableitung. Eine leere Konfiguration blockiert alle Quellen, ein
leeres Capability-Set alle Typen der bekannten Quelle. Die Policy erstellt beim
Erzeugen defensive Snapshots der Regelliste und aller Capability-Sets, sodass
nachträgliche externe Collection-Änderungen ihr Verhalten nicht verändern.

Quellentypen, Latenzklassen, Vertrauensstufen und konkrete Produktionsprovider
bleiben bewusst unmodelliert. Die Policy normalisiert keine Quellenbezeichner
und besitzt keine Netzwerk-, Provider-Mapping-, Validator-, Availability-,
Freshness-, Calculator-, Engine-, UI- oder Repository-Anbindung. `Allowed` ist
keine vollständige Berechnungsfreigabe.

### Konsolidierungsschritt 16 – Zentrale Marktdatenorchestrierung eingeführt

Der neue `MarketDataCalculationOrchestrator` führt die bislang getrennten
Marktdatenkomponenten in einer festen Fail-Fast-Reihenfolge zusammen:
Produktspezifikationsvalidierung, Marktdatenvalidierung, Cross-Model-
Kompatibilität, strukturelle Availability, Freshness, Quellenfreigabe und erst
danach Berechnung beziehungsweise Quote-Auswahl. Mehrfachfehler werden nur
innerhalb der bestehenden Validatorstufen gesammelt; bestehende Fehlercodes
werden ohne zentrale Duplikatcodes oder Benutzertexte weitergegeben.

`MarketDataCalculationRequest` beschreibt einen unveränderlichen Auftrag mit
explizitem Bewertungszeitpunkt. `MarketDataCalculationValue` unterscheidet
Kaufpreis, Verkaufspreis, gemeinsames absolutes und relatives Spread-Ergebnis
sowie Mid-Preis. `MarketDataCalculationOrchestrationResult` bildet jede
blockierende Stufe durch einen eigenen maschinenlesbaren Untertyp ab; ein
zusätzliches Stage-Enum ist nicht erforderlich.

Der Orchestrator erhält ausschließlich `MarketDataFreshnessPolicy` und
`MarketDataSourcePolicy` als konfigurierbare Konstruktorabhängigkeiten. Die
zustandslosen Validatoren, der AvailabilityEvaluator und der
`MarketDataCalculator` werden direkt verwendet. Kauf übernimmt den
freigegebenen Ask und Verkauf den freigegebenen positiven Bid ohne Arithmetik.
Spread verwendet die vorhandene absolute und relative Calculator-Funktion;
Mid verwendet die vorhandene Mid-Funktion. Es erfolgt keine zusätzliche
Rundung.

Die Komponente liest keine Systemzeit und besitzt keine Netzwerk-, Repository-,
UI-, Android-, Compose-, `TradeCalculationEngine`- oder `KoCalculator`-
Anbindung. Sie erzeugt weder Empfehlungen noch eine allgemeine Zusage der
Handelbarkeit.

---

## Fachliche Annahmen

Die ersten Berechnungen sind theoretische Modelle.

Noch nicht berücksichtigt werden:

- Spread
- Finanzierungskosten
- Aufgeld
- Emittentenanpassungen
- Wechselkursschwankungen
- unterschiedliche KO-Barriere und Basispreis

Diese Faktoren werden später getrennt ergänzt, damit die Berechnung nachvollziehbar und transparent bleibt.

---

## Verschobene UI-Idee

Nach Auswahl eines Basiswertes soll später eine kompakte Asset-Karte angezeigt werden.

Beispiel:

NVIDIA  
NVDA • NASDAQ • USD

Die Karte soll ein kleines Firmenlogo, den Firmennamen, Ticker, Börse und Währung enthalten.

Die Umsetzung erfolgt erst nach Fertigstellung der Berechnungsengine und der Zertifikatssuche.

---

## Konsolidierungsschritt 17 – KO-Repository-Ports eingeführt

Im neuen Application-Package `de.konavigator.app.application.repository`
existieren getrennte, serverneutrale Repository-Ports für
`KnockoutProductSpecification` und `KnockoutProductMarketData`. Beide
Verträge sind `suspend`-fähig, übergeben die Produkt-ISIN exakt und liefern
die vorhandenen Domainmodelle ohne Nullable-Rückgaben.

`RepositoryResult` unterscheidet strukturiert genau `Success(value)`,
`NotFound` und `DataAccessFailure`. Erwartbare Nichtgefunden- und
Datenzugriffsfehler werden damit nicht über normale Exceptions oder freie
Benutzertexte ausgedrückt. Konkrete technische Ursachen und Providerdetails
bleiben Aufgabe späterer Data-Layer-Implementierungen.

Dieser Schritt führt bewusst keine Repository-Implementierung, Netzwerk-,
Provider- oder UI-Kopplung ein. Er bildet ausschließlich die Grundlage für
den späteren `MarketDataCalculationApplicationService`; das bestehende
`UnderlyingRepository` bleibt ein separater, unveränderter Altpfad.

---

## Konsolidierungsschritt 18 – Application-Service für Marktdatenberechnungen eingeführt

Der neue `MarketDataCalculationApplicationService` koordiniert die beiden
KO-Repository-Ports und den bestehenden Domain-Orchestrator. Sein eigener
Request enthält Produkt-ISIN, CalculationType und einen expliziten
Bewertungszeitpunkt; Systemzeit wird nicht gelesen.

Der Ablauf lädt sequenziell und Fail Fast zuerst die Produktspezifikation und
danach die Marktdaten. Erst nach zwei erfolgreichen Repository-Zugriffen wird
der fertig konfigurierte `MarketDataCalculationOrchestrator` aufgerufen. Die
drei technischen Application-Fehlercodes `PRODUCT_NOT_FOUND`,
`MARKET_DATA_NOT_FOUND` und `DATA_ACCESS_FAILURE` bleiben von allen
Domainresultaten getrennt. Ein Domainresult wird unverändert eingebettet.

Der Service enthält keine Domainregeln und keine Netzwerk-, Provider- oder
UI-Anbindung. Es wurden keine Repository-Implementierungen eingeführt. Damit
ist die Application-Koordination als Grundlage für einen späteren
End-to-End-Testdatenfluss vorhanden.

---

## Konsolidierungsschritt 19 – In-Memory-KO-Repositories und Application-Integrationstest eingeführt

Mit `InMemoryKnockoutProductSpecificationRepository` und
`InMemoryKnockoutProductMarketDataRepository` existieren erstmals zwei
read-only Data-Layer-Implementierungen der KO-Repository-Ports. Beide erzeugen
beim Konstruieren defensive Map-Snapshots der explizit übergebenen
Domainmodelle. Die Suche verwendet ausschließlich die exakte, case- und
whitespace-sensitive Produkt-ISIN und normalisiert keine Eingaben.

Exakt doppelte Produkt-ISINs werden beim Erzeugen abgelehnt; ein
Last-write-wins-Verhalten gibt es nicht. Treffer liefern `Success(value)`,
Nichttreffer `NotFound`. Die Adapter besitzen keinen künstlichen
`DataAccessFailure`-, Exception- oder Latenzmodus, mutieren keine Daten und
führen keine Domainvalidierung aus.

Der neue JVM-Test `MarketDataCalculationApplicationIntegrationTest` prüft mit
acht gezielten Szenarien erstmals den vollständigen lokalen Pfad von realen
In-Memory-Repositories über den unveränderten
`MarketDataCalculationApplicationService` und den unveränderten
`MarketDataCalculationOrchestrator` bis zum typisierten Applicationresult. Die
vier CalculationTypes, beide `NotFound`-Abbildungen, ein unverändertes
Freshness-Domainresult und die exakte ISIN-Semantik werden abgedeckt.

Es wurden keine Produktions- oder Demodaten eingebaut und keine automatische
Composition eingeführt. Netzwerk, Datenbank, Provider, DTOs, Mapper, UI,
ViewModel und Android-Instrumentation bleiben außerhalb dieses Schritts. Der
bestehende `UnderlyingRepository`-Altpfad bleibt unverändert und getrennt.

---

## Konsolidierungsschritt 20A – Presentation-Vertrag und Marktdaten-ViewModel eingeführt

Das neue Package `de.konavigator.app.presentation.marketdata` bildet einen
isolierten Presentation-Vertrag zwischen dem bestehenden
`MarketDataCalculationApplicationService` und einer späteren Compose-UI. Der
immutable `MarketDataCalculationUiState` bewahrt ISIN, CalculationType und den
explizit eingegebenen Bewertungszeitpunkt unverändert auf. Seine berechnete
Button-Aktivierung prüft nur Pflichtfelder, Long-Parsebarkeit und Ladezustand;
eine ISIN-Fachvalidierung oder versteckte Systemzeit gibt es nicht.

`MarketDataCalculationUiSubmission` trennt Idle, Loading, strukturierte
UI-Eingabefehler und abgeschlossene Ergebnisse. Die typisierten UI-Resultate
bilden Kaufpreis, Verkaufspreis, beide Spreadwerte, Mid-Preis und verdichtete
Fehlerkategorien ohne Benutzertexte oder Throwables ab. Alle erfolgreichen
Werte werden roh und ungerundet übernommen; Formatierung bleibt einer späteren
Compose-Schicht vorbehalten.

Das `MarketDataCalculationViewModel` besitzt ausschließlich den
Application-Service als Konstruktorabhängigkeit, stellt einen read-only
`StateFlow` bereit und verarbeitet vier explizite Eingabemethoden. Es erzeugt
den Application-Request ohne Normalisierung, setzt vor dem Serviceaufruf
Loading, verhindert parallele Mehrfachklicks und bricht eine laufende
Auswertung bei jeder Eingabeänderung ab. Ein Request-Token verhindert, dass
ein veraltetes Resultat später einen neueren State überschreibt.
`CancellationException` bleibt Cancellation; andere unerwartete Exceptions
werden ohne Payload auf `UNEXPECTED_FAILURE` verdichtet.

23 fokussierte JVM-Tests mit kontrolliertem Main-Dispatcher sichern State,
UI-Eingabevalidierung, exakte Requestweitergabe, Coroutine- und Abbruchverhalten
sowie das vollständige Mapping der Application- und Domainresultate ab. Eine
Compose-, Factory-, Navigation-, MainActivity- oder Demo-Anbindung wurde noch
nicht eingeführt. Der bestehende `TradePlannerScreen` und der
`UnderlyingRepository`-Altpfad bleiben unverändert und getrennt.
