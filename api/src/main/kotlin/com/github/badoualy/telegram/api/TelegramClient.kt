package com.github.badoualy.telegram.api

import com.github.badoualy.telegram.api.utils.InputFileLocation
import com.github.badoualy.telegram.mtproto.MTProtoHandler
import com.github.badoualy.telegram.tl.api.*
import com.github.badoualy.telegram.tl.api.auth.TLAuthorization
import com.github.badoualy.telegram.tl.api.auth.TLSentCode
import com.github.badoualy.telegram.tl.api.request.TLRequestUploadGetFile
import com.github.badoualy.telegram.tl.api.upload.TLFile
import com.github.badoualy.telegram.tl.core.TLBytes
import com.github.badoualy.telegram.tl.core.TLMethod
import com.github.badoualy.telegram.tl.core.TLObject
import com.github.badoualy.telegram.tl.exception.RpcErrorException
import rx.Observable
import java.io.IOException
import java.io.OutputStream

interface TelegramClient : TelegramApi {

    /** Changes the request timeout with the supplied value (in ms) */
    fun setTimeout(timeout: Long)

    /** Changes the timeout value for an exported handler to expire (and be closed) with the supplied value (in ms) */
    fun setExportedClientTimeout(timeout: Long)

    /** Close connection to Telegram and stop everything */
    fun close()

    /** Close connection, and clean the threads if shutdown is true */
    fun close(shutdown: Boolean)

    fun isClosed(): Boolean

    /**
     * Queue a method to be executed with the next message.
     * @param method method to execute
     * @param validityTimeout validity duration in ms, if nothing is sent during this period, this method will be discarded
     */
    fun <T : TLObject> queueMethodImmediate(method: TLMethod<T>, validityTimeout: Long)

    /**
     * Queue a method to be executed with the next message.
     * @param method method to execute
     * @param type of queue
     * @param validityTimeout validity duration in ms, if nothing is sent during this period, this method will be discarded/send depending on type
     * @param timeout request timeout (applied on the observable)
     * @return an observable that will receive one unique item being the response
     */
    fun <T : TLObject> queueMethod(method: TLMethod<T>, type: Int = MTProtoHandler.QUEUE_TYPE_DISCARD, validityTimeout: Long, timeout: Long): Observable<T>?

    fun getDownloaderClient(): TelegramClient

    //////////////////////////////////////////////////////////
    //////////////////// Convenience API ////////////////////
    ////////////////////////////////////////////////////////
    @Throws(RpcErrorException::class, IOException::class)
    fun <T : TLObject> executeRpcQuery(method: TLMethod<T>) = executeRpcQueries(
            listOf(method)).first()

    @Throws(RpcErrorException::class, IOException::class)
    fun <T : TLObject> executeRpcQuery(method: TLMethod<T>, dcId: Int) = executeRpcQueries(
            listOf(method), dcId).first()

    @Throws(RpcErrorException::class, IOException::class)
    fun <T : TLObject> executeRpcQueries(methods: List<TLMethod<T>>): List<T>

    @Throws(RpcErrorException::class, IOException::class)
    fun <T : TLObject> executeRpcQueries(methods: List<TLMethod<T>>, dcId: Int): List<T>

    @Throws(RpcErrorException::class, IOException::class)
    override fun <T : TLObject?> invokeWithLayer(layer: Int, query: TLMethod<T>?): T

    /** Convenience method wrapping the argument with TelegramApp values and casting result with good type */
    @Suppress("UNCHECKED_CAST")
    @Throws(RpcErrorException::class, IOException::class)
    fun <T : TLObject> initConnection(query: TLMethod<T>): T

}