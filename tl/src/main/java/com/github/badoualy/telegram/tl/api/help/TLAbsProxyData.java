package com.github.badoualy.telegram.tl.api.help;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLProxyDataEmpty}: help.proxyDataEmpty#0</li>
 * <li>{@link TLProxyDataPromo}: help.proxyDataPromo#0</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsProxyData extends TLObject {
    protected int expires;

    public TLAbsProxyData() {
    }

    public int getExpires() {
        return expires;
    }

    public void setExpires(int expires) {
        this.expires = expires;
    }

    public abstract boolean isEmpty();

    public abstract boolean isNotEmpty();

    public TLProxyDataPromo getAsProxyDataPromo() {
        return null;
    }
}
