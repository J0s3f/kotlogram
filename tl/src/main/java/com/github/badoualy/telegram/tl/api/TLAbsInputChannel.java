package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLInputChannel}: inputChannel#afeb712e</li>
 * <li>{@link TLInputChannelEmpty}: inputChannelEmpty#ee8c1e86</li>
 * <li>{@link TLInputChannelFromMessage}: inputChannelFromMessage#2a286531</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsInputChannel extends TLObject {
    public TLAbsInputChannel() {
    }
}
