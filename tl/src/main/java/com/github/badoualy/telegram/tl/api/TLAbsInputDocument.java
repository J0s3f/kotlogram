package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLInputDocument}: inputDocument#1abfb575</li>
 * <li>{@link TLInputDocumentEmpty}: inputDocumentEmpty#72f0eaae</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsInputDocument extends TLObject {
    public TLAbsInputDocument() {
    }

    public abstract boolean isEmpty();

    public abstract boolean isNotEmpty();

    public TLInputDocument getAsInputDocument() {
        return null;
    }

    public static TLInputDocumentEmpty newEmpty() {
        return new TLInputDocumentEmpty();
    }

    public static TLInputDocument newNotEmpty() {
        return new TLInputDocument();
    }
}
