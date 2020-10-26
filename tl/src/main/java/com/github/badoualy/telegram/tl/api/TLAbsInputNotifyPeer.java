package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLInputNotifyBroadcasts}: inputNotifyBroadcasts#b1db7c7e</li>
 * <li>{@link TLInputNotifyChats}: inputNotifyChats#4a95e84e</li>
 * <li>{@link TLInputNotifyPeer}: inputNotifyPeer#b8bc5b0c</li>
 * <li>{@link TLInputNotifyUsers}: inputNotifyUsers#193b4417</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsInputNotifyPeer extends TLObject {
    public TLAbsInputNotifyPeer() {
    }
}
