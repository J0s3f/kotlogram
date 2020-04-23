package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.TLContext;
import com.github.badoualy.telegram.tl.core.TLObject;
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
public class TLStickerSet extends TLObject {
    public static final int CONSTRUCTOR_ID = 0x0;

    protected int flags;

    protected boolean archived;

    protected boolean official;

    protected boolean masks;

    protected boolean animated;

    protected Integer installedDate;

    protected long id;

    protected long accessHash;

    protected String title;

    protected String shortName;

    protected TLAbsPhotoSize thumb;

    protected Integer thumbDcId;

    protected int count;

    protected int hash;

    private final String _constructor = "stickerSet#0";

    public TLStickerSet() {
    }

    public TLStickerSet(boolean archived, boolean official, boolean masks, boolean animated, Integer installedDate, long id, long accessHash, String title, String shortName, TLAbsPhotoSize thumb, Integer thumbDcId, int count, int hash) {
        this.archived = archived;
        this.official = official;
        this.masks = masks;
        this.animated = animated;
        this.installedDate = installedDate;
        this.id = id;
        this.accessHash = accessHash;
        this.title = title;
        this.shortName = shortName;
        this.thumb = thumb;
        this.thumbDcId = thumbDcId;
        this.count = count;
        this.hash = hash;
    }

    private void computeFlags() {
        flags = 0;
        flags = archived ? (flags | 2) : (flags & ~2);
        flags = official ? (flags | 4) : (flags & ~4);
        flags = masks ? (flags | 8) : (flags & ~8);
        flags = animated ? (flags | 32) : (flags & ~32);
        flags = installedDate != null ? (flags | 1) : (flags & ~1);
        flags = thumb != null ? (flags | 16) : (flags & ~16);
        flags = thumbDcId != null ? (flags | 16) : (flags & ~16);
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        computeFlags();

        writeInt(flags, stream);
        if ((flags & 1) != 0) {
            if (installedDate == null) throwNullFieldException("installedDate", flags);
            writeInt(installedDate, stream);
        }
        writeLong(id, stream);
        writeLong(accessHash, stream);
        writeString(title, stream);
        writeString(shortName, stream);
        if ((flags & 16) != 0) {
            if (thumb == null) throwNullFieldException("thumb", flags);
            writeTLObject(thumb, stream);
        }
        if ((flags & 16) != 0) {
            if (thumbDcId == null) throwNullFieldException("thumbDcId", flags);
            writeInt(thumbDcId, stream);
        }
        writeInt(count, stream);
        writeInt(hash, stream);
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        flags = readInt(stream);
        archived = (flags & 2) != 0;
        official = (flags & 4) != 0;
        masks = (flags & 8) != 0;
        animated = (flags & 32) != 0;
        installedDate = (flags & 1) != 0 ? readInt(stream) : null;
        id = readLong(stream);
        accessHash = readLong(stream);
        title = readTLString(stream);
        shortName = readTLString(stream);
        thumb = (flags & 16) != 0 ? readTLObject(stream, context, TLAbsPhotoSize.class, -1) : null;
        thumbDcId = (flags & 16) != 0 ? readInt(stream) : null;
        count = readInt(stream);
        hash = readInt(stream);
    }

    @Override
    public int computeSerializedSize() {
        computeFlags();

        int size = SIZE_CONSTRUCTOR_ID;
        size += SIZE_INT32;
        if ((flags & 1) != 0) {
            if (installedDate == null) throwNullFieldException("installedDate", flags);
            size += SIZE_INT32;
        }
        size += SIZE_INT64;
        size += SIZE_INT64;
        size += computeTLStringSerializedSize(title);
        size += computeTLStringSerializedSize(shortName);
        if ((flags & 16) != 0) {
            if (thumb == null) throwNullFieldException("thumb", flags);
            size += thumb.computeSerializedSize();
        }
        if ((flags & 16) != 0) {
            if (thumbDcId == null) throwNullFieldException("thumbDcId", flags);
            size += SIZE_INT32;
        }
        size += SIZE_INT32;
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

    public boolean getArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public boolean getOfficial() {
        return official;
    }

    public void setOfficial(boolean official) {
        this.official = official;
    }

    public boolean getMasks() {
        return masks;
    }

    public void setMasks(boolean masks) {
        this.masks = masks;
    }

    public boolean getAnimated() {
        return animated;
    }

    public void setAnimated(boolean animated) {
        this.animated = animated;
    }

    public Integer getInstalledDate() {
        return installedDate;
    }

    public void setInstalledDate(Integer installedDate) {
        this.installedDate = installedDate;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getAccessHash() {
        return accessHash;
    }

    public void setAccessHash(long accessHash) {
        this.accessHash = accessHash;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public TLAbsPhotoSize getThumb() {
        return thumb;
    }

    public void setThumb(TLAbsPhotoSize thumb) {
        this.thumb = thumb;
    }

    public Integer getThumbDcId() {
        return thumbDcId;
    }

    public void setThumbDcId(Integer thumbDcId) {
        this.thumbDcId = thumbDcId;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getHash() {
        return hash;
    }

    public void setHash(int hash) {
        this.hash = hash;
    }
}
