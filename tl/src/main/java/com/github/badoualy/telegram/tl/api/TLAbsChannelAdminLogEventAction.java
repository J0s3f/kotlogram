package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLChannelAdminLogEventActionChangeAbout}: channelAdminLogEventActionChangeAbout#0</li>
 * <li>{@link TLChannelAdminLogEventActionChangeLinkedChat}: channelAdminLogEventActionChangeLinkedChat#0</li>
 * <li>{@link TLChannelAdminLogEventActionChangeLocation}: channelAdminLogEventActionChangeLocation#0</li>
 * <li>{@link TLChannelAdminLogEventActionChangePhoto}: channelAdminLogEventActionChangePhoto#0</li>
 * <li>{@link TLChannelAdminLogEventActionChangeStickerSet}: channelAdminLogEventActionChangeStickerSet#0</li>
 * <li>{@link TLChannelAdminLogEventActionChangeTitle}: channelAdminLogEventActionChangeTitle#0</li>
 * <li>{@link TLChannelAdminLogEventActionChangeUsername}: channelAdminLogEventActionChangeUsername#0</li>
 * <li>{@link TLChannelAdminLogEventActionDefaultBannedRights}: channelAdminLogEventActionDefaultBannedRights#0</li>
 * <li>{@link TLChannelAdminLogEventActionDeleteMessage}: channelAdminLogEventActionDeleteMessage#0</li>
 * <li>{@link TLChannelAdminLogEventActionEditMessage}: channelAdminLogEventActionEditMessage#0</li>
 * <li>{@link TLChannelAdminLogEventActionParticipantInvite}: channelAdminLogEventActionParticipantInvite#0</li>
 * <li>{@link TLChannelAdminLogEventActionParticipantJoin}: channelAdminLogEventActionParticipantJoin#0</li>
 * <li>{@link TLChannelAdminLogEventActionParticipantLeave}: channelAdminLogEventActionParticipantLeave#0</li>
 * <li>{@link TLChannelAdminLogEventActionParticipantToggleAdmin}: channelAdminLogEventActionParticipantToggleAdmin#0</li>
 * <li>{@link TLChannelAdminLogEventActionParticipantToggleBan}: channelAdminLogEventActionParticipantToggleBan#0</li>
 * <li>{@link TLChannelAdminLogEventActionStopPoll}: channelAdminLogEventActionStopPoll#0</li>
 * <li>{@link TLChannelAdminLogEventActionToggleInvites}: channelAdminLogEventActionToggleInvites#0</li>
 * <li>{@link TLChannelAdminLogEventActionTogglePreHistoryHidden}: channelAdminLogEventActionTogglePreHistoryHidden#0</li>
 * <li>{@link TLChannelAdminLogEventActionToggleSignatures}: channelAdminLogEventActionToggleSignatures#0</li>
 * <li>{@link TLChannelAdminLogEventActionToggleSlowMode}: channelAdminLogEventActionToggleSlowMode#0</li>
 * <li>{@link TLChannelAdminLogEventActionUpdatePinned}: channelAdminLogEventActionUpdatePinned#0</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsChannelAdminLogEventAction extends TLObject {
    public TLAbsChannelAdminLogEventAction() {
    }
}
