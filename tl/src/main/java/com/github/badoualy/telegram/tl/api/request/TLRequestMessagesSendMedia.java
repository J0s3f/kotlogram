package com.github.badoualy.telegram.tl.api.request;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.TLContext;
import com.github.badoualy.telegram.tl.api.TLAbsInputMedia;
import com.github.badoualy.telegram.tl.api.TLAbsInputPeer;
import com.github.badoualy.telegram.tl.api.TLAbsMessageEntity;
import com.github.badoualy.telegram.tl.api.TLAbsReplyMarkup;
import com.github.badoualy.telegram.tl.api.TLAbsUpdates;
import com.github.badoualy.telegram.tl.core.TLMethod;
import com.github.badoualy.telegram.tl.core.TLObject;
import com.github.badoualy.telegram.tl.core.TLVector;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;

/**
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public class TLRequestMessagesSendMedia extends TLMethod<TLAbsUpdates> {
    public static final int CONSTRUCTOR_ID = 0x3491eba9;

    protected int flags;

    protected boolean silent;

    protected boolean background;

    protected boolean clearDraft;

    protected TLAbsInputPeer peer;

    protected Integer replyToMsgId;

    protected TLAbsInputMedia media;

    protected String message;

    protected long randomId;

    protected TLAbsReplyMarkup replyMarkup;

    protected TLVector<TLAbsMessageEntity> entities;

    protected Integer scheduleDate;

    private final String _constructor = "messages.sendMedia#3491eba9";

    public TLRequestMessagesSendMedia() {
    }

    public TLRequestMessagesSendMedia(boolean silent, boolean background, boolean clearDraft, TLAbsInputPeer peer, Integer replyToMsgId, TLAbsInputMedia media, String message, long randomId, TLAbsReplyMarkup replyMarkup, TLVector<TLAbsMessageEntity> entities, Integer scheduleDate) {
        this.silent = silent;
        this.background = background;
        this.clearDraft = clearDraft;
        this.peer = peer;
        this.replyToMsgId = replyToMsgId;
        this.media = media;
        this.message = message;
        this.randomId = randomId;
        this.replyMarkup = replyMarkup;
        this.entities = entities;
        this.scheduleDate = scheduleDate;
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public TLAbsUpdates deserializeResponse(InputStream stream, TLContext context) throws IOException {
        final TLObject response = readTLObject(stream, context);
        if (response == null) {
            throw new IOException("Unable to parse response");
        }
        if (!(response instanceof TLAbsUpdates)) {
            throw new IOException("Incorrect response type, expected " + getClass().getCanonicalName() + ", found " + response.getClass().getCanonicalName());
        }
        return (TLAbsUpdates) response;
    }

    private void computeFlags() {
        flags = 0;
        flags = silent ? (flags | 32) : (flags & ~32);
        flags = background ? (flags | 64) : (flags & ~64);
        flags = clearDraft ? (flags | 128) : (flags & ~128);
        flags = replyToMsgId != null ? (flags | 1) : (flags & ~1);
        flags = replyMarkup != null ? (flags | 4) : (flags & ~4);
        flags = entities != null ? (flags | 8) : (flags & ~8);
        flags = scheduleDate != null ? (flags | 1024) : (flags & ~1024);
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        computeFlags();

        writeInt(flags, stream);
        writeTLObject(peer, stream);
        if ((flags & 1) != 0) {
            if (replyToMsgId == null) throwNullFieldException("replyToMsgId", flags);
            writeInt(replyToMsgId, stream);
        }
        writeTLObject(media, stream);
        writeString(message, stream);
        writeLong(randomId, stream);
        if ((flags & 4) != 0) {
            if (replyMarkup == null) throwNullFieldException("replyMarkup", flags);
            writeTLObject(replyMarkup, stream);
        }
        if ((flags & 8) != 0) {
            if (entities == null) throwNullFieldException("entities", flags);
            writeTLVector(entities, stream);
        }
        if ((flags & 1024) != 0) {
            if (scheduleDate == null) throwNullFieldException("scheduleDate", flags);
            writeInt(scheduleDate, stream);
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        flags = readInt(stream);
        silent = (flags & 32) != 0;
        background = (flags & 64) != 0;
        clearDraft = (flags & 128) != 0;
        peer = readTLObject(stream, context, TLAbsInputPeer.class, -1);
        replyToMsgId = (flags & 1) != 0 ? readInt(stream) : null;
        media = readTLObject(stream, context, TLAbsInputMedia.class, -1);
        message = readTLString(stream);
        randomId = readLong(stream);
        replyMarkup = (flags & 4) != 0 ? readTLObject(stream, context, TLAbsReplyMarkup.class, -1) : null;
        entities = (flags & 8) != 0 ? readTLVector(stream, context) : null;
        scheduleDate = (flags & 1024) != 0 ? readInt(stream) : null;
    }

    @Override
    public int computeSerializedSize() {
        computeFlags();

        int size = SIZE_CONSTRUCTOR_ID;
        size += SIZE_INT32;
        size += peer.computeSerializedSize();
        if ((flags & 1) != 0) {
            if (replyToMsgId == null) throwNullFieldException("replyToMsgId", flags);
            size += SIZE_INT32;
        }
        size += media.computeSerializedSize();
        size += computeTLStringSerializedSize(message);
        size += SIZE_INT64;
        if ((flags & 4) != 0) {
            if (replyMarkup == null) throwNullFieldException("replyMarkup", flags);
            size += replyMarkup.computeSerializedSize();
        }
        if ((flags & 8) != 0) {
            if (entities == null) throwNullFieldException("entities", flags);
            size += entities.computeSerializedSize();
        }
        if ((flags & 1024) != 0) {
            if (scheduleDate == null) throwNullFieldException("scheduleDate", flags);
            size += SIZE_INT32;
        }
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

    public boolean getSilent() {
        return silent;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    public boolean getBackground() {
        return background;
    }

    public void setBackground(boolean background) {
        this.background = background;
    }

    public boolean getClearDraft() {
        return clearDraft;
    }

    public void setClearDraft(boolean clearDraft) {
        this.clearDraft = clearDraft;
    }

    public TLAbsInputPeer getPeer() {
        return peer;
    }

    public void setPeer(TLAbsInputPeer peer) {
        this.peer = peer;
    }

    public Integer getReplyToMsgId() {
        return replyToMsgId;
    }

    public void setReplyToMsgId(Integer replyToMsgId) {
        this.replyToMsgId = replyToMsgId;
    }

    public TLAbsInputMedia getMedia() {
        return media;
    }

    public void setMedia(TLAbsInputMedia media) {
        this.media = media;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getRandomId() {
        return randomId;
    }

    public void setRandomId(long randomId) {
        this.randomId = randomId;
    }

    public TLAbsReplyMarkup getReplyMarkup() {
        return replyMarkup;
    }

    public void setReplyMarkup(TLAbsReplyMarkup replyMarkup) {
        this.replyMarkup = replyMarkup;
    }

    public TLVector<TLAbsMessageEntity> getEntities() {
        return entities;
    }

    public void setEntities(TLVector<TLAbsMessageEntity> entities) {
        this.entities = entities;
    }

    public Integer getScheduleDate() {
        return scheduleDate;
    }

    public void setScheduleDate(Integer scheduleDate) {
        this.scheduleDate = scheduleDate;
    }
}
