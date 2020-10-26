package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;
import java.lang.String;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLPhotoCachedSize}: photoCachedSize#e9a734fa</li>
 * <li>{@link TLPhotoSize}: photoSize#77bfb61b</li>
 * <li>{@link TLPhotoSizeEmpty}: photoSizeEmpty#e17e23c</li>
 * <li>{@link TLPhotoStrippedSize}: photoStrippedSize#e0b0bc2e</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsPhotoSize extends TLObject {
    protected String type;

    public TLAbsPhotoSize() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
