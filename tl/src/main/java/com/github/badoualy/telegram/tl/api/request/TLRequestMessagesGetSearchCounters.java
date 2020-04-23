package com.github.badoualy.telegram.tl.api.request;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.TLContext;
import com.github.badoualy.telegram.tl.api.TLAbsInputPeer;
import com.github.badoualy.telegram.tl.api.TLAbsMessagesFilter;
import com.github.badoualy.telegram.tl.api.messages.TLSearchCounter;
import com.github.badoualy.telegram.tl.core.TLMethod;
import com.github.badoualy.telegram.tl.core.TLVector;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;

/**
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public class TLRequestMessagesGetSearchCounters extends TLMethod<TLVector<TLSearchCounter>> {
    public static final int CONSTRUCTOR_ID = 0x0;

    protected TLAbsInputPeer peer;

    protected TLVector<TLAbsMessagesFilter> filters;

    private final String _constructor = "messages.getSearchCounters#0";

    public TLRequestMessagesGetSearchCounters() {
    }

    public TLRequestMessagesGetSearchCounters(TLAbsInputPeer peer, TLVector<TLAbsMessagesFilter> filters) {
        this.peer = peer;
        this.filters = filters;
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public TLVector<TLSearchCounter> deserializeResponse(InputStream stream, TLContext context) throws IOException {
        return readTLVector(stream, context);
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeTLObject(peer, stream);
        writeTLVector(filters, stream);
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        peer = readTLObject(stream, context, TLAbsInputPeer.class, -1);
        filters = readTLVector(stream, context);
    }

    @Override
    public int computeSerializedSize() {
        int size = SIZE_CONSTRUCTOR_ID;
        size += peer.computeSerializedSize();
        size += filters.computeSerializedSize();
        return size;
    }

    @Override
    public String toString() {
        return _constructor;
    }

    @Override
    public int getConstructorId() {
        return CONSTRUCTOR_ID;
    }

    public TLAbsInputPeer getPeer() {
        return peer;
    }

    public void setPeer(TLAbsInputPeer peer) {
        this.peer = peer;
    }

    public TLVector<TLAbsMessagesFilter> getFilters() {
        return filters;
    }

    public void setFilters(TLVector<TLAbsMessagesFilter> filters) {
        this.filters = filters;
    }
}
