package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLTopPeerCategoryBotsInline}: topPeerCategoryBotsInline#148677e2</li>
 * <li>{@link TLTopPeerCategoryBotsPM}: topPeerCategoryBotsPM#ab661b5b</li>
 * <li>{@link TLTopPeerCategoryChannels}: topPeerCategoryChannels#161d9628</li>
 * <li>{@link TLTopPeerCategoryCorrespondents}: topPeerCategoryCorrespondents#637b7ed</li>
 * <li>{@link TLTopPeerCategoryForwardChats}: topPeerCategoryForwardChats#fbeec0f0</li>
 * <li>{@link TLTopPeerCategoryForwardUsers}: topPeerCategoryForwardUsers#a8406ca9</li>
 * <li>{@link TLTopPeerCategoryGroups}: topPeerCategoryGroups#bd17a14a</li>
 * <li>{@link TLTopPeerCategoryPhoneCalls}: topPeerCategoryPhoneCalls#1e76a78c</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsTopPeerCategory extends TLObject {
    public TLAbsTopPeerCategory() {
    }
}
