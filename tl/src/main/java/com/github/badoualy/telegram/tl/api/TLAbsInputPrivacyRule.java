package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLInputPrivacyValueAllowAll}: inputPrivacyValueAllowAll#0</li>
 * <li>{@link TLInputPrivacyValueAllowChatParticipants}: inputPrivacyValueAllowChatParticipants#0</li>
 * <li>{@link TLInputPrivacyValueAllowContacts}: inputPrivacyValueAllowContacts#0</li>
 * <li>{@link TLInputPrivacyValueAllowUsers}: inputPrivacyValueAllowUsers#0</li>
 * <li>{@link TLInputPrivacyValueDisallowAll}: inputPrivacyValueDisallowAll#0</li>
 * <li>{@link TLInputPrivacyValueDisallowChatParticipants}: inputPrivacyValueDisallowChatParticipants#0</li>
 * <li>{@link TLInputPrivacyValueDisallowContacts}: inputPrivacyValueDisallowContacts#0</li>
 * <li>{@link TLInputPrivacyValueDisallowUsers}: inputPrivacyValueDisallowUsers#0</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsInputPrivacyRule extends TLObject {
    public TLAbsInputPrivacyRule() {
    }
}
