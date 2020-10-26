package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLPrivacyKeyAddedByPhone}: privacyKeyAddedByPhone#42ffd42b</li>
 * <li>{@link TLPrivacyKeyChatInvite}: privacyKeyChatInvite#500e6dfa</li>
 * <li>{@link TLPrivacyKeyForwards}: privacyKeyForwards#69ec56a3</li>
 * <li>{@link TLPrivacyKeyPhoneCall}: privacyKeyPhoneCall#3d662b7b</li>
 * <li>{@link TLPrivacyKeyPhoneNumber}: privacyKeyPhoneNumber#d19ae46d</li>
 * <li>{@link TLPrivacyKeyPhoneP2P}: privacyKeyPhoneP2P#39491cc8</li>
 * <li>{@link TLPrivacyKeyProfilePhoto}: privacyKeyProfilePhoto#96151fed</li>
 * <li>{@link TLPrivacyKeyStatusTimestamp}: privacyKeyStatusTimestamp#bc2eab30</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsPrivacyKey extends TLObject {
    public TLAbsPrivacyKey() {
    }
}
