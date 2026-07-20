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
