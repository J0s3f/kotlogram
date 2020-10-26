package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.TLContext;
import com.github.badoualy.telegram.tl.core.TLObject;
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
public class TLUserFull extends TLObject {
    public static final int CONSTRUCTOR_ID = 0xedf17c12;

    protected int flags;

    protected boolean blocked;

    protected boolean phoneCallsAvailable;

    protected boolean phoneCallsPrivate;

    protected boolean canPinMessage;

    protected boolean hasScheduled;

    protected boolean videoCallsAvailable;

    protected TLAbsUser user;

    protected String about;

    protected TLPeerSettings settings;

    protected TLAbsPhoto profilePhoto;

    protected TLPeerNotifySettings notifySettings;

    protected TLBotInfo botInfo;

    protected Integer pinnedMsgId;

    protected int commonChatsCount;

    protected Integer folderId;

    private final String _constructor = "userFull#edf17c12";

    public TLUserFull() {
    }

    public TLUserFull(boolean blocked, boolean phoneCallsAvailable, boolean phoneCallsPrivate, boolean canPinMessage, boolean hasScheduled, boolean videoCallsAvailable, TLAbsUser user, String about, TLPeerSettings settings, TLAbsPhoto profilePhoto, TLPeerNotifySettings notifySettings, TLBotInfo botInfo, Integer pinnedMsgId, int commonChatsCount, Integer folderId) {
        this.blocked = blocked;
        this.phoneCallsAvailable = phoneCallsAvailable;
        this.phoneCallsPrivate = phoneCallsPrivate;
        this.canPinMessage = canPinMessage;
        this.hasScheduled = hasScheduled;
        this.videoCallsAvailable = videoCallsAvailable;
        this.user = user;
        this.about = about;
        this.settings = settings;
        this.profilePhoto = profilePhoto;
        this.notifySettings = notifySettings;
        this.botInfo = botInfo;
        this.pinnedMsgId = pinnedMsgId;
        this.commonChatsCount = commonChatsCount;
        this.folderId = folderId;
    }

    private void computeFlags() {
        flags = 0;
        flags = blocked ? (flags | 1) : (flags & ~1);
        flags = phoneCallsAvailable ? (flags | 16) : (flags & ~16);
        flags = phoneCallsPrivate ? (flags | 32) : (flags & ~32);
        flags = canPinMessage ? (flags | 128) : (flags & ~128);
        flags = hasScheduled ? (flags | 4096) : (flags & ~4096);
        flags = videoCallsAvailable ? (flags | 8192) : (flags & ~8192);
        flags = about != null ? (flags | 2) : (flags & ~2);
        flags = profilePhoto != null ? (flags | 4) : (flags & ~4);
        flags = botInfo != null ? (flags | 8) : (flags & ~8);
        flags = pinnedMsgId != null ? (flags | 64) : (flags & ~64);
        flags = folderId != null ? (flags | 2048) : (flags & ~2048);
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        computeFlags();

        writeInt(flags, stream);
        writeTLObject(user, stream);
        if ((flags & 2) != 0) {
            if (about == null) throwNullFieldException("about", flags);
            writeString(about, stream);
        }
        writeTLObject(settings, stream);
        if ((flags & 4) != 0) {
            if (profilePhoto == null) throwNullFieldException("profilePhoto", flags);
            writeTLObject(profilePhoto, stream);
        }
        writeTLObject(notifySettings, stream);
        if ((flags & 8) != 0) {
            if (botInfo == null) throwNullFieldException("botInfo", flags);
            writeTLObject(botInfo, stream);
        }
        if ((flags & 64) != 0) {
            if (pinnedMsgId == null) throwNullFieldException("pinnedMsgId", flags);
            writeInt(pinnedMsgId, stream);
        }
        writeInt(commonChatsCount, stream);
        if ((flags & 2048) != 0) {
            if (folderId == null) throwNullFieldException("folderId", flags);
            writeInt(folderId, stream);
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        flags = readInt(stream);
        blocked = (flags & 1) != 0;
        phoneCallsAvailable = (flags & 16) != 0;
        phoneCallsPrivate = (flags & 32) != 0;
        canPinMessage = (flags & 128) != 0;
        hasScheduled = (flags & 4096) != 0;
        videoCallsAvailable = (flags & 8192) != 0;
        user = readTLObject(stream, context, TLAbsUser.class, -1);
        about = (flags & 2) != 0 ? readTLString(stream) : null;
        settings = readTLObject(stream, context, TLPeerSettings.class, TLPeerSettings.CONSTRUCTOR_ID);
        profilePhoto = (flags & 4) != 0 ? readTLObject(stream, context, TLAbsPhoto.class, -1) : null;
        notifySettings = readTLObject(stream, context, TLPeerNotifySettings.class, TLPeerNotifySettings.CONSTRUCTOR_ID);
        botInfo = (flags & 8) != 0 ? readTLObject(stream, context, TLBotInfo.class, TLBotInfo.CONSTRUCTOR_ID) : null;
        pinnedMsgId = (flags & 64) != 0 ? readInt(stream) : null;
        commonChatsCount = readInt(stream);
        folderId = (flags & 2048) != 0 ? readInt(stream) : null;
    }

    @Override
    public int computeSerializedSize() {
        computeFlags();

        int size = SIZE_CONSTRUCTOR_ID;
        size += SIZE_INT32;
        size += user.computeSerializedSize();
        if ((flags & 2) != 0) {
            if (about == null) throwNullFieldException("about", flags);
            size += computeTLStringSerializedSize(about);
        }
        size += settings.computeSerializedSize();
        if ((flags & 4) != 0) {
            if (profilePhoto == null) throwNullFieldException("profilePhoto", flags);
            size += profilePhoto.computeSerializedSize();
        }
        size += notifySettings.computeSerializedSize();
        if ((flags & 8) != 0) {
            if (botInfo == null) throwNullFieldException("botInfo", flags);
            size += botInfo.computeSerializedSize();
        }
        if ((flags & 64) != 0) {
            if (pinnedMsgId == null) throwNullFieldException("pinnedMsgId", flags);
            size += SIZE_INT32;
        }
        size += SIZE_INT32;
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

    public boolean getBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public boolean getPhoneCallsAvailable() {
        return phoneCallsAvailable;
    }

    public void setPhoneCallsAvailable(boolean phoneCallsAvailable) {
        this.phoneCallsAvailable = phoneCallsAvailable;
    }

    public boolean getPhoneCallsPrivate() {
        return phoneCallsPrivate;
    }

    public void setPhoneCallsPrivate(boolean phoneCallsPrivate) {
        this.phoneCallsPrivate = phoneCallsPrivate;
    }

    public boolean getCanPinMessage() {
        return canPinMessage;
    }

    public void setCanPinMessage(boolean canPinMessage) {
        this.canPinMessage = canPinMessage;
    }

    public boolean getHasScheduled() {
        return hasScheduled;
    }

    public void setHasScheduled(boolean hasScheduled) {
        this.hasScheduled = hasScheduled;
    }

    public boolean getVideoCallsAvailable() {
        return videoCallsAvailable;
    }

    public void setVideoCallsAvailable(boolean videoCallsAvailable) {
        this.videoCallsAvailable = videoCallsAvailable;
    }

    public TLAbsUser getUser() {
        return user;
    }

    public void setUser(TLAbsUser user) {
        this.user = user;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public TLPeerSettings getSettings() {
        return settings;
    }

    public void setSettings(TLPeerSettings settings) {
        this.settings = settings;
    }

    public TLAbsPhoto getProfilePhoto() {
        return profilePhoto;
    }

    public void setProfilePhoto(TLAbsPhoto profilePhoto) {
        this.profilePhoto = profilePhoto;
    }

    public TLPeerNotifySettings getNotifySettings() {
        return notifySettings;
    }

    public void setNotifySettings(TLPeerNotifySettings notifySettings) {
        this.notifySettings = notifySettings;
    }

    public TLBotInfo getBotInfo() {
        return botInfo;
    }

    public void setBotInfo(TLBotInfo botInfo) {
        this.botInfo = botInfo;
    }

    public Integer getPinnedMsgId() {
        return pinnedMsgId;
    }

    public void setPinnedMsgId(Integer pinnedMsgId) {
        this.pinnedMsgId = pinnedMsgId;
    }

    public int getCommonChatsCount() {
        return commonChatsCount;
    }

    public void setCommonChatsCount(int commonChatsCount) {
        this.commonChatsCount = commonChatsCount;
    }

    public Integer getFolderId() {
        return folderId;
    }

    public void setFolderId(Integer folderId) {
        this.folderId = folderId;
    }
}
