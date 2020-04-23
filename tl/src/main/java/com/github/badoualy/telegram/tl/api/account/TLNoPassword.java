package com.github.badoualy.telegram.tl.api.account;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.TLContext;
import com.github.badoualy.telegram.tl.core.TLBytes;
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
public class TLNoPassword extends TLAbsPassword {
    public static final int CONSTRUCTOR_ID = 0x96dabc18;

    private final String _constructor = "account.noPassword#96dabc18";

    public TLNoPassword() {
    }

    public TLNoPassword(TLBytes newSalt, String emailUnconfirmedPattern) {
        this.newSalt = newSalt;
        this.emailUnconfirmedPattern = emailUnconfirmedPattern;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeTLBytes(newSalt, stream);
        writeString(emailUnconfirmedPattern, stream);
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        newSalt = readTLBytes(stream, context);
        emailUnconfirmedPattern = readTLString(stream);
    }

    @Override
    public int computeSerializedSize() {
        int size = SIZE_CONSTRUCTOR_ID;
        size += computeTLBytesSerializedSize(newSalt);
        size += computeTLStringSerializedSize(emailUnconfirmedPattern);
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

    public TLBytes getNewSalt() {
        return newSalt;
    }

    public void setNewSalt(TLBytes newSalt) {
        this.newSalt = newSalt;
    }

    public String getEmailUnconfirmedPattern() {
        return emailUnconfirmedPattern;
    }

    public void setEmailUnconfirmedPattern(String emailUnconfirmedPattern) {
        this.emailUnconfirmedPattern = emailUnconfirmedPattern;
    }
}
