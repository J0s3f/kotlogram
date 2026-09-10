package com.github.badoualy.telegram.api

/**
 * Application identity shown by Telegram in the account's active sessions list.
 *
 * This retains Kotlogram's constructor shape. grammers owns the low-level init-connection data.
 */
data class TelegramApp(
    val apiId: Int,
    val apiHash: String,
    val deviceModel: String = "kotlogramme",
    val systemVersion: String = System.getProperty("os.name") ?: "JVM",
    val appVersion: String = "0.1.0",
    val langCode: String = "en",
)
