package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.TLContext;
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
public class TLChatFull extends TLAbsChatFull {
    public static final int CONSTRUCTOR_ID = 0x1b7c9db3;

    protected boolean canSetUsername;

    protected boolean hasScheduled;

    protected TLAbsChatParticipants participants;

    protected TLAbsPhoto chatPhoto;

    protected TLVector<TLBotInfo> botInfo;

    protected Integer pinnedMsgId;

    private final String _constructor = "chatFull#1b7c9db3";

    public TLChatFull() {
    }

    public TLChatFull(boolean canSetUsername, boolean hasScheduled, int id, String about, TLAbsChatParticipants participants, TLAbsPhoto chatPhoto, TLPeerNotifySettings notifySettings, TLAbsExportedChatInvite exportedInvite, TLVector<TLBotInfo> botInfo, Integer pinnedMsgId, Integer folderId) {
        this.canSetUsername = canSetUsername;
        this.hasScheduled = hasScheduled;
        this.id = id;
        this.about = about;
        this.participants = participants;
        this.chatPhoto = chatPhoto;
        this.notifySettings = notifySettings;
        this.exportedInvite = exportedInvite;
        this.botInfo = botInfo;
        this.pinnedMsgId = pinnedMsgId;
        this.folderId = folderId;
    }

    private void computeFlags() {
        flags = 0;
        flags = canSetUsername ? (flags | 128) : (flags & ~128);
        flags = hasScheduled ? (flags | 256) : (flags & ~256);
        flags = chatPhoto != null ? (flags | 4) : (flags & ~4);
        flags = botInfo != null ? (flags | 8) : (flags & ~8);
        flags = pinnedMsgId != null ? (flags | 64) : (flags & ~64);
        flags = folderId != null ? (flags | 2048) : (flags & ~2048);
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        computeFlags();

        writeInt(flags, stream);
        writeInt(id, stream);
        writeString(about, stream);
        writeTLObject(participants, stream);
        if ((flags & 4) != 0) {
            if (chatPhoto == null) throwNullFieldException("chatPhoto", flags);
            writeTLObject(chatPhoto, stream);
        }
        writeTLObject(notifySettings, stream);
        writeTLObject(exportedInvite, stream);
        if ((flags & 8) != 0) {
            if (botInfo == null) throwNullFieldException("botInfo", flags);
            writeTLVector(botInfo, stream);
        }
        if ((flags & 64) != 0) {
            if (pinnedMsgId == null) throwNullFieldException("pinnedMsgId", flags);
            writeInt(pinnedMsgId, stream);
        }
        if ((flags & 2048) != 0) {
            if (folderId == null) throwNullFieldException("folderId", flags);
            writeInt(folderId, stream);
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        flags = readInt(stream);
        canSetUsername = (flags & 128) != 0;
        hasScheduled = (flags & 256) != 0;
        id = readInt(stream);
        about = readTLString(stream);
        participants = readTLObject(stream, context, TLAbsChatParticipants.class, -1);
        chatPhoto = (flags & 4) != 0 ? readTLObject(stream, context, TLAbsPhoto.class, -1) : null;
        notifySettings = readTLObject(stream, context, TLPeerNotifySettings.class, TLPeerNotifySettings.CONSTRUCTOR_ID);
        exportedInvite = readTLObject(stream, context, TLAbsExportedChatInvite.class, -1);
        botInfo = (flags & 8) != 0 ? readTLVector(stream, context) : null;
        pinnedMsgId = (flags & 64) != 0 ? readInt(stream) : null;
        folderId = (flags & 2048) != 0 ? readInt(stream) : null;
    }

    @Override
    public int computeSerializedSize() {
        computeFlags();

        int size = SIZE_CONSTRUCTOR_ID;
        size += SIZE_INT32;
        size += SIZE_INT32;
        size += computeTLStringSerializedSize(about);
        size += participants.computeSerializedSize();
        if ((flags & 4) != 0) {
            if (chatPhoto == null) throwNullFieldException("chatPhoto", flags);
            size += chatPhoto.computeSerializedSize();
        }
        size += notifySettings.computeSerializedSize();
        size += exportedInvite.computeSerializedSize();
        if ((flags & 8) != 0) {
            if (botInfo == null) throwNullFieldException("botInfo", flags);
            size += botInfo.computeSerializedSize();
        }
        if ((flags & 64) != 0) {
            if (pinnedMsgId == null) throwNullFieldException("pinnedMsgId", flags);
            size += SIZE_INT32;
        }
        if ((flags & 2048) != 0) {
            if (folderId == null) throwNullFieldException("folderId", flags);
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

    public boolean getCanSetUsername() {
        return canSetUsername;
    }

    public void setCanSetUsername(boolean canSetUsername) {
        this.canSetUsername = canSetUsername;
    }

    public boolean getHasScheduled() {
        return hasScheduled;
    }

    public void setHasScheduled(boolean hasScheduled) {
        this.hasScheduled = hasScheduled;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public TLAbsChatParticipants getParticipants() {
        return participants;
    }

    public void setParticipants(TLAbsChatParticipants participants) {
        this.participants = participants;
    }

    public TLAbsPhoto getChatPhoto() {
        return chatPhoto;
    }

    public void setChatPhoto(TLAbsPhoto chatPhoto) {
        this.chatPhoto = chatPhoto;
    }

    public TLPeerNotifySettings getNotifySettings() {
        return notifySettings;
    }

    public void setNotifySettings(TLPeerNotifySettings notifySettings) {
        this.notifySettings = notifySettings;
    }

    public TLAbsExportedChatInvite getExportedInvite() {
        return exportedInvite;
    }

    public void setExportedInvite(TLAbsExportedChatInvite exportedInvite) {
        this.exportedInvite = exportedInvite;
    }

    public TLVector<TLBotInfo> getBotInfo() {
        return botInfo;
    }

    public void setBotInfo(TLVector<TLBotInfo> botInfo) {
        this.botInfo = botInfo;
    }

    public Integer getPinnedMsgId() {
        return pinnedMsgId;
    }

    public void setPinnedMsgId(Integer pinnedMsgId) {
        this.pinnedMsgId = pinnedMsgId;
    }

    public Integer getFolderId() {
        return folderId;
    }

    public void setFolderId(Integer folderId) {
        this.folderId = folderId;
    }
}
