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
public class TLChannelFull extends TLAbsChatFull {
    public static final int CONSTRUCTOR_ID = 0xf0e6672a;

    protected boolean canViewParticipants;

    protected boolean canSetUsername;

    protected boolean canSetStickers;

    protected boolean hiddenPrehistory;

    protected boolean canSetLocation;

    protected boolean hasScheduled;

    protected boolean canViewStats;

    protected Integer participantsCount;

    protected Integer adminsCount;

    protected Integer kickedCount;

    protected Integer bannedCount;

    protected Integer onlineCount;

    protected int readInboxMaxId;

    protected int readOutboxMaxId;

    protected int unreadCount;

    protected TLAbsPhoto chatPhoto;

    protected TLVector<TLBotInfo> botInfo;

    protected Integer migratedFromChatId;

    protected Integer migratedFromMaxId;

    protected Integer pinnedMsgId;

    protected TLStickerSet stickerset;

    protected Integer availableMinId;

    protected Integer linkedChatId;

    protected TLAbsChannelLocation location;

    protected Integer slowmodeSeconds;

    protected Integer slowmodeNextSendDate;

    protected Integer statsDc;

    protected int pts;

    private final String _constructor = "channelFull#f0e6672a";

    public TLChannelFull() {
    }

    public TLChannelFull(boolean canViewParticipants, boolean canSetUsername, boolean canSetStickers, boolean hiddenPrehistory, boolean canSetLocation, boolean hasScheduled, boolean canViewStats, int id, String about, Integer participantsCount, Integer adminsCount, Integer kickedCount, Integer bannedCount, Integer onlineCount, int readInboxMaxId, int readOutboxMaxId, int unreadCount, TLAbsPhoto chatPhoto, TLPeerNotifySettings notifySettings, TLAbsExportedChatInvite exportedInvite, TLVector<TLBotInfo> botInfo, Integer migratedFromChatId, Integer migratedFromMaxId, Integer pinnedMsgId, TLStickerSet stickerset, Integer availableMinId, Integer folderId, Integer linkedChatId, TLAbsChannelLocation location, Integer slowmodeSeconds, Integer slowmodeNextSendDate, Integer statsDc, int pts) {
        this.canViewParticipants = canViewParticipants;
        this.canSetUsername = canSetUsername;
        this.canSetStickers = canSetStickers;
        this.hiddenPrehistory = hiddenPrehistory;
        this.canSetLocation = canSetLocation;
        this.hasScheduled = hasScheduled;
        this.canViewStats = canViewStats;
        this.id = id;
        this.about = about;
        this.participantsCount = participantsCount;
        this.adminsCount = adminsCount;
        this.kickedCount = kickedCount;
        this.bannedCount = bannedCount;
        this.onlineCount = onlineCount;
        this.readInboxMaxId = readInboxMaxId;
        this.readOutboxMaxId = readOutboxMaxId;
        this.unreadCount = unreadCount;
        this.chatPhoto = chatPhoto;
        this.notifySettings = notifySettings;
        this.exportedInvite = exportedInvite;
        this.botInfo = botInfo;
        this.migratedFromChatId = migratedFromChatId;
        this.migratedFromMaxId = migratedFromMaxId;
        this.pinnedMsgId = pinnedMsgId;
        this.stickerset = stickerset;
        this.availableMinId = availableMinId;
        this.folderId = folderId;
        this.linkedChatId = linkedChatId;
        this.location = location;
        this.slowmodeSeconds = slowmodeSeconds;
        this.slowmodeNextSendDate = slowmodeNextSendDate;
        this.statsDc = statsDc;
        this.pts = pts;
    }

    private void computeFlags() {
        flags = 0;
        flags = canViewParticipants ? (flags | 8) : (flags & ~8);
        flags = canSetUsername ? (flags | 64) : (flags & ~64);
        flags = canSetStickers ? (flags | 128) : (flags & ~128);
        flags = hiddenPrehistory ? (flags | 1024) : (flags & ~1024);
        flags = canSetLocation ? (flags | 65536) : (flags & ~65536);
        flags = hasScheduled ? (flags | 524288) : (flags & ~524288);
        flags = canViewStats ? (flags | 1048576) : (flags & ~1048576);
        flags = participantsCount != null ? (flags | 1) : (flags & ~1);
        flags = adminsCount != null ? (flags | 2) : (flags & ~2);
        flags = kickedCount != null ? (flags | 4) : (flags & ~4);
        flags = bannedCount != null ? (flags | 4) : (flags & ~4);
        flags = onlineCount != null ? (flags | 8192) : (flags & ~8192);
        flags = migratedFromChatId != null ? (flags | 16) : (flags & ~16);
        flags = migratedFromMaxId != null ? (flags | 16) : (flags & ~16);
        flags = pinnedMsgId != null ? (flags | 32) : (flags & ~32);
        flags = stickerset != null ? (flags | 256) : (flags & ~256);
        flags = availableMinId != null ? (flags | 512) : (flags & ~512);
        flags = folderId != null ? (flags | 2048) : (flags & ~2048);
        flags = linkedChatId != null ? (flags | 16384) : (flags & ~16384);
        flags = location != null ? (flags | 32768) : (flags & ~32768);
        flags = slowmodeSeconds != null ? (flags | 131072) : (flags & ~131072);
        flags = slowmodeNextSendDate != null ? (flags | 262144) : (flags & ~262144);
        flags = statsDc != null ? (flags | 4096) : (flags & ~4096);
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        computeFlags();

        writeInt(flags, stream);
        writeInt(id, stream);
        writeString(about, stream);
        if ((flags & 1) != 0) {
            if (participantsCount == null) throwNullFieldException("participantsCount", flags);
            writeInt(participantsCount, stream);
        }
        if ((flags & 2) != 0) {
            if (adminsCount == null) throwNullFieldException("adminsCount", flags);
            writeInt(adminsCount, stream);
        }
        if ((flags & 4) != 0) {
            if (kickedCount == null) throwNullFieldException("kickedCount", flags);
            writeInt(kickedCount, stream);
        }
        if ((flags & 4) != 0) {
            if (bannedCount == null) throwNullFieldException("bannedCount", flags);
            writeInt(bannedCount, stream);
        }
        if ((flags & 8192) != 0) {
            if (onlineCount == null) throwNullFieldException("onlineCount", flags);
            writeInt(onlineCount, stream);
        }
        writeInt(readInboxMaxId, stream);
        writeInt(readOutboxMaxId, stream);
        writeInt(unreadCount, stream);
        writeTLObject(chatPhoto, stream);
        writeTLObject(notifySettings, stream);
        writeTLObject(exportedInvite, stream);
        writeTLVector(botInfo, stream);
        if ((flags & 16) != 0) {
            if (migratedFromChatId == null) throwNullFieldException("migratedFromChatId", flags);
            writeInt(migratedFromChatId, stream);
        }
        if ((flags & 16) != 0) {
            if (migratedFromMaxId == null) throwNullFieldException("migratedFromMaxId", flags);
            writeInt(migratedFromMaxId, stream);
        }
        if ((flags & 32) != 0) {
            if (pinnedMsgId == null) throwNullFieldException("pinnedMsgId", flags);
            writeInt(pinnedMsgId, stream);
        }
        if ((flags & 256) != 0) {
            if (stickerset == null) throwNullFieldException("stickerset", flags);
            writeTLObject(stickerset, stream);
        }
        if ((flags & 512) != 0) {
            if (availableMinId == null) throwNullFieldException("availableMinId", flags);
            writeInt(availableMinId, stream);
        }
        if ((flags & 2048) != 0) {
            if (folderId == null) throwNullFieldException("folderId", flags);
            writeInt(folderId, stream);
        }
        if ((flags & 16384) != 0) {
            if (linkedChatId == null) throwNullFieldException("linkedChatId", flags);
            writeInt(linkedChatId, stream);
        }
        if ((flags & 32768) != 0) {
            if (location == null) throwNullFieldException("location", flags);
            writeTLObject(location, stream);
        }
        if ((flags & 131072) != 0) {
            if (slowmodeSeconds == null) throwNullFieldException("slowmodeSeconds", flags);
            writeInt(slowmodeSeconds, stream);
        }
        if ((flags & 262144) != 0) {
            if (slowmodeNextSendDate == null) throwNullFieldException("slowmodeNextSendDate", flags);
            writeInt(slowmodeNextSendDate, stream);
        }
        if ((flags & 4096) != 0) {
            if (statsDc == null) throwNullFieldException("statsDc", flags);
            writeInt(statsDc, stream);
        }
        writeInt(pts, stream);
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        flags = readInt(stream);
        canViewParticipants = (flags & 8) != 0;
        canSetUsername = (flags & 64) != 0;
        canSetStickers = (flags & 128) != 0;
        hiddenPrehistory = (flags & 1024) != 0;
        canSetLocation = (flags & 65536) != 0;
        hasScheduled = (flags & 524288) != 0;
        canViewStats = (flags & 1048576) != 0;
        id = readInt(stream);
        about = readTLString(stream);
        participantsCount = (flags & 1) != 0 ? readInt(stream) : null;
        adminsCount = (flags & 2) != 0 ? readInt(stream) : null;
        kickedCount = (flags & 4) != 0 ? readInt(stream) : null;
        bannedCount = (flags & 4) != 0 ? readInt(stream) : null;
        onlineCount = (flags & 8192) != 0 ? readInt(stream) : null;
        readInboxMaxId = readInt(stream);
        readOutboxMaxId = readInt(stream);
        unreadCount = readInt(stream);
        chatPhoto = readTLObject(stream, context, TLAbsPhoto.class, -1);
        notifySettings = readTLObject(stream, context, TLPeerNotifySettings.class, TLPeerNotifySettings.CONSTRUCTOR_ID);
        exportedInvite = readTLObject(stream, context, TLAbsExportedChatInvite.class, -1);
        botInfo = readTLVector(stream, context);
        migratedFromChatId = (flags & 16) != 0 ? readInt(stream) : null;
        migratedFromMaxId = (flags & 16) != 0 ? readInt(stream) : null;
        pinnedMsgId = (flags & 32) != 0 ? readInt(stream) : null;
        stickerset = (flags & 256) != 0 ? readTLObject(stream, context, TLStickerSet.class, TLStickerSet.CONSTRUCTOR_ID) : null;
        availableMinId = (flags & 512) != 0 ? readInt(stream) : null;
        folderId = (flags & 2048) != 0 ? readInt(stream) : null;
        linkedChatId = (flags & 16384) != 0 ? readInt(stream) : null;
        location = (flags & 32768) != 0 ? readTLObject(stream, context, TLAbsChannelLocation.class, -1) : null;
        slowmodeSeconds = (flags & 131072) != 0 ? readInt(stream) : null;
        slowmodeNextSendDate = (flags & 262144) != 0 ? readInt(stream) : null;
        statsDc = (flags & 4096) != 0 ? readInt(stream) : null;
        pts = readInt(stream);
    }

    @Override
    public int computeSerializedSize() {
        computeFlags();

        int size = SIZE_CONSTRUCTOR_ID;
        size += SIZE_INT32;
        size += SIZE_INT32;
        size += computeTLStringSerializedSize(about);
        if ((flags & 1) != 0) {
            if (participantsCount == null) throwNullFieldException("participantsCount", flags);
            size += SIZE_INT32;
        }
        if ((flags & 2) != 0) {
            if (adminsCount == null) throwNullFieldException("adminsCount", flags);
            size += SIZE_INT32;
        }
        if ((flags & 4) != 0) {
            if (kickedCount == null) throwNullFieldException("kickedCount", flags);
            size += SIZE_INT32;
        }
        if ((flags & 4) != 0) {
            if (bannedCount == null) throwNullFieldException("bannedCount", flags);
            size += SIZE_INT32;
        }
        if ((flags & 8192) != 0) {
            if (onlineCount == null) throwNullFieldException("onlineCount", flags);
            size += SIZE_INT32;
        }
        size += SIZE_INT32;
        size += SIZE_INT32;
        size += SIZE_INT32;
        size += chatPhoto.computeSerializedSize();
        size += notifySettings.computeSerializedSize();
        size += exportedInvite.computeSerializedSize();
        size += botInfo.computeSerializedSize();
        if ((flags & 16) != 0) {
            if (migratedFromChatId == null) throwNullFieldException("migratedFromChatId", flags);
            size += SIZE_INT32;
        }
        if ((flags & 16) != 0) {
            if (migratedFromMaxId == null) throwNullFieldException("migratedFromMaxId", flags);
            size += SIZE_INT32;
        }
        if ((flags & 32) != 0) {
            if (pinnedMsgId == null) throwNullFieldException("pinnedMsgId", flags);
            size += SIZE_INT32;
        }
        if ((flags & 256) != 0) {
            if (stickerset == null) throwNullFieldException("stickerset", flags);
            size += stickerset.computeSerializedSize();
        }
        if ((flags & 512) != 0) {
            if (availableMinId == null) throwNullFieldException("availableMinId", flags);
            size += SIZE_INT32;
        }
        if ((flags & 2048) != 0) {
            if (folderId == null) throwNullFieldException("folderId", flags);
            size += SIZE_INT32;
        }
        if ((flags & 16384) != 0) {
            if (linkedChatId == null) throwNullFieldException("linkedChatId", flags);
            size += SIZE_INT32;
        }
        if ((flags & 32768) != 0) {
            if (location == null) throwNullFieldException("location", flags);
            size += location.computeSerializedSize();
        }
        if ((flags & 131072) != 0) {
            if (slowmodeSeconds == null) throwNullFieldException("slowmodeSeconds", flags);
            size += SIZE_INT32;
        }
        if ((flags & 262144) != 0) {
            if (slowmodeNextSendDate == null) throwNullFieldException("slowmodeNextSendDate", flags);
            size += SIZE_INT32;
        }
        if ((flags & 4096) != 0) {
            if (statsDc == null) throwNullFieldException("statsDc", flags);
            size += SIZE_INT32;
        }
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

    public boolean getCanViewParticipants() {
        return canViewParticipants;
    }

    public void setCanViewParticipants(boolean canViewParticipants) {
        this.canViewParticipants = canViewParticipants;
    }

    public boolean getCanSetUsername() {
        return canSetUsername;
    }

    public void setCanSetUsername(boolean canSetUsername) {
        this.canSetUsername = canSetUsername;
    }

    public boolean getCanSetStickers() {
        return canSetStickers;
    }

    public void setCanSetStickers(boolean canSetStickers) {
        this.canSetStickers = canSetStickers;
    }

    public boolean getHiddenPrehistory() {
        return hiddenPrehistory;
    }

    public void setHiddenPrehistory(boolean hiddenPrehistory) {
        this.hiddenPrehistory = hiddenPrehistory;
    }

    public boolean getCanSetLocation() {
        return canSetLocation;
    }

    public void setCanSetLocation(boolean canSetLocation) {
        this.canSetLocation = canSetLocation;
    }

    public boolean getHasScheduled() {
        return hasScheduled;
    }

    public void setHasScheduled(boolean hasScheduled) {
        this.hasScheduled = hasScheduled;
    }

    public boolean getCanViewStats() {
        return canViewStats;
    }

    public void setCanViewStats(boolean canViewStats) {
        this.canViewStats = canViewStats;
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

    public Integer getParticipantsCount() {
        return participantsCount;
    }

    public void setParticipantsCount(Integer participantsCount) {
        this.participantsCount = participantsCount;
    }

    public Integer getAdminsCount() {
        return adminsCount;
    }

    public void setAdminsCount(Integer adminsCount) {
        this.adminsCount = adminsCount;
    }

    public Integer getKickedCount() {
        return kickedCount;
    }

    public void setKickedCount(Integer kickedCount) {
        this.kickedCount = kickedCount;
    }

    public Integer getBannedCount() {
        return bannedCount;
    }

    public void setBannedCount(Integer bannedCount) {
        this.bannedCount = bannedCount;
    }

    public Integer getOnlineCount() {
        return onlineCount;
    }

    public void setOnlineCount(Integer onlineCount) {
        this.onlineCount = onlineCount;
    }

    public int getReadInboxMaxId() {
        return readInboxMaxId;
    }

    public void setReadInboxMaxId(int readInboxMaxId) {
        this.readInboxMaxId = readInboxMaxId;
    }

    public int getReadOutboxMaxId() {
        return readOutboxMaxId;
    }

    public void setReadOutboxMaxId(int readOutboxMaxId) {
        this.readOutboxMaxId = readOutboxMaxId;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
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

    public Integer getMigratedFromChatId() {
        return migratedFromChatId;
    }

    public void setMigratedFromChatId(Integer migratedFromChatId) {
        this.migratedFromChatId = migratedFromChatId;
    }

    public Integer getMigratedFromMaxId() {
        return migratedFromMaxId;
    }

    public void setMigratedFromMaxId(Integer migratedFromMaxId) {
        this.migratedFromMaxId = migratedFromMaxId;
    }

    public Integer getPinnedMsgId() {
        return pinnedMsgId;
    }

    public void setPinnedMsgId(Integer pinnedMsgId) {
        this.pinnedMsgId = pinnedMsgId;
    }

    public TLStickerSet getStickerset() {
        return stickerset;
    }

    public void setStickerset(TLStickerSet stickerset) {
        this.stickerset = stickerset;
    }

    public Integer getAvailableMinId() {
        return availableMinId;
    }

    public void setAvailableMinId(Integer availableMinId) {
        this.availableMinId = availableMinId;
    }

    public Integer getFolderId() {
        return folderId;
    }

    public void setFolderId(Integer folderId) {
        this.folderId = folderId;
    }

    public Integer getLinkedChatId() {
        return linkedChatId;
    }

    public void setLinkedChatId(Integer linkedChatId) {
        this.linkedChatId = linkedChatId;
    }

    public TLAbsChannelLocation getLocation() {
        return location;
    }

    public void setLocation(TLAbsChannelLocation location) {
        this.location = location;
    }

    public Integer getSlowmodeSeconds() {
        return slowmodeSeconds;
    }

    public void setSlowmodeSeconds(Integer slowmodeSeconds) {
        this.slowmodeSeconds = slowmodeSeconds;
    }

    public Integer getSlowmodeNextSendDate() {
        return slowmodeNextSendDate;
    }

    public void setSlowmodeNextSendDate(Integer slowmodeNextSendDate) {
        this.slowmodeNextSendDate = slowmodeNextSendDate;
    }

    public Integer getStatsDc() {
        return statsDc;
    }

    public void setStatsDc(Integer statsDc) {
        this.statsDc = statsDc;
    }

    public int getPts() {
        return pts;
    }

    public void setPts(int pts) {
        this.pts = pts;
    }
}
