package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.TLContext;
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
public class TLInputMediaPhotoExternal extends TLAbsInputMedia {
    public static final int CONSTRUCTOR_ID = 0xe5bbfe1a;

    protected int flags;

    protected String url;

    protected Integer ttlSeconds;

    private final String _constructor = "inputMediaPhotoExternal#e5bbfe1a";

    public TLInputMediaPhotoExternal() {
    }

    public TLInputMediaPhotoExternal(String url, Integer ttlSeconds) {
        this.url = url;
        this.ttlSeconds = ttlSeconds;
    }

    private void computeFlags() {
        flags = 0;
        flags = ttlSeconds != null ? (flags | 1) : (flags & ~1);
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        computeFlags();

        writeInt(flags, stream);
        writeString(url, stream);
        if ((flags & 1) != 0) {
            if (ttlSeconds == null) throwNullFieldException("ttlSeconds", flags);
            writeInt(ttlSeconds, stream);
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        flags = readInt(stream);
        url = readTLString(stream);
        ttlSeconds = (flags & 1) != 0 ? readInt(stream) : null;
    }

    @Override
    public int computeSerializedSize() {
        computeFlags();

        int size = SIZE_CONSTRUCTOR_ID;
        size += SIZE_INT32;
        size += computeTLStringSerializedSize(url);
        if ((flags & 1) != 0) {
            if (ttlSeconds == null) throwNullFieldException("ttlSeconds", flags);
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(Integer ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }
}