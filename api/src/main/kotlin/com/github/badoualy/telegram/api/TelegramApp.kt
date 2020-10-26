package com.github.badoualy.telegram.api

data class TelegramApp(
        val apiId: Int,
        val apiHash: String,
        val deviceModel: String,
        val systemVersion: String,
        val appVersion: String,
        val systemLangCode: String,
        val langPack: String,
        val langCode: String
)