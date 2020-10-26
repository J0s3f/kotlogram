package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;
import java.lang.String;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLRecentMeUrlChat}: recentMeUrlChat#a01b22f9</li>
 * <li>{@link TLRecentMeUrlChatInvite}: recentMeUrlChatInvite#eb49081d</li>
 * <li>{@link TLRecentMeUrlStickerSet}: recentMeUrlStickerSet#bc0a57dc</li>
 * <li>{@link TLRecentMeUrlUnknown}: recentMeUrlUnknown#46e1d13d</li>
 * <li>{@link TLRecentMeUrlUser}: recentMeUrlUser#8dbc3336</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsRecentMeUrl extends TLObject {
    protected String url;

    public TLAbsRecentMeUrl() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
