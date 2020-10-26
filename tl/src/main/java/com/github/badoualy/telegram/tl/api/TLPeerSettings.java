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
public class TLPeerSettings extends TLObject {
    public static final int CONSTRUCTOR_ID = 0x818426cd;

    protected int flags;

    protected boolean reportSpam;

    protected boolean addContact;

    protected boolean blockContact;

    protected boolean shareContact;

    protected boolean needContactsException;

    protected boolean reportGeo;

    private final String _constructor = "peerSettings#818426cd";

    public TLPeerSettings() {
    }

    public TLPeerSettings(boolean reportSpam, boolean addContact, boolean blockContact, boolean shareContact, boolean needContactsException, boolean reportGeo) {
        this.reportSpam = reportSpam;
        this.addContact = addContact;
        this.blockContact = blockContact;
        this.shareContact = shareContact;
        this.needContactsException = needContactsException;
        this.reportGeo = reportGeo;
    }

    private void computeFlags() {
        flags = 0;
        flags = reportSpam ? (flags | 1) : (flags & ~1);
        flags = addContact ? (flags | 2) : (flags & ~2);
        flags = blockContact ? (flags | 4) : (flags & ~4);
        flags = shareContact ? (flags | 8) : (flags & ~8);
        flags = needContactsException ? (flags | 16) : (flags & ~16);
        flags = reportGeo ? (flags | 32) : (flags & ~32);
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
        reportSpam = (flags & 1) != 0;
        addContact = (flags & 2) != 0;
        blockContact = (flags & 4) != 0;
        shareContact = (flags & 8) != 0;
        needContactsException = (flags & 16) != 0;
        reportGeo = (flags & 32) != 0;
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

    public boolean getReportSpam() {
        return reportSpam;
    }

    public void setReportSpam(boolean reportSpam) {
        this.reportSpam = reportSpam;
    }

    public boolean getAddContact() {
        return addContact;
    }

    public void setAddContact(boolean addContact) {
        this.addContact = addContact;
    }

    public boolean getBlockContact() {
        return blockContact;
    }

    public void setBlockContact(boolean blockContact) {
        this.blockContact = blockContact;
    }

    public boolean getShareContact() {
        return shareContact;
    }

    public void setShareContact(boolean shareContact) {
        this.shareContact = shareContact;
    }

    public boolean getNeedContactsException() {
        return needContactsException;
    }

    public void setNeedContactsException(boolean needContactsException) {
        this.needContactsException = needContactsException;
    }

    public boolean getReportGeo() {
        return reportGeo;
    }

    public void setReportGeo(boolean reportGeo) {
        this.reportGeo = reportGeo;
    }
}
