package com.github.badoualy.telegram.tl.api.request;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.TLContext;
import com.github.badoualy.telegram.tl.api.TLAbsInputStickerSet;
import com.github.badoualy.telegram.tl.api.messages.TLStickerSet;
import com.github.badoualy.telegram.tl.core.TLMethod;
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
public class TLRequestMessagesGetStickerSet extends TLMethod<TLStickerSet> {
    public static final int CONSTRUCTOR_ID = 0x0;

    protected TLAbsInputStickerSet stickerset;

    private final String _constructor = "messages.getStickerSet#0";

    public TLRequestMessagesGetStickerSet() {
    }

    public TLRequestMessagesGetStickerSet(TLAbsInputStickerSet stickerset) {
        this.stickerset = stickerset;
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public TLStickerSet deserializeResponse(InputStream stream, TLContext context) throws IOException {
        final TLObject response = readTLObject(stream, context);
        if (response == null) {
            throw new IOException("Unable to parse response");
        }
        if (!(response instanceof TLStickerSet)) {
            throw new IOException("Incorrect response type, expected " + getClass().getCanonicalName() + ", found " + response.getClass().getCanonicalName());
        }
        return (TLStickerSet) response;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeTLObject(stickerset, stream);
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        stickerset = readTLObject(stream, context, TLAbsInputStickerSet.class, -1);
    }

    @Override
    public int computeSerializedSize() {
        int size = SIZE_CONSTRUCTOR_ID;
        size += stickerset.computeSerializedSize();
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

    public TLAbsInputStickerSet getStickerset() {
        return stickerset;
    }

    public void setStickerset(TLAbsInputStickerSet stickerset) {
        this.stickerset = stickerset;
    }
}
