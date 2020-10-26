package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLInputPrivacyKeyAddedByPhone}: inputPrivacyKeyAddedByPhone#d1219bdd</li>
 * <li>{@link TLInputPrivacyKeyChatInvite}: inputPrivacyKeyChatInvite#bdfb0426</li>
 * <li>{@link TLInputPrivacyKeyForwards}: inputPrivacyKeyForwards#a4dd4c08</li>
 * <li>{@link TLInputPrivacyKeyPhoneCall}: inputPrivacyKeyPhoneCall#fabadc5f</li>
 * <li>{@link TLInputPrivacyKeyPhoneNumber}: inputPrivacyKeyPhoneNumber#352dafa</li>
 * <li>{@link TLInputPrivacyKeyPhoneP2P}: inputPrivacyKeyPhoneP2P#db9e70d2</li>
 * <li>{@link TLInputPrivacyKeyProfilePhoto}: inputPrivacyKeyProfilePhoto#5719bacc</li>
 * <li>{@link TLInputPrivacyKeyStatusTimestamp}: inputPrivacyKeyStatusTimestamp#4f96cb18</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsInputPrivacyKey extends TLObject {
    public TLAbsInputPrivacyKey() {
    }
}
