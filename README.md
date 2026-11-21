# Das Blatt

Minimalistische Nachrichten-App im Stil einer gedruckten Zeitung (Android, WebView).
Die Meldungen kommen aus RSS- und Atom-Feeds.

- Aufmacher mit großer Schlagzeile, darunter Ressorts in Zeitungsspalten
- Feeds werden nativ in der App abgerufen, also ohne CORS-Sperre und ohne Proxy
- Quellen unter „Quellen“ hinzufügen, entfernen und Ressorts zuordnen
- Gelesene Artikel werden grau, die letzte Ausgabe bleibt offline verfügbar
- Zum Aktualisieren herunterziehen, Artikel öffnen sich im Browser
- Hell- und Dunkelmodus folgen dem System

## APK bekommen

1. Unter **Actions** → „APK bauen“ läuft der Build nach jedem Push automatisch (ca. 2–5 Minuten).
2. Im fertigen Lauf unten bei **Artifacts** „DasBlatt-APK“ herunterladen, ZIP entpacken, `app-debug.apk` installieren.

Die Oberfläche liegt in `app/src/main/assets/index.html`, der Feed-Abruf in
`app/src/main/java/de/neroic/dasblatt/MainActivity.java`.
