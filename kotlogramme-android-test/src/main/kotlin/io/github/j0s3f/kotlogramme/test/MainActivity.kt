package io.github.j0s3f.kotlogramme.test

import android.app.Activity
import android.os.Bundle
import android.util.Log
import org.kotlogramme.TelegramClient
import java.io.File

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread {
            try {
                val storage = File(filesDir, "test.session")
                val client = com.github.badoualy.telegram.api.Kotlogram.getDefaultClient(
                    com.github.badoualy.telegram.api.TelegramApp(12345, "0123456789abcdef0123456789abcdef"),
                    com.github.badoualy.telegram.api.FileTelegramApiStorage(storage.toPath()),
                )
                Log.i("KotlogrammeTest", "Client created OK, authorized=${client.isAuthorized()}")
                client.close()
            } catch (t: Throwable) {
                Log.e("KotlogrammeTest", "FAILED", t)
            }
        }.start()
    }
}