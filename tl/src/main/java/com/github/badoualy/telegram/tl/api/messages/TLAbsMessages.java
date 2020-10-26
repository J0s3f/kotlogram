package com.github.badoualy.telegram.tl.api.messages;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLChannelMessages}: messages.channelMessages#99262e37</li>
 * <li>{@link TLMessages}: messages.messages#8c718e87</li>
 * <li>{@link TLMessagesNotModified}: messages.messagesNotModified#74535f21</li>
 * <li>{@link TLMessagesSlice}: messages.messagesSlice#c8edce1e</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsMessages extends TLObject {
    public TLAbsMessages() {
    }
}
