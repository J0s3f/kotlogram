package com.github.badoualy.telegram.api

import org.kotlogramme.TelegramClient as GrammersClient
import com.github.badoualy.telegram.mtproto.model.DataCenter
import java.nio.file.Path
import kotlin.random.Random

/** Entry point compatible with Kotlogram's familiar factory method. */
object Kotlogram {
    /** Kotlogram used Layer 66. This implementation delegates layer selection to grammers. */
    const val API_LAYER: Int = -1

    @JvmField val PROD_DC1 = DataCenter("149.154.175.50", 443)
    @JvmField val PROD_DC2 = DataCenter("149.154.167.51", 443)
    @JvmField val PROD_DC3 = DataCenter("149.154.175.100", 443)
    @JvmField val PROD_DC4 = DataCenter("149.154.167.91", 443)
    @JvmField val PROD_DC5 = DataCenter("91.108.56.165", 443)
    @JvmField val PROD_DCS = arrayOf(PROD_DC1, PROD_DC2, PROD_DC3, PROD_DC4, PROD_DC5)

    @JvmStatic
    @JvmOverloads
    fun getDefaultClient(
        application: TelegramApp,
        apiStorage: TelegramApiStorage,
        updateCallback: UpdateCallback? = null,
        @Suppress("UNUSED_PARAMETER") preferredDataCenter: DataCenter = PROD_DC4,
        @Suppress("UNUSED_PARAMETER") tag: String = Random.nextInt().toString(),
    ): TelegramClient = DefaultTelegramClient(
        GrammersClient.create(application.apiId, application.apiHash, apiStorage.sessionPath),
        application,
        updateCallback,
    )

    @JvmStatic
    @JvmOverloads
    fun getDefaultClient(
        application: TelegramApp,
        sessionPath: Path,
        updateCallback: UpdateCallback? = null,
        preferredDataCenter: DataCenter = PROD_DC4,
        tag: String = Random.nextInt().toString(),
    ): TelegramClient = getDefaultClient(
        application,
        FileTelegramApiStorage(sessionPath),
        updateCallback,
        preferredDataCenter,
        tag,
    )

    @JvmStatic
    fun getDcById(id: Int): DataCenter = PROD_DCS[id - 1]

    @JvmStatic
    fun getDcId(dataCenter: DataCenter): Int = PROD_DCS.indexOf(dataCenter) + 1

    /** Client shutdown is scoped to [TelegramClient.close]; no global executor is retained. */
    @JvmStatic
    fun shutdown() = Unit
}
