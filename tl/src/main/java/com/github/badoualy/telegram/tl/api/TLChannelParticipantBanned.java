package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.TLContext;
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
public class TLChannelParticipantBanned extends TLAbsChannelParticipant {
    public static final int CONSTRUCTOR_ID = 0x0;

    protected int flags;

    protected boolean left;

    protected int kickedBy;

    protected int date;

    protected TLChatBannedRights bannedRights;

    private final String _constructor = "channelParticipantBanned#0";

    public TLChannelParticipantBanned() {
    }

    public TLChannelParticipantBanned(boolean left, int userId, int kickedBy, int date, TLChatBannedRights bannedRights) {
        this.left = left;
        this.userId = userId;
        this.kickedBy = kickedBy;
        this.date = date;
        this.bannedRights = bannedRights;
    }

    private void computeFlags() {
        flags = 0;
        flags = left ? (flags | 1) : (flags & ~1);
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        computeFlags();

        writeInt(flags, stream);
        writeInt(userId, stream);
        writeInt(kickedBy, stream);
        writeInt(date, stream);
        writeTLObject(bannedRights, stream);
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        flags = readInt(stream);
        left = (flags & 1) != 0;
        userId = readInt(stream);
        kickedBy = readInt(stream);
        date = readInt(stream);
        bannedRights = readTLObject(stream, context, TLChatBannedRights.class, TLChatBannedRights.CONSTRUCTOR_ID);
    }

    @Override
    public int computeSerializedSize() {
        computeFlags();

        int size = SIZE_CONSTRUCTOR_ID;
        size += SIZE_INT32;
        size += SIZE_INT32;
        size += SIZE_INT32;
        size += SIZE_INT32;
        size += bannedRights.computeSerializedSize();
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

    public boolean getLeft() {
        return left;
    }

    public void setLeft(boolean left) {
        this.left = left;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getKickedBy() {
        return kickedBy;
    }

    public void setKickedBy(int kickedBy) {
        this.kickedBy = kickedBy;
    }

    public int getDate() {
        return date;
    }

    public void setDate(int date) {
        this.date = date;
    }

    public TLChatBannedRights getBannedRights() {
        return bannedRights;
    }

    public void setBannedRights(TLChatBannedRights bannedRights) {
        this.bannedRights = bannedRights;
    }
}
