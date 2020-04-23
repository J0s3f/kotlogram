package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.TLContext;
import com.github.badoualy.telegram.tl.core.TLVector;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;

/**
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public class TLMessage extends TLAbsMessage {
    public static final int CONSTRUCTOR_ID = 0x0;

    protected int flags;

    protected boolean out;

    protected boolean mentioned;

    protected boolean mediaUnread;

    protected boolean silent;

    protected boolean post;

    protected boolean fromScheduled;

    protected boolean legacy;

    protected boolean editHide;

    protected Integer fromId;

    protected TLAbsPeer toId;

    protected TLMessageFwdHeader fwdFrom;

    protected Integer viaBotId;

    protected Integer replyToMsgId;

    protected int date;

    protected String message;

    protected TLAbsMessageMedia media;

    protected TLAbsReplyMarkup replyMarkup;

    protected TLVector<TLAbsMessageEntity> entities;

    protected Integer views;

    protected Integer editDate;

    protected String postAuthor;

    protected Long groupedId;

    protected TLVector<TLRestrictionReason> restrictionReason;

    private final String _constructor = "message#0";

    public TLMessage() {
    }

    public TLMessage(boolean out, boolean mentioned, boolean mediaUnread, boolean silent, boolean post, boolean fromScheduled, boolean legacy, boolean editHide, int id, Integer fromId, TLAbsPeer toId, TLMessageFwdHeader fwdFrom, Integer viaBotId, Integer replyToMsgId, int date, String message, TLAbsMessageMedia media, TLAbsReplyMarkup replyMarkup, TLVector<TLAbsMessageEntity> entities, Integer views, Integer editDate, String postAuthor, Long groupedId, TLVector<TLRestrictionReason> restrictionReason) {
        this.out = out;
        this.mentioned = mentioned;
        this.mediaUnread = mediaUnread;
        this.silent = silent;
        this.post = post;
        this.fromScheduled = fromScheduled;
        this.legacy = legacy;
        this.editHide = editHide;
        this.id = id;
        this.fromId = fromId;
        this.toId = toId;
        this.fwdFrom = fwdFrom;
        this.viaBotId = viaBotId;
        this.replyToMsgId = replyToMsgId;
        this.date = date;
        this.message = message;
        this.media = media;
        this.replyMarkup = replyMarkup;
        this.entities = entities;
        this.views = views;
        this.editDate = editDate;
        this.postAuthor = postAuthor;
        this.groupedId = groupedId;
        this.restrictionReason = restrictionReason;
    }

    private void computeFlags() {
        flags = 0;
        flags = out ? (flags | 2) : (flags & ~2);
        flags = mentioned ? (flags | 16) : (flags & ~16);
        flags = mediaUnread ? (flags | 32) : (flags & ~32);
        flags = silent ? (flags | 8192) : (flags & ~8192);
        flags = post ? (flags | 16384) : (flags & ~16384);
        flags = fromScheduled ? (flags | 262144) : (flags & ~262144);
        flags = legacy ? (flags | 524288) : (flags & ~524288);
        flags = editHide ? (flags | 2097152) : (flags & ~2097152);
        flags = fromId != null ? (flags | 256) : (flags & ~256);
        flags = fwdFrom != null ? (flags | 4) : (flags & ~4);
        flags = viaBotId != null ? (flags | 2048) : (flags & ~2048);
        flags = replyToMsgId != null ? (flags | 8) : (flags & ~8);
        flags = media != null ? (flags | 512) : (flags & ~512);
        flags = replyMarkup != null ? (flags | 64) : (flags & ~64);
        flags = entities != null ? (flags | 128) : (flags & ~128);
        flags = views != null ? (flags | 1024) : (flags & ~1024);
        flags = editDate != null ? (flags | 32768) : (flags & ~32768);
        flags = postAuthor != null ? (flags | 65536) : (flags & ~65536);
        flags = groupedId != null ? (flags | 131072) : (flags & ~131072);
        flags = restrictionReason != null ? (flags | 4194304) : (flags & ~4194304);
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        computeFlags();

        writeInt(flags, stream);
        writeInt(id, stream);
        if ((flags & 256) != 0) {
            if (fromId == null) throwNullFieldException("fromId", flags);
            writeInt(fromId, stream);
        }
        writeTLObject(toId, stream);
        if ((flags & 4) != 0) {
            if (fwdFrom == null) throwNullFieldException("fwdFrom", flags);
            writeTLObject(fwdFrom, stream);
        }
        if ((flags & 2048) != 0) {
            if (viaBotId == null) throwNullFieldException("viaBotId", flags);
            writeInt(viaBotId, stream);
        }
        if ((flags & 8) != 0) {
            if (replyToMsgId == null) throwNullFieldException("replyToMsgId", flags);
            writeInt(replyToMsgId, stream);
        }
        writeInt(date, stream);
        writeString(message, stream);
        if ((flags & 512) != 0) {
            if (media == null) throwNullFieldException("media", flags);
            writeTLObject(media, stream);
        }
        if ((flags & 64) != 0) {
            if (replyMarkup == null) throwNullFieldException("replyMarkup", flags);
            writeTLObject(replyMarkup, stream);
        }
        if ((flags & 128) != 0) {
            if (entities == null) throwNullFieldException("entities", flags);
            writeTLVector(entities, stream);
        }
        if ((flags & 1024) != 0) {
            if (views == null) throwNullFieldException("views", flags);
            writeInt(views, stream);
        }
        if ((flags & 32768) != 0) {
            if (editDate == null) throwNullFieldException("editDate", flags);
            writeInt(editDate, stream);
        }
        if ((flags & 65536) != 0) {
            if (postAuthor == null) throwNullFieldException("postAuthor", flags);
            writeString(postAuthor, stream);
        }
        if ((flags & 131072) != 0) {
            if (groupedId == null) throwNullFieldException("groupedId", flags);
            writeLong(groupedId, stream);
        }
        if ((flags & 4194304) != 0) {
            if (restrictionReason == null) throwNullFieldException("restrictionReason", flags);
            writeTLVector(restrictionReason, stream);
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        flags = readInt(stream);
        out = (flags & 2) != 0;
        mentioned = (flags & 16) != 0;
        mediaUnread = (flags & 32) != 0;
        silent = (flags & 8192) != 0;
        post = (flags & 16384) != 0;
        fromScheduled = (flags & 262144) != 0;
        legacy = (flags & 524288) != 0;
        editHide = (flags & 2097152) != 0;
        id = readInt(stream);
        fromId = (flags & 256) != 0 ? readInt(stream) : null;
        toId = readTLObject(stream, context, TLAbsPeer.class, -1);
        fwdFrom = (flags & 4) != 0 ? readTLObject(stream, context, TLMessageFwdHeader.class, TLMessageFwdHeader.CONSTRUCTOR_ID) : null;
        viaBotId = (flags & 2048) != 0 ? readInt(stream) : null;
        replyToMsgId = (flags & 8) != 0 ? readInt(stream) : null;
        date = readInt(stream);
        message = readTLString(stream);
        media = (flags & 512) != 0 ? readTLObject(stream, context, TLAbsMessageMedia.class, -1) : null;
        replyMarkup = (flags & 64) != 0 ? readTLObject(stream, context, TLAbsReplyMarkup.class, -1) : null;
        entities = (flags & 128) != 0 ? readTLVector(stream, context) : null;
        views = (flags & 1024) != 0 ? readInt(stream) : null;
        editDate = (flags & 32768) != 0 ? readInt(stream) : null;
        postAuthor = (flags & 65536) != 0 ? readTLString(stream) : null;
        groupedId = (flags & 131072) != 0 ? readLong(stream) : null;
        restrictionReason = (flags & 4194304) != 0 ? readTLVector(stream, context) : null;
    }

    @Override
    public int computeSerializedSize() {
        computeFlags();

        int size = SIZE_CONSTRUCTOR_ID;
        size += SIZE_INT32;
        size += SIZE_INT32;
        if ((flags & 256) != 0) {
            if (fromId == null) throwNullFieldException("fromId", flags);
            size += SIZE_INT32;
        }
        size += toId.computeSerializedSize();
        if ((flags & 4) != 0) {
            if (fwdFrom == null) throwNullFieldException("fwdFrom", flags);
            size += fwdFrom.computeSerializedSize();
        }
        if ((flags & 2048) != 0) {
            if (viaBotId == null) throwNullFieldException("viaBotId", flags);
            size += SIZE_INT32;
        }
        if ((flags & 8) != 0) {
            if (replyToMsgId == null) throwNullFieldException("replyToMsgId", flags);
            size += SIZE_INT32;
        }
        size += SIZE_INT32;
        size += computeTLStringSerializedSize(message);
        if ((flags & 512) != 0) {
            if (media == null) throwNullFieldException("media", flags);
            size += media.computeSerializedSize();
        }
        if ((flags & 64) != 0) {
            if (replyMarkup == null) throwNullFieldException("replyMarkup", flags);
            size += replyMarkup.computeSerializedSize();
        }
        if ((flags & 128) != 0) {
            if (entities == null) throwNullFieldException("entities", flags);
            size += entities.computeSerializedSize();
        }
        if ((flags & 1024) != 0) {
            if (views == null) throwNullFieldException("views", flags);
            size += SIZE_INT32;
        }
        if ((flags & 32768) != 0) {
            if (editDate == null) throwNullFieldException("editDate", flags);
            size += SIZE_INT32;
        }
        if ((flags & 65536) != 0) {
            if (postAuthor == null) throwNullFieldException("postAuthor", flags);
            size += computeTLStringSerializedSize(postAuthor);
        }
        if ((flags & 131072) != 0) {
            if (groupedId == null) throwNullFieldException("groupedId", flags);
            size += SIZE_INT64;
        }
        if ((flags & 4194304) != 0) {
            if (restrictionReason == null) throwNullFieldException("restrictionReason", flags);
            size += restrictionReason.computeSerializedSize();
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

    public boolean getOut() {
        return out;
    }

    public void setOut(boolean out) {
        this.out = out;
    }

    public boolean getMentioned() {
        return mentioned;
    }

    public void setMentioned(boolean mentioned) {
        this.mentioned = mentioned;
    }

    public boolean getMediaUnread() {
        return mediaUnread;
    }

    public void setMediaUnread(boolean mediaUnread) {
        this.mediaUnread = mediaUnread;
    }

    public boolean getSilent() {
        return silent;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    public boolean getPost() {
        return post;
    }

    public void setPost(boolean post) {
        this.post = post;
    }

    public boolean getFromScheduled() {
        return fromScheduled;
    }

    public void setFromScheduled(boolean fromScheduled) {
        this.fromScheduled = fromScheduled;
    }

    public boolean getLegacy() {
        return legacy;
    }

    public void setLegacy(boolean legacy) {
        this.legacy = legacy;
    }

    public boolean getEditHide() {
        return editHide;
    }

    public void setEditHide(boolean editHide) {
        this.editHide = editHide;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getFromId() {
        return fromId;
    }

    public void setFromId(Integer fromId) {
        this.fromId = fromId;
    }

    public TLAbsPeer getToId() {
        return toId;
    }

    public void setToId(TLAbsPeer toId) {
        this.toId = toId;
    }

    public TLMessageFwdHeader getFwdFrom() {
        return fwdFrom;
    }

    public void setFwdFrom(TLMessageFwdHeader fwdFrom) {
        this.fwdFrom = fwdFrom;
    }

    public Integer getViaBotId() {
        return viaBotId;
    }

    public void setViaBotId(Integer viaBotId) {
        this.viaBotId = viaBotId;
    }

    public Integer getReplyToMsgId() {
        return replyToMsgId;
    }

    public void setReplyToMsgId(Integer replyToMsgId) {
        this.replyToMsgId = replyToMsgId;
    }

    public int getDate() {
        return date;
    }

    public void setDate(int date) {
        this.date = date;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public TLAbsMessageMedia getMedia() {
        return media;
    }

    public void setMedia(TLAbsMessageMedia media) {
        this.media = media;
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

    public Integer getViews() {
        return views;
    }

    public void setViews(Integer views) {
        this.views = views;
    }

    public Integer getEditDate() {
        return editDate;
    }

    public void setEditDate(Integer editDate) {
        this.editDate = editDate;
    }

    public String getPostAuthor() {
        return postAuthor;
    }

    public void setPostAuthor(String postAuthor) {
        this.postAuthor = postAuthor;
    }

    public Long getGroupedId() {
        return groupedId;
    }

    public void setGroupedId(Long groupedId) {
        this.groupedId = groupedId;
    }

    public TLVector<TLRestrictionReason> getRestrictionReason() {
        return restrictionReason;
    }

    public void setRestrictionReason(TLVector<TLRestrictionReason> restrictionReason) {
        this.restrictionReason = restrictionReason;
    }
}
