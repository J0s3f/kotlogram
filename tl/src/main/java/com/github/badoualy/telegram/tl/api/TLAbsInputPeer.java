package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLInputPeerChannel}: inputPeerChannel#20adaef8</li>
 * <li>{@link TLInputPeerChannelFromMessage}: inputPeerChannelFromMessage#9c95f7bb</li>
 * <li>{@link TLInputPeerChat}: inputPeerChat#179be863</li>
 * <li>{@link TLInputPeerEmpty}: inputPeerEmpty#7f3b18ea</li>
 * <li>{@link TLInputPeerSelf}: inputPeerSelf#7da07ec9</li>
 * <li>{@link TLInputPeerUser}: inputPeerUser#7b8e7de6</li>
 * <li>{@link TLInputPeerUserFromMessage}: inputPeerUserFromMessage#17bae2e6</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsInputPeer extends TLObject {
    public TLAbsInputPeer() {
    }
}
