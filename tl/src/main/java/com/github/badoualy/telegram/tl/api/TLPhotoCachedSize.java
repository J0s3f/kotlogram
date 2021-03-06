package com.github.badoualy.telegram.tl.api;

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
public class TLPhotoCachedSize extends TLAbsPhotoSize {
    public static final int CONSTRUCTOR_ID = 0xe9a734fa;

    protected TLFileLocationToBeDeprecated location;

    protected int w;

    protected int h;

    protected TLBytes bytes;

    private final String _constructor = "photoCachedSize#e9a734fa";

    public TLPhotoCachedSize() {
    }

    public TLPhotoCachedSize(String type, TLFileLocationToBeDeprecated location, int w, int h, TLBytes bytes) {
        this.type = type;
        this.location = location;
        this.w = w;
        this.h = h;
        this.bytes = bytes;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeString(type, stream);
        writeTLObject(location, stream);
        writeInt(w, stream);
        writeInt(h, stream);
        writeTLBytes(bytes, stream);
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        type = readTLString(stream);
        location = readTLObject(stream, context, TLFileLocationToBeDeprecated.class, TLFileLocationToBeDeprecated.CONSTRUCTOR_ID);
        w = readInt(stream);
        h = readInt(stream);
        bytes = readTLBytes(stream, context);
    }

    @Override
    public int computeSerializedSize() {
        int size = SIZE_CONSTRUCTOR_ID;
        size += computeTLStringSerializedSize(type);
        size += location.computeSerializedSize();
        size += SIZE_INT32;
        size += SIZE_INT32;
        size += computeTLBytesSerializedSize(bytes);
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public TLFileLocationToBeDeprecated getLocation() {
        return location;
    }

    public void setLocation(TLFileLocationToBeDeprecated location) {
        this.location = location;
    }

    public int getW() {
        return w;
    }

    public void setW(int w) {
        this.w = w;
    }

    public int getH() {
        return h;
    }

    public void setH(int h) {
        this.h = h;
    }

    public TLBytes getBytes() {
        return bytes;
    }

    public void setBytes(TLBytes bytes) {
        this.bytes = bytes;
    }
}
