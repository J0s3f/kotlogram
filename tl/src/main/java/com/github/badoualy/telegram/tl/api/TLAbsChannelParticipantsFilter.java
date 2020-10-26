package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLChannelParticipantsAdmins}: channelParticipantsAdmins#b4608969</li>
 * <li>{@link TLChannelParticipantsBanned}: channelParticipantsBanned#1427a5e1</li>
 * <li>{@link TLChannelParticipantsBots}: channelParticipantsBots#b0d1865b</li>
 * <li>{@link TLChannelParticipantsContacts}: channelParticipantsContacts#bb6ae88d</li>
 * <li>{@link TLChannelParticipantsKicked}: channelParticipantsKicked#a3b54985</li>
 * <li>{@link TLChannelParticipantsRecent}: channelParticipantsRecent#de3f3c79</li>
 * <li>{@link TLChannelParticipantsSearch}: channelParticipantsSearch#656ac4b</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsChannelParticipantsFilter extends TLObject {
    public TLAbsChannelParticipantsFilter() {
    }
}
