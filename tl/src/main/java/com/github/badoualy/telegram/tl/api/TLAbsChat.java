package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLChannel}: channel#d31a961e</li>
 * <li>{@link TLChannelForbidden}: channelForbidden#289da732</li>
 * <li>{@link TLChat}: chat#3bda1bde</li>
 * <li>{@link TLChatEmpty}: chatEmpty#9ba2d800</li>
 * <li>{@link TLChatForbidden}: chatForbidden#7328bdb</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsChat extends TLObject {
    protected int id;

    public TLAbsChat() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
