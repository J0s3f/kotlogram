package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLBotInlineMessageMediaAuto}: botInlineMessageMediaAuto#764cf810</li>
 * <li>{@link TLBotInlineMessageMediaContact}: botInlineMessageMediaContact#18d1cdc2</li>
 * <li>{@link TLBotInlineMessageMediaGeo}: botInlineMessageMediaGeo#b722de65</li>
 * <li>{@link TLBotInlineMessageMediaVenue}: botInlineMessageMediaVenue#8a86659c</li>
 * <li>{@link TLBotInlineMessageText}: botInlineMessageText#8c7f65e2</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsBotInlineMessage extends TLObject {
    protected int flags;

    protected TLAbsReplyMarkup replyMarkup;

    public TLAbsBotInlineMessage() {
    }

    public TLAbsReplyMarkup getReplyMarkup() {
        return replyMarkup;
    }

    public void setReplyMarkup(TLAbsReplyMarkup replyMarkup) {
        this.replyMarkup = replyMarkup;
    }
}
