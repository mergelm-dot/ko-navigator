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

---

## Konsolidierungsschritt 20B – Debug-exklusive Engine-Demo eingeführt

Der neue Marktdatenberechnungspfad ist erstmals über eine ausschließlich im
Debug-Build vorhandene `MarketDataCalculationDemoActivity` lokal sichtbar und
ausführbar. Ein zusätzlicher Debug-Manifest-Eintrag stellt dafür einen zweiten
Launcher-Einstieg bereit. Die bestehende `MainActivity`, der
`TradePlannerScreen` und dessen `UnderlyingRepository`-Altpfad bleiben
unverändert; Navigation wurde nicht eingeführt.

`MarketDataCalculationDemoRoute` sammelt den vorhandenen `StateFlow` mit
`collectAsStateWithLifecycle` und reicht ausschließlich State und die vier
vorhandenen Eingabe-Callbacks an den stateless
`MarketDataCalculationDemoScreen` weiter. Der Screen enthält keine Domainlogik,
keine Systemzeit und keinen Repository- oder Servicezugriff. Er stellt
Eingaben, CalculationTypes, Loading, Eingabefehler und typisierte Resultate dar.
Rohwerte bleiben unverändert; locale-basierte Rundung findet ausschließlich in
der Debug-Anzeige statt. Alle sichtbaren Texte liegen in Debug-Ressourcen.

Die allgemeine `MarketDataCalculationViewModelFactory` im Main-Presentation-
Package besitzt ausschließlich den Application-Service als Abhängigkeit. Die
Debug-Composition erzeugt pro Aufruf einen neuen vollständigen lokalen
Objektgraphen aus In-Memory-Repositories, expliziten Policies, Orchestrator,
Application-Service und Factory. Sie verwendet genau ein neutrales Produkt mit
der ISIN `DE000DEMO001`, Quelle `demo-source` und dem festen expliziten
Bewertungszeitpunkt `1700000000000`. Es werden weder Systemzeit noch echte
Produkte, Emittenten oder Marktdaten verwendet.

Fünf Factory-Tests, vier Debug-Composition-Tests und zwanzig semantics-basierte
Compose-Szenarien sichern die neuen Verträge ab. Demo-Activity, Screen,
Composition, Manifest und Ressourcen liegen ausschließlich unter `src/debug`.
Der Release-Build enthält dadurch keinen Demo-Code und keine Demo-Daten. Der
sichtbare Hinweis kennzeichnet den Bildschirm als interne lokale Testdaten-
Demo und stellt klar, dass keine Anlageberatung erfolgt.

---

## Entwicklungsschritt 22A – Synchronen Trade-Planning-Application-Service eingeführt

Das neue Package `de.konavigator.app.application.tradeplanning` enthält mit
dem `TradePlanningApplicationService` einen synchronen Application-Einstieg
für die theoretische Trade-Planung. Der Service besitzt exakt die bestehende
`TradeCalculationEngine` als Konstruktorabhängigkeit. Seine einzige öffentliche
Fachmethode `execute` reicht den vollständigen `TradeCalculationInput`
unverändert an die Engine weiter und gibt deren `TradeCalculationResult`
unverändert zurück.

Der Service enthält keine eigene Validierung, Berechnung, Rundung oder
Fehlerabbildung. Er verwendet weder Repositories noch Marktdatenkomponenten und
kennt keine UI-, ViewModel-, Android- oder Compose-Details. Damit bleibt die
theoretische Trade-Planung ausdrücklich vom produktbezogenen
`MarketDataCalculationApplicationService` getrennt. Eine Anbindung an den
`TradePlannerScreen` oder ein neues ViewModel wurde nicht vorgenommen.

Neun fokussierte JVM-Tests sichern Konstruktor und öffentliche API, den
synchronen Aufruf, die unveränderte Long- und Short-Weitergabe, gültige und
ungültige Engine-Resultate, Ratio und Wechselkurs sowie die Freiheit von
Repository-, Marktdaten-, UI-, Android- und Compose-Abhängigkeiten ab.

---

## Entwicklungsschritt 22B.1 – Brokerneutrale Einstiegskursrelation typisiert

Das neue Package `de.konavigator.app.domain.tradeplanning` enthält einen
isolierten Domainbaustein für die Relation zwischen aktuellem Basiswertkurs und
geplantem Einstiegskurs. `EntryPriceRelation` unterscheidet ausschließlich
`BELOW_CURRENT`, `AT_CURRENT` und `ABOVE_CURRENT`. Zwei strukturierte
Fehlercodes bilden einen ungültigen aktuellen Kurs beziehungsweise einen
ungültigen geplanten Einstiegskurs ohne Benutzertexte ab.

Der zustandslose `EntryPriceRelationEvaluator` prüft zuerst den aktuellen Kurs
und danach den geplanten Einstieg jeweils auf Endlichkeit und einen Wert größer
als `0.0`. Nur zwei gültige Werte werden klassifiziert. Version 1 verwendet
exakte numerische Gleichheit ohne Rundung, Toleranz, Epsilon- oder
Tick-Size-Annahme; unmittelbar benachbarte `Double`-Werte bleiben daher
unterschiedliche Relationen.

Die Relation kennt weder Long oder Short noch Kauf, Verkauf, Broker-Ordertypen
oder Strategiehinweise. Sie ist nicht an UI, Engine, Application-Service,
Repository oder Marktdaten angebunden. Die bestehende Compose-Ordertyp-Logik
bleibt bis zu einer späteren, gesondert geprüften Migration unverändert aktiv.
19 fokussierte JVM-Tests sichern Relation, Grenznachbarn, Validierung,
Fehlerpriorität, Determinismus und die Android-/Compose-freie API ab.

---

## Entwicklungsschritt 22C.1 – Trade-Planner-Presentation-Verträge eingeführt

Das neue Package `de.konavigator.app.presentation.tradeplanner` enthält einen
minimalen typisierten Presentation-Vertrag und ein isoliertes synchrones
`TradePlannerViewModel`. Der immutable State bewahrt aktuellen Basiswertkurs,
geplanten Einstieg und Zielhebel als unveränderte Eingabestrings auf und führt
die Richtung über `TradeDirection`. Fünf explizite Methoden ändern jeweils nur
ihren Statebereich beziehungsweise starten die Berechnung.

Erst beim Berechnungsaufruf werden lokale Parsingkopien getrimmt, Dezimalkommas
in Punkte überführt und mit `toDoubleOrNull()` geparst. Pflicht-, Parse-,
Endlichkeits- und Wertebereichsfehler werden vollständig mit höchstens einem
Fehler je Feld in stabiler Reihenfolge gesammelt. Nach erfolgreicher
Presentation-Validierung wird zuerst die brokerneutrale Einstiegskursrelation
ausgewertet und erst danach der synchrone `TradePlanningApplicationService`
aufgerufen.

Der vollständige Übergangsinput verwendet weiterhin `exchangeRate = 1.0` und
`ratio = 0.01` sowie das explizite Mapping von `TradeDirection` auf den
bestehenden Boolean-Vertrag. Engine-Fehler werden auf vier strukturierte
Presentation-Codes abgebildet; inkonsistente Legacy-Resultate erhalten einen
eigenen Code. Der bestehende Domain-Freitext wird vollständig ignoriert, und
Rechenwerte werden weder gerundet noch formatiert.

Der Pfad verwendet einen read-only `StateFlow`, aber keine Coroutines, keinen
Loading-Zustand, keine Systemzeit und keine Repository- oder
Marktdatenkomponenten. Eine Anbindung an `TradePlannerScreen`, `MainActivity`,
Factory oder Composition wurde nicht vorgenommen. 30 fokussierte JVM-Tests
sichern State, Parsing, Validierung, Relation, Inputkonstruktion, Mapping und
die Verantwortungsgrenzen ab.

---

## Entwicklungsschritt 22D.1 – Trade-Planner-Factory und produktive Composition eingeführt

Die neue `TradePlannerViewModelFactory` im Package
`de.konavigator.app.presentation.tradeplanner` erzeugt ausschließlich das
`TradePlannerViewModel` und erhält als einzige Konstruktorabhängigkeit den
`TradePlanningApplicationService`. Sie akzeptiert den ViewModel-Typ nur über
einen exakten Klassenvergleich, lehnt unbekannte Typen strukturiert mit einer
`IllegalArgumentException` ab und besitzt keine Context-, Ressourcen-,
SavedState-, Repository-, MarketData- oder Compose-Abhängigkeit.

Mit `TradePlannerComposition` im Main-Package
`de.konavigator.app.composition` existiert erstmals ein produktiver,
zustandsloser Composition-Einstieg für die theoretische Trade-Planung. Jeder
Aufruf von `createViewModelFactory()` erzeugt einen neuen Objektgraphen aus dem
bestehenden `TradeCalculationEngine`-Object, einem neuen
`TradePlanningApplicationService` und einer neuen
`TradePlannerViewModelFactory`. Es werden weder Service noch ViewModel-State
global gespeichert.

Der Aufbau ist Android-Context-frei und enthält keine Repositories,
MarketData-Komponenten oder Debug-Abhängigkeiten. `MainActivity`, Route und
`TradePlannerScreen` bleiben weiterhin unverändert und noch nicht angebunden.
15 neue fokussierte JVM-Tests sichern Factory-Vertrag, exakte
Abhängigkeitsidentitäten, Objektgraph, Instanztrennung, deterministisches
Verhalten und die ausgeschlossenen Verantwortungsbereiche ab.

---

## Entwicklungsschritt 22E.1 – Trade-Planner-Route und state-gesteuerten Screen eingeführt

Die neue produktive `TradePlannerRoute` im Package
`de.konavigator.app.presentation.tradeplanner` sammelt den read-only
`StateFlow` des übergebenen `TradePlannerViewModel` mit
`collectAsStateWithLifecycle`. Sie reicht den vollständigen State, exakt fünf
ViewModel-Callbacks und den äußeren `Modifier` an den `TradePlannerScreen`
weiter. Die Route besitzt keinen eigenen State und kennt weder Factory,
Composition, Application-Service noch Berechnungsengine.

Der `TradePlannerScreen` besitzt nun einen state-gesteuerten Vertrag aus
`TradePlannerUiState` und fünf Callbacks. Aktueller Basiswertkurs, geplanter
Einstiegskurs, Zielhebel und `TradeDirection` werden nicht mehr lokal
gespiegelt. Lokale Zustände bleiben vorläufig ausschließlich für Basiswertsuche
und Assetauswahl sowie Broker- und Emittentenauswahl bestehen. Bei einer
Assetauswahl wird ein vorhandener Kurs locale-stabil wie bisher formatiert an
beide Preis-Callbacks gegeben; ein fehlender Kurs wird als leerer String
weitergereicht.

Die frühere Komma-Parsinglogik, lokale Eingabevalidierung, Prozentformel und
Buy-/Sell-/Market-Ordertypmatrix einschließlich Strategieerklärungen wurden
vollständig aus dem Composable entfernt. Alle sechs strukturierten Inputfehler
werden feldnah auf Ressourcen gemappt. Erfolgreiche Submissions zeigen eine
neutrale theoretische Ergebnisbox mit locale-basierter reiner
Darstellungsformatierung; sie enthält keine Produktidentität, keine Marktdaten
und keine echte Zertifikatskarte. Der theoretische Wert wird ausdrücklich
nicht als Kaufpreis bezeichnet. Vier CalculationError-Werte werden in einer
neutralen Fehlerbox dargestellt.

Da `MainActivity` in diesem Schritt absichtlich unverändert bleibt, stellt ein
klar markierter No-Argument-Kompatibilitäts-Wrapper nur einen lokalen
Übergangszustand bereit. Er enthält keine fachliche Logik und muss mit der
Activity-Anbindung in Schritt 22E.2 entfernt werden. Die
`lifecycle-runtime-compose`-Abhängigkeit liegt nun im Main-Classpath.

17 semantics-basierte Screen-Tests sichern State und Callback-Vertrag,
Long/Short-Typisierung, alle Fehler- und Relationstexte, die neutrale
Ergebnisdarstellung, die verbliebenen lokalen UI-Bereiche sowie Scrollbarkeit
und Entfernung der alten Ordertypen ab. Vier Route-Tests prüfen initiale
Darstellung, exakte ViewModel-Änderungen, Berechnung samt gerendertem Resultat
und die ausgeschlossenen Route-Abhängigkeiten. `MainActivity` ist noch nicht
an Route, Factory und Composition angebunden.

---

## Entwicklungsschritt 22E.2 – Trade-Planner produktiv an MainActivity angebunden

`MainActivity` erzeugt die `TradePlannerViewModelFactory` einmalig Activity-seitig
außerhalb der Recomposition über `TradePlannerComposition`. Das
`TradePlannerViewModel` wird mit `ViewModelProvider` aus dem
Activity-`ViewModelStore` bezogen. Innerhalb der unveränderten Theme-Struktur ist
`TradePlannerRoute` nun der aktive UI-Einstieg; Service, Engine,
Einstiegskursrelation, Repositories und Marktdaten bleiben aus der Activity
ausgeschlossen.

Der temporäre No-Argument-Kompatibilitäts-Wrapper wurde vollständig entfernt.
Produktiv verbleibt nur der state- und callback-gesteuerte Screen-Vertrag. Beim
ersten vollständigen Lauf auf einem realen Gerät wurden zudem ausschließlich im
Screen die Semantikgrenzen der bestehenden Feldfehler und typisierten
Long-/Short-Auswahl präzisiert. Die Richtungsauswahl steht im unveränderten
Eingabebereich vor den drei Berechnungsfeldern, damit sie im initialen
Geräte-Viewport direkt erreichbar ist. Fachlogik, Texte, Styling und Callbacks
blieben unverändert.

Der JVM-Lauf bestätigte erneut 806/806 Tests. `assembleDebug` und
`assembleDebugAndroidTest` waren erfolgreich. Auf einem autorisierten OPPO
CPH2791 mit Android 16 (SDK 36) liefen alle 42 Instrumentationstests erfolgreich,
darunter alle 21 Trade-Planner-Tests; der Gradle-Lauf endete mit
`BUILD SUCCESSFUL`.

Der praktische Gerätetest bestätigte den normalen Start über `MainActivity`, die
Editierbarkeit aller drei Berechnungseingaben, die Long-/Short-Umschaltung,
gültige Berechnung und theoretische Ergebnisbox sowie eine feldnahe Meldung bei
ungültiger Eingabe. Die entfernten Ordertypen erscheinen nicht, Scrollen
funktioniert, und die aktuelle UI blieb im Portrait- und Landscape-Modus
nutzbar. Nach Wiederherstellung der ursprünglichen Portrait-Sperre startete die
App dreimal hintereinander kalt und absturzfrei wieder in der aktuellen
Trade-Planner-Oberfläche.

---

## 2026-07-22 – Langfristige Datenqualitäts- und Explainability-Architektur dokumentiert

Die langfristige Architekturrichtung wurde ausschließlich in der
Projektdokumentation ergänzt; Produktiv- und Testcode blieben unverändert.
Sieben Entscheidungen sind erstmals als `ADR-0001` bis `ADR-0007` im neuen
Register `docs/DECISIONS.md` erfasst: CurrencyPolicy, Quote-Freshness mit
Warn- und Blockierstufen, kontextabhängige Bid-/Ask-Policy, realistische
Robustheits-Integrationstests, Zertifikats-Qualitätsscore, Explainable Engine
und Confidence Score.

Die akzeptierten ADRs legen die jeweilige Architekturrichtung fest, nehmen aber
keine noch offenen Produktionsdetails vorweg. Insbesondere bleiben FX-Provider,
Freshness- und Spread-Schwellen, technische Ordertyp-Zuordnungen, Score-Skalen,
Normalisierung, Gewichte, Mindestdaten, Emittenten- und Liquiditätsmetriken
sowie Finanzierungskosten vor einer Implementierung fachlich abzustimmen.

`ROADMAP.md` führt die neuen Bausteine in drei priorisierten Langfristphasen.
`docs/ARCHITECTURE.md` beschreibt Verantwortungsgrenzen, Zielreihenfolge und
die Trennung von Produktqualität, Berechnungszuverlässigkeit und Erklärung.
`docs/FORMULAS.md` ergänzt verbindliche Zielkonventionen für Currency-
Freigabe, Quote-Auswahl, Freshness sowie die noch unparametrisierten Score-
Strukturen. `AGENTS.md` hält die ADR-Abstimmung und die Pflege aller
verbindlichen Dokumente als Projektregel fest.

Die bereits implementierte isolierte `MarketDataFreshnessPolicy` und die
vorhandene Auswahl von Purchase=Ask, Sale=Bid und Mid im
Marktdatenorchestrator werden als Teilbasis dokumentiert, nicht fälschlich als
neue oder vollständige Implementierung. Es erfolgte keine Änderung an Formeln,
Laufzeitverhalten, Abhängigkeiten oder Projektstruktur.

---

## Entwicklungsschritt 23B – Engine-Referenz- und Grenzwerttests ergänzt

Die neue JVM-Suite `TradeCalculationEngineReferenceTest` sichert mit exakt 14
Tests den aktuellen Vertrag der realen `TradeCalculationEngine` und des realen
`TradeCalculationInput` ohne Mocks oder neue Produktionshelper ab. Acht feste
Long-/Short-Referenzvektoren prüfen Standardwerte, Zielhebel `3` und `5`, ein
Bezugsverhältnis von `0.1`, einen Wechselkurs von `1.1` sowie sehr kleine
positive Übergangswerte.

Die übrigen Tests charakterisieren bekannte offene Abweichungen ausdrücklich
als Ist-Zustand: `exchangeRate` wird aktuell ignoriert, Ratio `0` wird aktuell
akzeptiert, die bestehende Cent-Rundung kann kleine positive Rohwerte auf
`0.00` reduzieren, und ein sehr hoher endlicher Zielhebel kann für Long und
Short die KO-Barriere auf den Einstieg runden. Zusätzlich werden das
JVM-Double-Verhalten für `Math.nextUp(1.0)`, Determinismus und die derzeitige
Unabhängigkeit der Engine-Mathematik vom aktuellen `underlyingPrice` geprüft.

Keine Produktivformel, Validierung, Rundung, UI, Architektur oder Abhängigkeit
wurde geändert. Die Charakterisierung ist keine fachliche Freigabe der
Abweichungen. Der nächste fachliche Schritt muss den FX-, Ratio- und
tatsächlichen Hebelvertrag gezielt und getrennt entscheiden, bevor diese
Bereiche produktiv erweitert werden.

---

## Entwicklungsschritt 23D.1 – Isolierten FX-/Ratio-Produktwertvertrag implementiert

Mit `CurrencyCode`, `CurrencyConversion` und
`TheoreticalProductValueCalculator` wurden drei neue isolierte
Produktivverträge eingeführt. `CurrencyCode` trimmt und normalisiert
dreistellige ASCII-Währungscodes, ohne eine externe Währungsdatenbank oder
stille Ersatzwerte zu verwenden. `CurrencyConversion` unterscheidet den
Same-Currency-Fall ohne numerische Rate von Cross-Currency mit verschiedenen
Währungen und einer positiven, endlichen Rate.

Die FX-Richtung ist typisiert als Basiswertwährung je Produktwährung. Der neue
Calculator wendet das Bezugsverhältnis als Basiswerteinheiten je Produktstück
exakt einmal an und teilt den Wert in Basiswertwährung bei Cross-Currency durch
die beschriftete Rate. Abstand, Ratio und abgeleitete Werte werden defensiv auf
positive Endlichkeit geprüft. Es findet keine Rundung statt; insbesondere
bleiben sehr kleine positive Modellwerte positiv.

Exakt 18 neue JVM-Tests prüfen Currency-Normalisierung, ungültige Codes,
Same-/Cross-Currency-Verträge, ungültige FX-Raten, Ratio- und
Abstandsvalidierung, USD→EUR, EUR→USD, Same-Currency, die genau einmalige
Ratio-Anwendung, ungerundete Kleinwerte, Determinismus und die Freiheit von
Android-, Repository-, MarketData-, Systemzeit- und Coroutine-Abhängigkeiten.

`TradeCalculationEngine`, `TradeCalculationInput`, `TradeCalculationResult`,
`KoCalculator`, `PriceConverter`, Application Service, ViewModel, UI und der
MarketData-Pfad sind unverändert und nicht mit dem neuen Vertrag verbunden.
Damit bleiben auch die bestehenden Charakterisierungstests und das aktuelle
Laufzeitverhalten unverändert. Ein Gerätetest ist für diesen reinen JVM- und
Dokumentationsschritt nicht erforderlich. ADR-0008 dokumentiert die
abgestimmte Entscheidung und den späteren Migrationspfad.

---

## Entwicklungsschritt 23D.2 – Aktiven Engine-Pfad auf FX-/Ratio-Vertrag migriert

`TradeCalculationInput` verlangt jetzt `targetLeverage`, Ratio und den
typisierten `CurrencyConversion`-Kontext ohne stille Defaults. Das alte
`exchangeRate`-Feld wurde entfernt. `TradeCalculationEngine` validiert Ratio
defensiv und delegiert die gesamte FX-/Ratio-Produktwertberechnung an den
`TheoreticalProductValueCalculator`. Cross-Currency wird mit der verbindlichen
Richtung Basiswertwährung je Produktwährung durch Division umgerechnet.

`TradeCalculationResult` führt den ungerundeten theoretischen Wert in
Basiswertwährung, den ungerundeten theoretischen Produktwert sowie beide
Ergebniswährungen. Das missverständliche Feld `certificatePrice`, Domain-
Freitexte und die frühzeitige Cent-Rundung wurden aus dem aktiven Engine-Pfad
entfernt. KO- und Distanzformeln blieben unverändert. Ungültige Ratio-Werte und
ungültige abgeleitete Produktwerte liefern strukturierte Fehler.

Der `TradePlanningApplicationService` bleibt ein unveränderter synchroner
Durchreicher. Das ViewModel setzt für den noch abstrakten UI-Pfad zentral und
sichtbar die Übergangsannahmen Ratio `0.01` und `SameCurrency(XXX)`. Es gibt
keine neue Ratio-, FX- oder Währungsanzeige. Der bestehende Ergebniswert wurde
intern zu `theoreticalProductValue` umbenannt; die sichtbare neutrale
Bezeichnung und die Anzahl der Ergebniszeilen bleiben unverändert.

Die bisherigen Charakterisierungstests für ignoriertes FX, akzeptierte Ratio
`0` und frühe Cent-Rundung wurden nicht entfernt, sondern in fachliche
Regressionstests für FX-Division, Ratio-Validierung und ungerundete kleine
Werte umgewandelt. Der JVM-Bestand umfasst nun 846 Tests. Der Android-Testcode
wurde ausschließlich an Feldname und neue strukturierte Fehlerabbildungen
angepasst. `KoCalculator.calculateCertificatePrice` und `PriceConverter`
bleiben als Legacy-Code vorhanden, werden vom aktiven Engine-Pfad aber nicht
mehr verwendet.

Ein berechneter theoretischer Hebel, Zielhebelabweichung, reale Produktdaten,
Bid/Ask, Spread, MarketData-Integration und BigDecimal bleiben ausdrücklich
außerhalb dieses Schritts.

---

## Entwicklungsschritt 23D.3 – Berechneten theoretischen Hebel am geplanten Einstieg implementiert

Der neue reine `TheoreticalLeverageCalculator` berechnet zunächst das
Basiswert-Exposure in Produktwährung aus geplantem Einstieg, Ratio und dem
typisierten `CurrencyConversion`-Kontext. Anschließend teilt er dieses Exposure
durch den vollständig ungerundeten theoretischen Produktwert. Der so
rückgerechnete Wert heißt ausschließlich „Berechneter theoretischer Hebel am
geplanten Einstieg“ und wird nicht aus dem Zielhebel kopiert.

`TradeCalculationResult` führt nun den validierten Zielhebel, das Exposure in
Produktwährung und den berechneten theoretischen Hebel. Ungültige oder nicht
endliche Exposure- und Hebelwerte sowie Hebel kleiner oder gleich `1` werden
strukturiert als `INVALID_CALCULATED_LEVERAGE` abgelehnt. Fehlerresultate
enthalten weiterhin genau einen Fehler und keine Rechenwerte. KO-, Distanz-,
FX-, Ratio- und Produktwertformeln sowie deren Rundungsfreiheit bleiben
unverändert.

Der `TradePlanningApplicationService` bleibt ein dünner synchroner
Durchreicher. Das Domainresult wird ohne Hebelberechnung im ViewModel auf
`TradePlannerUiResult` abgebildet. Die bestehende theoretische Ergebnisbox
zeigt genau zwei neue neutrale Zeilen für Zielhebel und berechneten
theoretischen Hebel; Exposure, Ratio, FX und Währungen werden nicht ergänzt.
Der Wert behauptet keinen tatsächlichen oder handelbaren Produkthebel.

15 neue Calculator-Tests sowie zwei neue Engine-Tests erhöhen den JVM-Bestand
auf 863 erfolgreiche Tests. Die Android-Testquellen umfassen nun 43
kompilierte Instrumentationstests. `assembleDebug` und
`assembleDebugAndroidTest` sind erfolgreich. Ein Gerätetest der sichtbaren
Hebelzeilen ist in diesem Umsetzungsschritt noch nicht erfolgt.

---

## Entwicklungsschritt 24A – Strukturiertes Mock-Daten-Szenario-Kit eingeführt

Das neue test-only Package `de.konavigator.app.scenarios` ergänzt die
vorhandenen Unit- und Referenztests um einen begrenzten, strukturierten Katalog
vollständiger Berechnungsfälle der realen `TradeCalculationEngine`. Es enthält
ein typisiertes Szenariomodell, zentral gepflegte Fixtures und einen
JUnit-4-parameterisierten Vertragstest. Eine zweite Testumgebung, ein neues
Modul oder externe Datenquellen wurden nicht eingeführt.

Der Katalog umfasst exakt 25 Szenarien: 15 Erfolgsfälle und 10 Fehlerfälle.
Abgedeckt sind Long und Short, Same- und Cross-Currency in beiden Richtungen,
die Ratios `0.1`, `0.01` und `0.001`, niedrige und hohe gültige Zielhebel,
ein hoher gültiger Einstieg, ein normaler Dezimalpreis, ein sehr kleiner
positiver Produktwert, numerische Overflow-Fälle und die bestehende
Validierungspriorität. Ein klar benanntes Characterization-Szenario sichert
den aktuellen Ist-Vertrag, dass `input.underlyingPrice` die Engine-Mathematik
nicht beeinflusst und `result.underlyingPrice` den geplanten Einstieg enthält.

Alle erwarteten numerischen Werte sind feste, vorab unabhängig geprüfte
Literale. Die Fixtures bauen keine Produktionsformel nach und verwenden weder
Produktionscalculator noch Engine-Ausgaben zur Sollwerterzeugung. Jeder
Erfolgsfall prüft sämtliche Felder des `TradeCalculationResult`; jeder
Fehlerfall prüft den exakten Fehlercode und alle Rechenfelder auf `null`.
Absolute und relative Vergleiche verwenden zentral jeweils eine Toleranz von
`1e-12`, und jede Assertion führt den Szenarionamen als Kontext.

Der gezielte Lauf bestätigte 25/25 Szenarien. Die vollständige JVM-Suite
umfasst nun 888/888 erfolgreiche Tests ohne Fehler oder übersprungene Tests.
`assembleDebug` und `assembleDebugAndroidTest` waren erfolgreich. Produktivcode,
Formeln, Gradle-Konfiguration, Manifeste und Android-Testcode blieben
unverändert. Ein Gerätetest ist für diesen ausschließlich JVM-Testcode und
Dokumentation betreffenden Schritt nicht erforderlich.

---

## Entwicklungsschritt 24C – Projektdokumentation auf den Ist-Stand synchronisiert

**Datum:** 2026-07-23

Dieser reine Dokumentationsschritt gleicht `ROADMAP.md`,
`docs/ARCHITECTURE.md`, `docs/DECISIONS.md` und `DEVLOG.md` mit dem
implementierten Stand ab. Produktivcode, Tests, Gradle-Dateien, Manifeste und
`docs/FORMULAS.md` bleiben unverändert.

Die Roadmap trennt nun ausdrücklich den weit fortgeschrittenen theoretischen
Trade-Planning-Pfad von den weiterhin offenen Anforderungen realer
KO-Produkte. Dokumentiert sind insbesondere der aktive
`TradePlannerScreen`-Pfad, der typisierte FX-/Ratio-Vertrag, der theoretische
Produktwert und Hebel, die 25 vollständig geprüften Engine-Szenarien sowie der
lokale KO-Produktpfad über Repository-Ports und In-Memory-Adapter. Reale
Produkt-, Marktpreis- und FX-Provider, externe DTOs und Mapper bestehen
weiterhin nicht.

Die Architektur beschreibt die bereits vorhandenen, getrennten Komponenten
für Produktspezifikations- und Marktdatenvalidierung, Kompatibilität,
Verfügbarkeit, Freshness, Quellenfreigabe und die siebenstufige
Fail-Fast-Orchestrierung jetzt als Ist-Zustand. Historische Aussagen zu
`exchangeRate`, `certificatePrice`, der Trade-Planner-Anbindung und erst
späterer Orchestrierung wurden dort auf den aktuellen Stand gebracht. Die
bestehenden Einzelkomponenten und ihre Regeln bleiben unverändert.

Mit ADR-0009 ist die nächste Zielgrenze akzeptiert: Das zukünftige Package
`de.konavigator.app.domain.dataquality` erhält einen strukturellen
Data-Quality-Vertrag und einen delegierenden Validator. Schritt 25A verwendet
die bestehenden strukturellen Validatoren als Single Source of Truth und
bildet zunächst nur `PASSED` sowie blockierende Findings ab. `WARNING` wird
lediglich als Vertragsoption vorbereitet; Spread-, Freshness-, FX-, Broker-
oder andere Produktionsschwellen werden nicht erfunden. Availability,
Freshness, Source Policy und der bestehende Orchestrator werden in diesem
ersten Schritt nicht migriert.

Data-Quality-Status, Confidence Score und Produktqualität bleiben getrennte
Konzepte. Der neue Vertrag führt weder Scoring, Ranking, konkrete
Providerannahmen noch Kauf- oder Verkaufsempfehlungen ein.

Der unveränderte Teststand vor und nach diesem Dokumentationsschritt umfasst
888/888 erfolgreiche JVM-Tests. Der zuletzt unveränderte
Instrumentationstestbestand umfasst 43/43 erfolgreiche Tests. Ein Gerätetest
ist für die ausschließlich dokumentarische Änderung nicht erforderlich.

Der nächste freigegebene technische Schritt lautet:

**Schritt 25A – Strukturellen Data-Quality-Vertrag und delegierenden Validator
für `KnockoutProductSpecification` und `KnockoutProductMarketData`
einführen.**

---

## Entwicklungsschritt 24D – Neutraler Datenqualitäts-Platzhalter im Trade Planner

**Datum:** 2026-07-23

Die bestehende theoretische Ergebnisbox des `TradePlannerScreen` enthält nach
den vorhandenen Ergebniswerten und vor dem Modellhinweis genau eine neue
neutrale Zeile. Das Label lautet „Datenqualität“, der einzige angezeigte Wert
„Noch nicht bewertet“. Die Darstellung verwendet die bestehende
`ResultValueRow` ohne zusätzliche Karte, Interaktion, Farbe, Ampellogik, Icon
oder Badge.

Der Wert ist ausschließlich ein statischer UI-Platzhalter. Es findet noch
keine technische Data-Quality-Bewertung statt; weder Domainmodell noch
Validator, Policy, Orchestrator, Engine, ViewModel, Application Service oder
Repository sind angebunden oder verändert. Insbesondere wird kein Status wie
`PASSED`, `WARNING` oder `BLOCKED` behauptet.

Der bestehende Android-Test des state-gesteuerten Screens prüft die Sichtbarkeit
von Label, neutralem Wert und Modellhinweis sowie das Ausbleiben positiver oder
negativer Qualitätsbehauptungen. Schritt 25A bleibt der nächste fachliche
Data-Quality-Schritt. Vor einem Commit ist ein Gerätetest des Platzhalters
erforderlich.

---

## Entwicklungsschritt 25A – Strukturellen Data-Quality-Vertrag eingeführt

**Datum:** 2026-07-23

Das neue Domain-Package `de.konavigator.app.domain.dataquality` enthält den
ersten einheitlichen, strukturellen Data-Quality-Vertrag. Der Gesamtstatus
unterscheidet `PASSED`, `WARNING` und `BLOCKED`; der delegierende Validator
erzeugt in Version 1 ausschließlich `PASSED` ohne Findings oder `BLOCKED` mit
mindestens einem blockierenden Finding. `WARNING` ist nur als Vertragstyp
vorbereitet. Es wurden keine Warnschwellen oder neuen Fachregeln eingeführt.

Jedes Finding führt eine typisierte Severity, Komponente, Kategorie und einen
stabilen Data-Quality-Code. Alle neun bestehenden
Produktspezifikationsfehler, zehn Marktdatenfehler und zwei
Kompatibilitätsfehler werden vollständig und deterministisch jeweils auf einen
eigenen Finding-Code abgebildet. Die Data-Quality-Codes bewahren damit die
ursprüngliche Fehleridentität maschinenlesbar, ohne UI- oder Exception-Texte zu
übernehmen.

Der zustandslose `KnockoutProductDataQualityValidator` delegiert unverändert an
`KnockoutProductSpecificationValidator`,
`KnockoutProductMarketDataValidator` und – nur bei zwei intern gültigen
Einzelmodellen – an
`KnockoutProductMarketDataCompatibilityValidator`. Spezifikationsfindings
stehen vor Marktdatenfindings; Kompatibilitätsfindings folgen zuletzt. Die
vorhandenen Validatoren und ihre Fehlerreihenfolgen bleiben Single Source of
Truth. Es werden weder Eingaben normalisiert oder mutiert noch Daten
korrigiert oder berechnet.

27 neue fokussierte JVM-Tests prüfen Status- und Assessment-Invarianten,
defensive Findings-Snapshots, Determinismus, vollständige Einzel- und
Mehrfachfehlermappings, Komponenten, Severity, stabile Reihenfolge,
Cross-Model-Bedingung sowie Mutations- und Normalisierungsfreiheit. Der
gezielte Data-Quality-Testlauf war mit 27/27 Tests erfolgreich. Die vollständige
JVM-Suite umfasst nun 915/915 erfolgreiche Tests ohne Fehler oder übersprungene
Tests; `assembleDebug` und `assembleDebugAndroidTest` waren ebenfalls
erfolgreich.

Availability, Freshness, Source Policy und der
`MarketDataCalculationOrchestrator` wurden nicht angebunden oder verändert.
Ebenso bestehen keine Repository-, API-, DTO-, Mapper-, Coroutine-, Systemzeit-,
Application-, ViewModel- oder UI-Abhängigkeiten. Der sichtbare neutrale
Datenqualitäts-Platzhalter bleibt unverändert. Ein Gerätetest ist für diesen
reinen Domain-, JVM-Test- und Dokumentationsschritt nicht erforderlich.

---

## Entwicklungsschritt 25B – Strukturelles Assessment in den Orchestrator integriert

**Datum:** 2026-07-23

Der `MarketDataCalculationOrchestrator` verwendet das bestehende
`KnockoutProductDataQualityValidator`-Assessment jetzt als erste fachliche
Freigabestufe. Ein `BLOCKED`-Assessment beendet die Orchestrierung fail closed,
bevor Availability, Freshness, Source Policy oder Berechnung ausgewertet
werden. `PASSED` führt in den bisherigen Ablauf. `WARNING` wird weiterhin
nicht erzeugt, ist im Resultatvertrag aber transportierbar und wird defensiv
als nicht blockierend behandelt, ohne Folgeprüfungen zu überspringen.

Der Orchestrator-Resultatvertrag führt das vollständige, nicht-nullbare
`DataQualityAssessment` in jedem Erfolgs- und Blockierungsresultat. Die drei
früheren strukturellen Resultattypen wurden durch einen eindeutigen
`StructuralDataQualityBlocked`-Typ ersetzt; die vollständigen Details und ihre
stabile Reihenfolge bleiben ausschließlich in den bestehenden Findings.
Availability-, Freshness-, Source- und Calculation-Fehler bleiben unverändert
und überschreiben das strukturelle Assessment nicht.

Der Orchestrator erzeugt oder klassifiziert keine Findings und dupliziert
keinen der 21 Codes. Validator-, Availability-, Freshness- und Source-Regeln,
Repositorys, Adapter, Produktmodelle, APIs, Engine und Formeln bleiben
unverändert. Der bestehende Application Service reicht Domainresultate bereits
unverändert weiter. Der exhaustive Market-Data-Presentation-Adapter wurde nur
für den neuen Domainresultat-Subtyp kompilierbar gehalten; eine sichtbare
Data-Quality-Anbindung, neue Texte oder Änderungen am neutralen
Trade-Planner-Platzhalter erfolgten nicht.

Der fokussierte Orchestrator-Testbestand umfasst nun 94 erfolgreiche Tests.
Neue beziehungsweise migrierte Prüfungen sichern strukturelle Blockierungen,
vollständige Findings und Reihenfolge, Nichtausführung späterer Stufen,
Assessment-Erhalt bei späteren Blockierungen, deterministische Ergebnisse und
die vorbereitete WARNING-Transportierbarkeit ab. Die vollständige JVM-Suite
umfasst nun 926/926 erfolgreiche Tests ohne Fehler oder übersprungene Tests;
`assembleDebug` und `assembleDebugAndroidTest` waren ebenfalls erfolgreich.
Ein Gerätetest ist ohne sichtbare UI- oder Android-Laufzeitänderung nicht
erforderlich.
