package com.github.badoualy.telegram.tl.api.auth;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLLoginToken}: auth.loginToken#629f1980</li>
 * <li>{@link TLLoginTokenMigrateTo}: auth.loginTokenMigrateTo#68e9916</li>
 * <li>{@link TLLoginTokenSuccess}: auth.loginTokenSuccess#390d5c5e</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsLoginToken extends TLObject {
    public TLAbsLoginToken() {
    }
}
