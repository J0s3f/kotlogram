# Kotlogram (WIP)

> **Work in progress:** Dieses Repository ersetzt die archivierte Implementierung schrittweise. Es zielt darauf ab, die bekannte Kotlogram-API weiter anzubieten, während das Telegram-Protokoll durch das Rust-Projekt [grammers](https://codeberg.org/Lonami/grammers) umgesetzt wird.

Eine Kotlin/JVM-Fassade für die Telegram-API, die auf grammers aufbaut und die Bedienbarkeit von [Kotlogram](https://github.com/badoualy/kotlogram) übernimmt.

## Status

Die erste Kompatibilitäts-Schicht ist implementiert, aber noch nicht vollständig:

- persistente SQLite-Sessions über grammers
- Bot- und User-Authentifizierung inklusive 2FA-Schritt
- Autorisierungsstatus
- öffentliche Usernames auflösen
- Textnachrichten senden, ändern, löschen und als gelesen markieren
- Dialoge und Nachrichtenhistorie laden sowie Chats beitreten/verlassen
- bekannte Kotlogram-Einstiegspunkte unter `com.github.badoualy.telegram.api`
- Native-Library-Laden aus dem JAR
- CI-Build für Linux x86_64, macOS x86_64/arm64 und Windows x86_64

Noch offen sind insbesondere die vollständige Abdeckung der historischen generierten TL-API und ein geordneter Update-Stream. Anwendungen sollten die Bibliothek daher derzeit als WIP behandeln und die abgebildeten Funktionen gezielt testen.

Die alte Kotlogram-Codebasis ist archiviert und verwendet eine alte Telegram-TL-Schicht. Deshalb ist dieses Projekt bewusst keine Quellcode-Kopie, sondern eine neue, kompatible Kotlin-Fassade mit grammers als Protokoll- und Update-Schicht.

## Verwendung

```kotlin
import com.github.badoualy.telegram.api.FileTelegramApiStorage
import com.github.badoualy.telegram.api.Kotlogram
import com.github.badoualy.telegram.api.TelegramApp
import java.nio.file.Path

Kotlogram.getDefaultClient(
    TelegramApp(System.getenv("TG_API_ID").toInt(), System.getenv("TG_API_HASH")),
    FileTelegramApiStorage(Path.of("bot.session")),
).use { client ->
    if (!client.isAuthorized()) {
        client.authImportBotAuthorization(System.getenv("TG_BOT_TOKEN"))
    }
    val peer = client.contactsResolveUsername("some_public_username")
    client.messagesSendMessage(peer, "Hallo von Kotlin")
}
```

Die API-ID und der API-Hash stammen aus `my.telegram.org`; der Bot-Token stammt aus BotFather. Sessions enthalten sensible Authentifizierungsdaten und dürfen nicht in Git eingecheckt werden.

## Architektur

`kotlogramme-kotlin` enthält die öffentliche JVM-API und den Native-Library-Loader. Die Drop-in-orientierte Fassade liegt unter `com.github.badoualy.telegram.api`; `org.kotlogramme` ist die kleinere direkte Bridge. `native` startet einen Tokio-Runtime-Thread und ruft die grammers-Client-API auf. Die GitHub Actions bauen native Bibliotheken für Windows x86_64, Linux x86_64 sowie macOS x86_64/arm64. Der Packaging-Job bündelt alle vier Varianten als Ressourcen im Maven-JAR; beim Start extrahiert und lädt der Loader ausschließlich die Variante für das aktuelle Betriebssystem und die aktuelle Architektur. Die Bridge pinnt grammers aktuell auf 0.8.1, weil die neueren crates.io-Releases im aktuellen Dependency-Stand nicht reproduzierbar bauen (0.9 referenziert eine zurückgezogene Dependency, 0.10 hat einen inkompatiblen Transitive-Dependency-Graph).

Kotlogram hatte generierte Layer-66-TL-Klassen. Diese werden nicht als „kompatibel“ nachgebaut, weil sie gegen aktuelle Telegram-Layer nicht zuverlässig funktionieren würden. Nicht abgedeckte Spezialaufrufe werden über eine versionierte Raw-API ergänzt; die genaue Zuordnung der bereits abgebildeten Kernfunktionen steht in `docs/compatibility.md`.

## Lizenz

Dieses Projekt steht unter Apache-2.0. grammers ist wahlweise unter Apache-2.0 oder MIT lizenziert; die Lizenz- und Copyright-Hinweise der Abhängigkeit bleiben maßgeblich.
