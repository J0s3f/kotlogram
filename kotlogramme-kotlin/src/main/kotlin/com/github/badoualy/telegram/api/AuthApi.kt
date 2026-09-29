package com.github.badoualy.telegram.api

import org.kotlogramme.protocol.SignInResult

/** Sign-in, 2FA, account-identity and sign-out operations. */
interface AuthApi : BridgeApi {
    @Suppress("UNUSED_PARAMETER")
    fun authSendCode(
        allowFlashcall: Boolean = false,
        phoneNumber: String,
        currentNumber: Boolean = false,
    ): SentCode {
        bridge.requestLoginCode(phoneNumber)
        // The native bridge retains the actual Telegram token, which is deliberately not exposed.
        return SentCode(phoneNumber, "managed-by-kotlogramme")
    }

    fun authSignIn(phoneNumber: String, phoneCodeHash: String, phoneCode: String): Authorization =
        when (val result = bridge.signIn(phoneCode)) {
            is SignInResult.Authorized -> Authorization(result.user.toCompatibility())
            is SignInResult.PasswordRequired -> throw PasswordRequiredException(result.hint)
        }

    fun authCheckPassword(password: String): Authorization =
        Authorization(bridge.checkPassword(password).toCompatibility())

    /**
     * Signs out of the account the session is authorized with.
     *
     * The client stays connected and usable for a later sign-in; only the authorization is dropped.
     * Returns whether the native side reported the sign-out as done.
     */
    fun authLogOut(): Boolean = bridge.signOut()

    /**
     * Imports a bot authorization token created by BotFather.
     *
     * Unlike the other methods this one has no default implementation: bot sign-in also needs the
     * API hash the client was created with, which only the implementation knows.
     */
    fun authImportBotAuthorization(botAuthToken: String): Authorization

    /** The account associated with the current session. */
    fun getMe(): User = bridge.getMe().user.toCompatibility()

    /**
     * The account associated with the current session together with the data centre its session is
     * homed on.
     *
     * This is the full identity; [getMe] is the same account without the data centre.
     */
    fun getAccountIdentity(): AccountIdentity = bridge.getMe().toCompatibility()

    /**
     * The home data centre of the session, which is the one its main queries run against.
     *
     * [Kotlogram.getDcById] names the same identifier.
     */
    fun getDataCentreId(): Int = bridge.getDataCentreId()

    fun contactsResolveUsername(username: String): TelegramPeer = bridge.resolveUsername(username).toCompatibility()
}
