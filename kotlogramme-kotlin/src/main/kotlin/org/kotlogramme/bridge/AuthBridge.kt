package org.kotlogramme.bridge

import org.kotlogramme.Operation
import org.kotlogramme.TelegramException
import org.kotlogramme.Transport
import org.kotlogramme.protocol.CodePayload
import org.kotlogramme.protocol.DataCentreResult
import org.kotlogramme.protocol.EmptyPayload
import org.kotlogramme.protocol.LoginCodeSent
import org.kotlogramme.protocol.Me
import org.kotlogramme.protocol.OperationResult
import org.kotlogramme.protocol.PasswordPayload
import org.kotlogramme.protocol.Peer
import org.kotlogramme.protocol.PeerTarget
import org.kotlogramme.protocol.PhonePayload
import org.kotlogramme.protocol.SignInResponse
import org.kotlogramme.protocol.SignInResult
import org.kotlogramme.protocol.User

/** Signing in, and resolving who the session belongs to. */
internal interface AuthBridge {
    val transport: Transport

    /** Requests the code needed to sign in to a regular user account. */
    @Operation("requestLoginCode")
    fun requestLoginCode(phone: String): LoginCodeSent =
        transport.request("requestLoginCode", PhonePayload(phone))

    /** Completes the code step of a user login. */
    @Operation("signIn")
    fun signIn(code: String): SignInResult {
        val response: SignInResponse = transport.request("signIn", CodePayload(code))
        return when (response.status) {
            "authorized" -> SignInResult.Authorized(requireNotNull(response.user))
            "passwordRequired" -> SignInResult.PasswordRequired(response.hint)
            else -> throw TelegramException("Unexpected sign-in response: ${response.status}")
        }
    }

    /** Completes a two-factor-authentication login after [signIn] returned PasswordRequired. */
    @Operation("checkPassword")
    fun checkPassword(password: String): User = transport.request("checkPassword", PasswordPayload(password))

    /**
     * Signs the session out of the account it is authorized with.
     *
     * grammers keeps the client connected; only the authorization is dropped, so a later call has
     * to sign in again. Returns whether the sign-out was reported as done.
     */
    @Operation("signOut")
    fun signOut(): Boolean =
        transport.request<EmptyPayload, OperationResult>("signOut", EmptyPayload).ok

    /** Fetches the account associated with the current session, with the data centre it lives on. */
    @Operation("getMe")
    fun getMe(): Me = transport.request("getMe", EmptyPayload)

    /**
     * Fetches the peer the session's account is itself, which addresses Saved Messages.
     *
     * The private chat with yourself is the peer `inputPeerSelf`, not a chat id: it never appears
     * in the dialog listing, so this is the only way to reach it. The answer is an ordinary peer
     * with a registered handle, so every peer-taking operation can target it.
     */
    @Operation("getSelfPeer")
    fun getSelfPeer(): Peer = transport.request("getSelfPeer", EmptyPayload)

    /** The home data centre of the session, which is what a raw call defaults to. */
    @Operation("getDataCentreId")
    fun getDataCentreId(): Int =
        transport.request<EmptyPayload, DataCentreResult>("getDataCentreId", EmptyPayload).dataCentreId

    /** Resolves a public @username into a peer handle usable by the other operations. */
    @Operation("resolveUsername")
    fun resolveUsername(username: String): Peer =
        transport.request("resolveUsername", PeerTarget(username = username.removePrefix("@")))
}

/** The [AuthBridge] [org.kotlogramme.TelegramClient] delegates to. */
internal class AuthOperations(override val transport: Transport) : AuthBridge
