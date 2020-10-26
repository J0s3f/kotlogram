package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLChannelParticipant}: channelParticipant#15ebac1d</li>
 * <li>{@link TLChannelParticipantAdmin}: channelParticipantAdmin#ccbebbaf</li>
 * <li>{@link TLChannelParticipantBanned}: channelParticipantBanned#1c0facaf</li>
 * <li>{@link TLChannelParticipantCreator}: channelParticipantCreator#808d15a4</li>
 * <li>{@link TLChannelParticipantSelf}: channelParticipantSelf#a3289a6d</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsChannelParticipant extends TLObject {
    protected int userId;

    public TLAbsChannelParticipant() {
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}
