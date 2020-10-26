package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;
import java.lang.String;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLSecureValueError}: secureValueError#869d758f</li>
 * <li>{@link TLSecureValueErrorData}: secureValueErrorData#e8a40bd9</li>
 * <li>{@link TLSecureValueErrorFile}: secureValueErrorFile#7a700873</li>
 * <li>{@link TLSecureValueErrorFiles}: secureValueErrorFiles#666220e9</li>
 * <li>{@link TLSecureValueErrorFrontSide}: secureValueErrorFrontSide#be3dfa</li>
 * <li>{@link TLSecureValueErrorReverseSide}: secureValueErrorReverseSide#868a2aa5</li>
 * <li>{@link TLSecureValueErrorSelfie}: secureValueErrorSelfie#e537ced6</li>
 * <li>{@link TLSecureValueErrorTranslationFile}: secureValueErrorTranslationFile#a1144770</li>
 * <li>{@link TLSecureValueErrorTranslationFiles}: secureValueErrorTranslationFiles#34636dd8</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsSecureValueError extends TLObject {
    protected TLAbsSecureValueType type;

    protected String text;

    public TLAbsSecureValueError() {
    }

    public TLAbsSecureValueType getType() {
        return type;
    }

    public void setType(TLAbsSecureValueType type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
