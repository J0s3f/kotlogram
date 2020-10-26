package com.github.badoualy.telegram.tl.api.updates;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;
import java.lang.Integer;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLChannelDifference}: updates.channelDifference#2064674e</li>
 * <li>{@link TLChannelDifferenceEmpty}: updates.channelDifferenceEmpty#3e11affb</li>
 * <li>{@link TLChannelDifferenceTooLong}: updates.channelDifferenceTooLong#a4bcc6fe</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsChannelDifference extends TLObject {
    protected int flags;

    protected boolean _final;

    protected Integer timeout;

    public TLAbsChannelDifference() {
    }

    public boolean getFinal() {
        return _final;
    }

    public void setFinal(boolean _final) {
        this._final = _final;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }
}
