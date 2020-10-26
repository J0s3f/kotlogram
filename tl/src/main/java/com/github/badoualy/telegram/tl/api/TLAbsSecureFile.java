package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLSecureFile}: secureFile#e0277a62</li>
 * <li>{@link TLSecureFileEmpty}: secureFileEmpty#64199744</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsSecureFile extends TLObject {
    public TLAbsSecureFile() {
    }

    public abstract boolean isEmpty();

    public abstract boolean isNotEmpty();

    public TLSecureFile getAsSecureFile() {
        return null;
    }
}
