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
public class TLThemeSettings extends TLObject {
    public static final int CONSTRUCTOR_ID = 0x9c14984a;

    protected int flags;

    protected TLAbsBaseTheme baseTheme;

    protected int accentColor;

    protected Integer messageTopColor;

    protected Integer messageBottomColor;

    protected TLAbsWallPaper wallpaper;

    private final String _constructor = "themeSettings#9c14984a";

    public TLThemeSettings() {
    }

    public TLThemeSettings(TLAbsBaseTheme baseTheme, int accentColor, Integer messageTopColor, Integer messageBottomColor, TLAbsWallPaper wallpaper) {
        this.baseTheme = baseTheme;
        this.accentColor = accentColor;
        this.messageTopColor = messageTopColor;
        this.messageBottomColor = messageBottomColor;
        this.wallpaper = wallpaper;
    }

    private void computeFlags() {
        flags = 0;
        flags = messageTopColor != null ? (flags | 1) : (flags & ~1);
        flags = messageBottomColor != null ? (flags | 1) : (flags & ~1);
        flags = wallpaper != null ? (flags | 2) : (flags & ~2);
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        computeFlags();

        writeInt(flags, stream);
        writeTLObject(baseTheme, stream);
        writeInt(accentColor, stream);
        if ((flags & 1) != 0) {
            if (messageTopColor == null) throwNullFieldException("messageTopColor", flags);
            writeInt(messageTopColor, stream);
        }
        if ((flags & 1) != 0) {
            if (messageBottomColor == null) throwNullFieldException("messageBottomColor", flags);
            writeInt(messageBottomColor, stream);
        }
        if ((flags & 2) != 0) {
            if (wallpaper == null) throwNullFieldException("wallpaper", flags);
            writeTLObject(wallpaper, stream);
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        flags = readInt(stream);
        baseTheme = readTLObject(stream, context, TLAbsBaseTheme.class, -1);
        accentColor = readInt(stream);
        messageTopColor = (flags & 1) != 0 ? readInt(stream) : null;
        messageBottomColor = (flags & 1) != 0 ? readInt(stream) : null;
        wallpaper = (flags & 2) != 0 ? readTLObject(stream, context, TLAbsWallPaper.class, -1) : null;
    }

    @Override
    public int computeSerializedSize() {
        computeFlags();

        int size = SIZE_CONSTRUCTOR_ID;
        size += SIZE_INT32;
        size += baseTheme.computeSerializedSize();
        size += SIZE_INT32;
        if ((flags & 1) != 0) {
            if (messageTopColor == null) throwNullFieldException("messageTopColor", flags);
            size += SIZE_INT32;
        }
        if ((flags & 1) != 0) {
            if (messageBottomColor == null) throwNullFieldException("messageBottomColor", flags);
            size += SIZE_INT32;
        }
        if ((flags & 2) != 0) {
            if (wallpaper == null) throwNullFieldException("wallpaper", flags);
            size += wallpaper.computeSerializedSize();
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

    public TLAbsBaseTheme getBaseTheme() {
        return baseTheme;
    }

    public void setBaseTheme(TLAbsBaseTheme baseTheme) {
        this.baseTheme = baseTheme;
    }

    public int getAccentColor() {
        return accentColor;
    }

    public void setAccentColor(int accentColor) {
        this.accentColor = accentColor;
    }

    public Integer getMessageTopColor() {
        return messageTopColor;
    }

    public void setMessageTopColor(Integer messageTopColor) {
        this.messageTopColor = messageTopColor;
    }

    public Integer getMessageBottomColor() {
        return messageBottomColor;
    }

    public void setMessageBottomColor(Integer messageBottomColor) {
        this.messageBottomColor = messageBottomColor;
    }

    public TLAbsWallPaper getWallpaper() {
        return wallpaper;
    }

    public void setWallpaper(TLAbsWallPaper wallpaper) {
        this.wallpaper = wallpaper;
    }
}
