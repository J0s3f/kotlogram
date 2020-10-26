package com.github.badoualy.telegram.tl.api.contacts;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLTopPeers}: contacts.topPeers#70b772a8</li>
 * <li>{@link TLTopPeersDisabled}: contacts.topPeersDisabled#b52c939d</li>
 * <li>{@link TLTopPeersNotModified}: contacts.topPeersNotModified#de266ef5</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsTopPeers extends TLObject {
    public TLAbsTopPeers() {
    }
}
