package com.github.badoualy.telegram.tl.api.contacts;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.TLContext;
import com.github.badoualy.telegram.tl.api.TLAbsUser;
import com.github.badoualy.telegram.tl.api.TLContactBlocked;
import com.github.badoualy.telegram.tl.core.TLVector;
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
public class TLBlockedSlice extends TLAbsBlocked {
    public static final int CONSTRUCTOR_ID = 0x0;

    protected int count;

    private final String _constructor = "contacts.blockedSlice#0";

    public TLBlockedSlice() {
    }

    public TLBlockedSlice(int count, TLVector<TLContactBlocked> blocked, TLVector<TLAbsUser> users) {
        this.count = count;
        this.blocked = blocked;
        this.users = users;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeInt(count, stream);
        writeTLVector(blocked, stream);
        writeTLVector(users, stream);
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        count = readInt(stream);
        blocked = readTLVector(stream, context);
        users = readTLVector(stream, context);
    }

    @Override
    public int computeSerializedSize() {
        int size = SIZE_CONSTRUCTOR_ID;
        size += SIZE_INT32;
        size += blocked.computeSerializedSize();
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

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public TLVector<TLContactBlocked> getBlocked() {
        return blocked;
    }

    public void setBlocked(TLVector<TLContactBlocked> blocked) {
        this.blocked = blocked;
    }

    public TLVector<TLAbsUser> getUsers() {
        return users;
    }

    public void setUsers(TLVector<TLAbsUser> users) {
        this.users = users;
    }
}
