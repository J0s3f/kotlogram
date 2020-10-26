package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;
import com.github.badoualy.telegram.tl.core.TLStringVector;
import java.lang.String;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLEmojiKeyword}: emojiKeyword#d5b3b9f9</li>
 * <li>{@link TLEmojiKeywordDeleted}: emojiKeywordDeleted#236df622</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsEmojiKeyword extends TLObject {
    protected String keyword;

    protected TLStringVector emoticons;

    public TLAbsEmojiKeyword() {
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public TLStringVector getEmoticons() {
        return emoticons;
    }

    public void setEmoticons(TLStringVector emoticons) {
        this.emoticons = emoticons;
    }
}
