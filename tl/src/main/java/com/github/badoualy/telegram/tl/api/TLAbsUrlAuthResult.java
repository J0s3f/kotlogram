package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLUrlAuthResultAccepted}: urlAuthResultAccepted#8f8c0e4e</li>
 * <li>{@link TLUrlAuthResultDefault}: urlAuthResultDefault#a9d6db1f</li>
 * <li>{@link TLUrlAuthResultRequest}: urlAuthResultRequest#92d33a0e</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsUrlAuthResult extends TLObject {
    public TLAbsUrlAuthResult() {
    }
}
