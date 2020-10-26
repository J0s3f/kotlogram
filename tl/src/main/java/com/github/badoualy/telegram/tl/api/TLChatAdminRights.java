package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.TLContext;
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
public class TLChatAdminRights extends TLObject {
    public static final int CONSTRUCTOR_ID = 0x5fb224d5;

    protected int flags;

    protected boolean changeInfo;

    protected boolean postMessages;

    protected boolean editMessages;

    protected boolean deleteMessages;

    protected boolean banUsers;

    protected boolean inviteUsers;

    protected boolean pinMessages;

    protected boolean addAdmins;

    protected boolean anonymous;

    private final String _constructor = "chatAdminRights#5fb224d5";

    public TLChatAdminRights() {
    }

    public TLChatAdminRights(boolean changeInfo, boolean postMessages, boolean editMessages, boolean deleteMessages, boolean banUsers, boolean inviteUsers, boolean pinMessages, boolean addAdmins, boolean anonymous) {
        this.changeInfo = changeInfo;
        this.postMessages = postMessages;
        this.editMessages = editMessages;
        this.deleteMessages = deleteMessages;
        this.banUsers = banUsers;
        this.inviteUsers = inviteUsers;
        this.pinMessages = pinMessages;
        this.addAdmins = addAdmins;
        this.anonymous = anonymous;
    }

    private void computeFlags() {
        flags = 0;
        flags = changeInfo ? (flags | 1) : (flags & ~1);
        flags = postMessages ? (flags | 2) : (flags & ~2);
        flags = editMessages ? (flags | 4) : (flags & ~4);
        flags = deleteMessages ? (flags | 8) : (flags & ~8);
        flags = banUsers ? (flags | 16) : (flags & ~16);
        flags = inviteUsers ? (flags | 32) : (flags & ~32);
        flags = pinMessages ? (flags | 128) : (flags & ~128);
        flags = addAdmins ? (flags | 512) : (flags & ~512);
        flags = anonymous ? (flags | 1024) : (flags & ~1024);
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        computeFlags();

        writeInt(flags, stream);
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        flags = readInt(stream);
        changeInfo = (flags & 1) != 0;
        postMessages = (flags & 2) != 0;
        editMessages = (flags & 4) != 0;
        deleteMessages = (flags & 8) != 0;
        banUsers = (flags & 16) != 0;
        inviteUsers = (flags & 32) != 0;
        pinMessages = (flags & 128) != 0;
        addAdmins = (flags & 512) != 0;
        anonymous = (flags & 1024) != 0;
    }

    @Override
    public int computeSerializedSize() {
        computeFlags();

        int size = SIZE_CONSTRUCTOR_ID;
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

    public boolean getChangeInfo() {
        return changeInfo;
    }

    public void setChangeInfo(boolean changeInfo) {
        this.changeInfo = changeInfo;
    }

    public boolean getPostMessages() {
        return postMessages;
    }

    public void setPostMessages(boolean postMessages) {
        this.postMessages = postMessages;
    }

    public boolean getEditMessages() {
        return editMessages;
    }

    public void setEditMessages(boolean editMessages) {
        this.editMessages = editMessages;
    }

    public boolean getDeleteMessages() {
        return deleteMessages;
    }

    public void setDeleteMessages(boolean deleteMessages) {
        this.deleteMessages = deleteMessages;
    }

    public boolean getBanUsers() {
        return banUsers;
    }

    public void setBanUsers(boolean banUsers) {
        this.banUsers = banUsers;
    }

    public boolean getInviteUsers() {
        return inviteUsers;
    }

    public void setInviteUsers(boolean inviteUsers) {
        this.inviteUsers = inviteUsers;
    }

    public boolean getPinMessages() {
        return pinMessages;
    }

    public void setPinMessages(boolean pinMessages) {
        this.pinMessages = pinMessages;
    }

    public boolean getAddAdmins() {
        return addAdmins;
    }

    public void setAddAdmins(boolean addAdmins) {
        this.addAdmins = addAdmins;
    }

    public boolean getAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }
}
