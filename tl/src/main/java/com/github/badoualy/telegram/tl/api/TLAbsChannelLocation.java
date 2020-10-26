package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLChannelLocation}: channelLocation#209b82db</li>
 * <li>{@link TLChannelLocationEmpty}: channelLocationEmpty#bfb5ad8b</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsChannelLocation extends TLObject {
    public TLAbsChannelLocation() {
    }

    public abstract boolean isEmpty();

    public abstract boolean isNotEmpty();

    public TLChannelLocation getAsChannelLocation() {
        return null;
    }
}
