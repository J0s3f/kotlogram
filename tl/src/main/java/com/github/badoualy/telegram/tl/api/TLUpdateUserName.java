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
public class TLUpdateUserName extends TLAbsUpdate {
    public static final int CONSTRUCTOR_ID = 0x0;

    protected int userId;

    protected String firstName;

    protected String lastName;

    protected String username;

    private final String _constructor = "updateUserName#0";

    public TLUpdateUserName() {
    }

    public TLUpdateUserName(int userId, String firstName, String lastName, String username) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeInt(userId, stream);
        writeString(firstName, stream);
        writeString(lastName, stream);
        writeString(username, stream);
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        userId = readInt(stream);
        firstName = readTLString(stream);
        lastName = readTLString(stream);
        username = readTLString(stream);
    }

    @Override
    public int computeSerializedSize() {
        int size = SIZE_CONSTRUCTOR_ID;
        size += SIZE_INT32;
        size += computeTLStringSerializedSize(firstName);
        size += computeTLStringSerializedSize(lastName);
        size += computeTLStringSerializedSize(username);
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

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
