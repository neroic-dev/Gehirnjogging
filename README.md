# Das Blatt

Minimalistische Nachrichten-App im Stil einer gedruckten Zeitung (Android, WebView).
Die Meldungen kommen aus RSS- und Atom-Feeds.

- Aufmacher mit großer Schlagzeile, darunter Ressorts in Zeitungsspalten
- Leseansicht mit dem kompletten Text, den der Feed mitliefert (ungekürzt)
- Feeds werden nativ in der App abgerufen, also ohne CORS-Sperre und ohne Proxy
- Quellen unter „Quellen“ hinzufügen, entfernen und Ressorts zuordnen
- Gelesene Artikel werden grau, die letzte Ausgabe bleibt offline verfügbar
- Zum Aktualisieren herunterziehen, Originalartikel öffnet sich im Browser
- Hell- und Dunkelmodus folgen dem System

## APK bekommen

Direkt herunterladen (ohne ZIP, ohne Login):
**https://github.com/neroic-dev/Gehirnjogging/releases/latest/download/DasBlatt.apk**

Nach jedem Push auf `main` baut GitHub Actions die APK neu (ca. 1–2 Minuten) und ersetzt sie unter diesem Link.

Die Oberfläche liegt in `app/src/main/assets/index.html`, der Feed-Abruf in
`app/src/main/java/de/neroic/dasblatt/MainActivity.java`.

Die APK ist mit einem festen Debug-Schlüssel (`app/debug.keystore`) signiert, damit neue Builds die installierte App aktualisieren können.
