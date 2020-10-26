package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLInputUser}: inputUser#d8292816</li>
 * <li>{@link TLInputUserEmpty}: inputUserEmpty#b98886cf</li>
 * <li>{@link TLInputUserFromMessage}: inputUserFromMessage#2d117597</li>
 * <li>{@link TLInputUserSelf}: inputUserSelf#f7c1b13f</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsInputUser extends TLObject {
    public TLAbsInputUser() {
    }
}
