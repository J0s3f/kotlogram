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
public class TLWallPaperSettings extends TLObject {
    public static final int CONSTRUCTOR_ID = 0x5086cf8;

    protected int flags;

    protected boolean blur;

    protected boolean motion;

    protected Integer backgroundColor;

    protected Integer secondBackgroundColor;

    protected Integer intensity;

    protected Integer rotation;

    private final String _constructor = "wallPaperSettings#5086cf8";

    public TLWallPaperSettings() {
    }

    public TLWallPaperSettings(boolean blur, boolean motion, Integer backgroundColor, Integer secondBackgroundColor, Integer intensity, Integer rotation) {
        this.blur = blur;
        this.motion = motion;
        this.backgroundColor = backgroundColor;
        this.secondBackgroundColor = secondBackgroundColor;
        this.intensity = intensity;
        this.rotation = rotation;
    }

    private void computeFlags() {
        flags = 0;
        flags = blur ? (flags | 2) : (flags & ~2);
        flags = motion ? (flags | 4) : (flags & ~4);
        flags = backgroundColor != null ? (flags | 1) : (flags & ~1);
        flags = secondBackgroundColor != null ? (flags | 16) : (flags & ~16);
        flags = intensity != null ? (flags | 8) : (flags & ~8);
        flags = rotation != null ? (flags | 16) : (flags & ~16);
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        computeFlags();

        writeInt(flags, stream);
        if ((flags & 1) != 0) {
            if (backgroundColor == null) throwNullFieldException("backgroundColor", flags);
            writeInt(backgroundColor, stream);
        }
        if ((flags & 16) != 0) {
            if (secondBackgroundColor == null) throwNullFieldException("secondBackgroundColor", flags);
            writeInt(secondBackgroundColor, stream);
        }
        if ((flags & 8) != 0) {
            if (intensity == null) throwNullFieldException("intensity", flags);
            writeInt(intensity, stream);
        }
        if ((flags & 16) != 0) {
            if (rotation == null) throwNullFieldException("rotation", flags);
            writeInt(rotation, stream);
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        flags = readInt(stream);
        blur = (flags & 2) != 0;
        motion = (flags & 4) != 0;
        backgroundColor = (flags & 1) != 0 ? readInt(stream) : null;
        secondBackgroundColor = (flags & 16) != 0 ? readInt(stream) : null;
        intensity = (flags & 8) != 0 ? readInt(stream) : null;
        rotation = (flags & 16) != 0 ? readInt(stream) : null;
    }

    @Override
    public int computeSerializedSize() {
        computeFlags();

        int size = SIZE_CONSTRUCTOR_ID;
        size += SIZE_INT32;
        if ((flags & 1) != 0) {
            if (backgroundColor == null) throwNullFieldException("backgroundColor", flags);
            size += SIZE_INT32;
        }
        if ((flags & 16) != 0) {
            if (secondBackgroundColor == null) throwNullFieldException("secondBackgroundColor", flags);
            size += SIZE_INT32;
        }
        if ((flags & 8) != 0) {
            if (intensity == null) throwNullFieldException("intensity", flags);
            size += SIZE_INT32;
        }
        if ((flags & 16) != 0) {
            if (rotation == null) throwNullFieldException("rotation", flags);
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

    public boolean getBlur() {
        return blur;
    }

    public void setBlur(boolean blur) {
        this.blur = blur;
    }

    public boolean getMotion() {
        return motion;
    }

    public void setMotion(boolean motion) {
        this.motion = motion;
    }

    public Integer getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Integer backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public Integer getSecondBackgroundColor() {
        return secondBackgroundColor;
    }

    public void setSecondBackgroundColor(Integer secondBackgroundColor) {
        this.secondBackgroundColor = secondBackgroundColor;
    }

    public Integer getIntensity() {
        return intensity;
    }

    public void setIntensity(Integer intensity) {
        this.intensity = intensity;
    }

    public Integer getRotation() {
        return rotation;
    }

    public void setRotation(Integer rotation) {
        this.rotation = rotation;
    }
}
