package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.TLContext;
import com.github.badoualy.telegram.tl.core.TLIntVector;
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
public class TLPrivacyValueDisallowUsers extends TLAbsPrivacyRule {
    public static final int CONSTRUCTOR_ID = 0x0;

    protected TLIntVector users;

    private final String _constructor = "privacyValueDisallowUsers#0";

    public TLPrivacyValueDisallowUsers() {
    }

    public TLPrivacyValueDisallowUsers(TLIntVector users) {
        this.users = users;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeTLVector(users, stream);
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        users = readTLIntVector(stream, context);
    }

    @Override
    public int computeSerializedSize() {
        int size = SIZE_CONSTRUCTOR_ID;
        size += users.computeSerializedSize();
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

    public TLIntVector getUsers() {
        return users;
    }

    public void setUsers(TLIntVector users) {
        this.users = users;
    }
}
