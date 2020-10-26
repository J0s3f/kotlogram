package com.github.badoualy.telegram.tl.api.messages;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.TLContext;
import com.github.badoualy.telegram.tl.api.TLAbsStickerSetCovered;
import com.github.badoualy.telegram.tl.core.TLLongVector;
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
public class TLFeaturedStickers extends TLAbsFeaturedStickers {
    public static final int CONSTRUCTOR_ID = 0xb6abc341;

    protected int hash;

    protected TLVector<TLAbsStickerSetCovered> sets;

    protected TLLongVector unread;

    private final String _constructor = "messages.featuredStickers#b6abc341";

    public TLFeaturedStickers() {
    }

    public TLFeaturedStickers(int hash, int count, TLVector<TLAbsStickerSetCovered> sets, TLLongVector unread) {
        this.hash = hash;
        this.count = count;
        this.sets = sets;
        this.unread = unread;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeInt(hash, stream);
        writeInt(count, stream);
        writeTLVector(sets, stream);
        writeTLVector(unread, stream);
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        hash = readInt(stream);
        count = readInt(stream);
        sets = readTLVector(stream, context);
        unread = readTLLongVector(stream, context);
    }

    @Override
    public int computeSerializedSize() {
        int size = SIZE_CONSTRUCTOR_ID;
        size += SIZE_INT32;
        size += SIZE_INT32;
        size += sets.computeSerializedSize();
        size += unread.computeSerializedSize();
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

    public int getHash() {
        return hash;
    }

    public void setHash(int hash) {
        this.hash = hash;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public TLVector<TLAbsStickerSetCovered> getSets() {
        return sets;
    }

    public void setSets(TLVector<TLAbsStickerSetCovered> sets) {
        this.sets = sets;
    }

    public TLLongVector getUnread() {
        return unread;
    }

    public void setUnread(TLLongVector unread) {
        this.unread = unread;
    }
}
