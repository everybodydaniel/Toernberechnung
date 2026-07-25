# Crewspace-Chat: Android-Konfiguration

## Firebase

1. In demselben Firebase-Projekt wie iOS eine Android-App mit dem Paket
   `com.example.trnberechnung` anlegen.
2. In Firebase Authentication die Anmeldemethode **E-Mail/Passwort** aktivieren.
3. Die von Firebase erzeugte Datei als `app/google-services.json` ablegen.
   Diese Datei wird nicht vom Projekt erzeugt und soll nicht aus einem anderen
   Firebase-Projekt kopiert werden.
4. Cloud Messaging für das Projekt aktivieren. Die App verwendet die aktuelle
   FID-Registrierung (`firebase_messaging_installation_id_enabled`) und sendet
   die von `onRegistered` gelieferte Installation-ID an den Server.

Der Server muss Android-Nachrichten als **high-priority Data-only-FCM** senden.
Die Data-Map enthält exakt `conversation_id`, `message_id` und `message_type`.
Insbesondere darf für Android weder ein FCM-`notification`-Block noch
`sender_name` oder Nachrichtentext enthalten sein: Ein `notification`-Block
würde bei einer App im Hintergrund direkt vom Betriebssystem angezeigt und
damit die lokale Konto- und Installation-Prüfung umgehen.

Nach Empfang prüft Android die aktuell angemeldete Firebase-UID sowie die vom
Server bestätigte UID/FID-Bindung erneut. Anschließend wird die kanonische
Nachricht bei Bedarf per REST synchronisiert und der Absendername aus der
accountbezogenen Room-Datenbank aufgelöst. Erst dann erzeugt die App lokal die
sichtbare Benachrichtigung im Channel `crewspace_messages`; sichtbar sind nur
der Absendername und „Neue Nachricht“. Beim Logout werden ausstehende
Push-Arbeiten und sichtbare Crewspace-Benachrichtigungen dieses Kontos entfernt.

Ohne `app/google-services.json` bleibt der Quellcode buildbar, Login und Push
zeigen zur Laufzeit jedoch bewusst einen Konfigurationsfehler. Es gibt keinen
lokalen Auth-Fallback und keine gespeicherten ID-Tokens.

## Server-URL und TLS

Die URL wird beim Build als Gradle-Property gesetzt und muss mit `/` enden:

```properties
CREWSPACE_BASE_URL=https://chat.example.de/
```

Sie kann beispielsweise in der benutzerspezifischen
`~/.gradle/gradle.properties` liegen. Release-Betrieb benötigt gültiges HTTPS;
der WebSocket wird daraus als `wss://…/crewspace/realtime` abgeleitet. Der
eingebaute Default `https://example.invalid/` verhindert versehentliche
Klartext- oder Testserver-Nutzung.

Der Go-Server benötigt passend dazu `PUBLIC_BASE_URL`, `FIREBASE_PROJECT_ID`,
`GOOGLE_APPLICATION_CREDENTIALS` und `UPLOAD_PUBLIC_ORIGIN`.

## Android-Berechtigungen

- Ab Android 13 fragt die App `POST_NOTIFICATIONS` zur Laufzeit ab.
- `RECORD_AUDIO` wird erst beim Start einer Sprachnachricht abgefragt.
- Bildauswahl verwendet den System-Picker; die Datei wird für die Outbox in den
  app-eigenen Speicher kopiert.

## Lokales SDK

`local.properties` muss auf ein auf diesem Rechner vorhandenes Android SDK mit
API 36 zeigen, zum Beispiel:

```properties
sdk.dir=/Users/NAME/Library/Android/sdk
```

Keine Windows-Pfade in eine macOS-Konfiguration übernehmen.

## Funktionsprüfung

1. Mit zwei verschiedenen Firebase-Konten auf Android und iOS anmelden.
2. Die im Profil angezeigte Firebase-UID des jeweils anderen Kontos verwenden.
3. Text, Bild und Audio in beide Richtungen senden.
4. Eine App offline schalten, mehr als 100 Nachrichten erzeugen und danach
   Reconnect/Delta-Sync prüfen.
5. Doppelt auf „Erneut senden“ tippen: Auf dem Server darf dank
   `client_message_id` nur eine Nachricht entstehen.
6. App in den Hintergrund legen: Push zeigt nur Absendername und
   „Neue Nachricht“ und öffnet die richtige Unterhaltung.
7. Blockieren, Senden im gesperrten Composer prüfen, danach entblocken.
8. Offline aus Konto A ausloggen und Konto B anmelden: Ein verspäteter
   Data-only-Push für A darf wegen der UID/FID-Bindung keine Benachrichtigung
   unter Konto B erzeugen.
