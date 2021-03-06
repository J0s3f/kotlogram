package com.github.badoualy.telegram.tl.api.request;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.TLContext;
import com.github.badoualy.telegram.tl.api.TLAbsInputPeer;
import com.github.badoualy.telegram.tl.api.TLAbsInputUser;
import com.github.badoualy.telegram.tl.api.TLAbsMessagesFilter;
import com.github.badoualy.telegram.tl.api.messages.TLAbsMessages;
import com.github.badoualy.telegram.tl.core.TLMethod;
import com.github.badoualy.telegram.tl.core.TLObject;
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
public class TLRequestMessagesSearch extends TLMethod<TLAbsMessages> {
    public static final int CONSTRUCTOR_ID = 0x8614ef68;

    protected int flags;

    protected TLAbsInputPeer peer;

    protected String q;

    protected TLAbsInputUser fromId;

    protected TLAbsMessagesFilter filter;

    protected int minDate;

    protected int maxDate;

    protected int offsetId;

    protected int addOffset;

    protected int limit;

    protected int maxId;

    protected int minId;

    protected int hash;

    private final String _constructor = "messages.search#8614ef68";

    public TLRequestMessagesSearch() {
    }

    public TLRequestMessagesSearch(TLAbsInputPeer peer, String q, TLAbsInputUser fromId, TLAbsMessagesFilter filter, int minDate, int maxDate, int offsetId, int addOffset, int limit, int maxId, int minId, int hash) {
        this.peer = peer;
        this.q = q;
        this.fromId = fromId;
        this.filter = filter;
        this.minDate = minDate;
        this.maxDate = maxDate;
        this.offsetId = offsetId;
        this.addOffset = addOffset;
        this.limit = limit;
        this.maxId = maxId;
        this.minId = minId;
        this.hash = hash;
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public TLAbsMessages deserializeResponse(InputStream stream, TLContext context) throws IOException {
        final TLObject response = readTLObject(stream, context);
        if (response == null) {
            throw new IOException("Unable to parse response");
        }
        if (!(response instanceof TLAbsMessages)) {
            throw new IOException("Incorrect response type, expected " + getClass().getCanonicalName() + ", found " + response.getClass().getCanonicalName());
        }
        return (TLAbsMessages) response;
    }

    private void computeFlags() {
        flags = 0;
        flags = fromId != null ? (flags | 1) : (flags & ~1);
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        computeFlags();

        writeInt(flags, stream);
        writeTLObject(peer, stream);
        writeString(q, stream);
        if ((flags & 1) != 0) {
            if (fromId == null) throwNullFieldException("fromId", flags);
            writeTLObject(fromId, stream);
        }
        writeTLObject(filter, stream);
        writeInt(minDate, stream);
        writeInt(maxDate, stream);
        writeInt(offsetId, stream);
        writeInt(addOffset, stream);
        writeInt(limit, stream);
        writeInt(maxId, stream);
        writeInt(minId, stream);
        writeInt(hash, stream);
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        flags = readInt(stream);
        peer = readTLObject(stream, context, TLAbsInputPeer.class, -1);
        q = readTLString(stream);
        fromId = (flags & 1) != 0 ? readTLObject(stream, context, TLAbsInputUser.class, -1) : null;
        filter = readTLObject(stream, context, TLAbsMessagesFilter.class, -1);
        minDate = readInt(stream);
        maxDate = readInt(stream);
        offsetId = readInt(stream);
        addOffset = readInt(stream);
        limit = readInt(stream);
        maxId = readInt(stream);
        minId = readInt(stream);
        hash = readInt(stream);
    }

    @Override
    public int computeSerializedSize() {
        computeFlags();

        int size = SIZE_CONSTRUCTOR_ID;
        size += SIZE_INT32;
        size += peer.computeSerializedSize();
        size += computeTLStringSerializedSize(q);
        if ((flags & 1) != 0) {
            if (fromId == null) throwNullFieldException("fromId", flags);
            size += fromId.computeSerializedSize();
        }
        size += filter.computeSerializedSize();
        size += SIZE_INT32;
        size += SIZE_INT32;
        size += SIZE_INT32;
        size += SIZE_INT32;
        size += SIZE_INT32;
        size += SIZE_INT32;
        size += SIZE_INT32;
        size += SIZE_INT32;
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

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public TLAbsInputUser getFromId() {
        return fromId;
    }

    public void setFromId(TLAbsInputUser fromId) {
        this.fromId = fromId;
    }

    public TLAbsMessagesFilter getFilter() {
        return filter;
    }

    public void setFilter(TLAbsMessagesFilter filter) {
        this.filter = filter;
    }

    public int getMinDate() {
        return minDate;
    }

    public void setMinDate(int minDate) {
        this.minDate = minDate;
    }

    public int getMaxDate() {
        return maxDate;
    }

    public void setMaxDate(int maxDate) {
        this.maxDate = maxDate;
    }

    public int getOffsetId() {
        return offsetId;
    }

    public void setOffsetId(int offsetId) {
        this.offsetId = offsetId;
    }

    public int getAddOffset() {
        return addOffset;
    }

    public void setAddOffset(int addOffset) {
        this.addOffset = addOffset;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getMaxId() {
        return maxId;
    }

    public void setMaxId(int maxId) {
        this.maxId = maxId;
    }

    public int getMinId() {
        return minId;
    }

    public void setMinId(int minId) {
        this.minId = minId;
    }

    public int getHash() {
        return hash;
    }

    public void setHash(int hash) {
        this.hash = hash;
    }
}
