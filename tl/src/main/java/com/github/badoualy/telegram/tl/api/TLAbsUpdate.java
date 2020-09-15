package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLUpdateBotCallbackQuery}: updateBotCallbackQuery#0</li>
 * <li>{@link TLUpdateBotInlineQuery}: updateBotInlineQuery#0</li>
 * <li>{@link TLUpdateBotInlineSend}: updateBotInlineSend#0</li>
 * <li>{@link TLUpdateBotPrecheckoutQuery}: updateBotPrecheckoutQuery#0</li>
 * <li>{@link TLUpdateBotShippingQuery}: updateBotShippingQuery#0</li>
 * <li>{@link TLUpdateBotWebhookJSON}: updateBotWebhookJSON#0</li>
 * <li>{@link TLUpdateBotWebhookJSONQuery}: updateBotWebhookJSONQuery#0</li>
 * <li>{@link TLUpdateChannel}: updateChannel#0</li>
 * <li>{@link TLUpdateChannelAvailableMessages}: updateChannelAvailableMessages#0</li>
 * <li>{@link TLUpdateChannelMessageViews}: updateChannelMessageViews#0</li>
 * <li>{@link TLUpdateChannelPinnedMessage}: updateChannelPinnedMessage#0</li>
 * <li>{@link TLUpdateChannelReadMessagesContents}: updateChannelReadMessagesContents#0</li>
 * <li>{@link TLUpdateChannelTooLong}: updateChannelTooLong#0</li>
 * <li>{@link TLUpdateChannelWebPage}: updateChannelWebPage#0</li>
 * <li>{@link TLUpdateChatDefaultBannedRights}: updateChatDefaultBannedRights#0</li>
 * <li>{@link TLUpdateChatParticipantAdd}: updateChatParticipantAdd#0</li>
 * <li>{@link TLUpdateChatParticipantAdmin}: updateChatParticipantAdmin#0</li>
 * <li>{@link TLUpdateChatParticipantDelete}: updateChatParticipantDelete#0</li>
 * <li>{@link TLUpdateChatParticipants}: updateChatParticipants#0</li>
 * <li>{@link TLUpdateChatPinnedMessage}: updateChatPinnedMessage#0</li>
 * <li>{@link TLUpdateChatUserTyping}: updateChatUserTyping#0</li>
 * <li>{@link TLUpdateConfig}: updateConfig#0</li>
 * <li>{@link TLUpdateContactsReset}: updateContactsReset#0</li>
 * <li>{@link TLUpdateDcOptions}: updateDcOptions#0</li>
 * <li>{@link TLUpdateDeleteChannelMessages}: updateDeleteChannelMessages#0</li>
 * <li>{@link TLUpdateDeleteMessages}: updateDeleteMessages#0</li>
 * <li>{@link TLUpdateDeleteScheduledMessages}: updateDeleteScheduledMessages#0</li>
 * <li>{@link TLUpdateDialogFilter}: updateDialogFilter#0</li>
 * <li>{@link TLUpdateDialogFilterOrder}: updateDialogFilterOrder#0</li>
 * <li>{@link TLUpdateDialogFilters}: updateDialogFilters#0</li>
 * <li>{@link TLUpdateDialogPinned}: updateDialogPinned#0</li>
 * <li>{@link TLUpdateDialogUnreadMark}: updateDialogUnreadMark#0</li>
 * <li>{@link TLUpdateDraftMessage}: updateDraftMessage#0</li>
 * <li>{@link TLUpdateEditChannelMessage}: updateEditChannelMessage#0</li>
 * <li>{@link TLUpdateEditMessage}: updateEditMessage#0</li>
 * <li>{@link TLUpdateEncryptedChatTyping}: updateEncryptedChatTyping#0</li>
 * <li>{@link TLUpdateEncryptedMessagesRead}: updateEncryptedMessagesRead#0</li>
 * <li>{@link TLUpdateEncryption}: updateEncryption#0</li>
 * <li>{@link TLUpdateFavedStickers}: updateFavedStickers#0</li>
 * <li>{@link TLUpdateFolderPeers}: updateFolderPeers#0</li>
 * <li>{@link TLUpdateGeoLiveViewed}: updateGeoLiveViewed#0</li>
 * <li>{@link TLUpdateInlineBotCallbackQuery}: updateInlineBotCallbackQuery#0</li>
 * <li>{@link TLUpdateLangPack}: updateLangPack#0</li>
 * <li>{@link TLUpdateLangPackTooLong}: updateLangPackTooLong#0</li>
 * <li>{@link TLUpdateLoginToken}: updateLoginToken#0</li>
 * <li>{@link TLUpdateMessageID}: updateMessageID#0</li>
 * <li>{@link TLUpdateMessagePoll}: updateMessagePoll#0</li>
 * <li>{@link TLUpdateMessagePollVote}: updateMessagePollVote#0</li>
 * <li>{@link TLUpdateNewChannelMessage}: updateNewChannelMessage#0</li>
 * <li>{@link TLUpdateNewEncryptedMessage}: updateNewEncryptedMessage#0</li>
 * <li>{@link TLUpdateNewMessage}: updateNewMessage#0</li>
 * <li>{@link TLUpdateNewScheduledMessage}: updateNewScheduledMessage#0</li>
 * <li>{@link TLUpdateNewStickerSet}: updateNewStickerSet#0</li>
 * <li>{@link TLUpdateNotifySettings}: updateNotifySettings#0</li>
 * <li>{@link TLUpdatePeerLocated}: updatePeerLocated#0</li>
 * <li>{@link TLUpdatePeerSettings}: updatePeerSettings#0</li>
 * <li>{@link TLUpdatePhoneCall}: updatePhoneCall#0</li>
 * <li>{@link TLUpdatePinnedDialogs}: updatePinnedDialogs#0</li>
 * <li>{@link TLUpdatePrivacy}: updatePrivacy#0</li>
 * <li>{@link TLUpdatePtsChanged}: updatePtsChanged#0</li>
 * <li>{@link TLUpdateReadChannelInbox}: updateReadChannelInbox#0</li>
 * <li>{@link TLUpdateReadChannelOutbox}: updateReadChannelOutbox#0</li>
 * <li>{@link TLUpdateReadFeaturedStickers}: updateReadFeaturedStickers#0</li>
 * <li>{@link TLUpdateReadHistoryInbox}: updateReadHistoryInbox#0</li>
 * <li>{@link TLUpdateReadHistoryOutbox}: updateReadHistoryOutbox#0</li>
 * <li>{@link TLUpdateReadMessagesContents}: updateReadMessagesContents#0</li>
 * <li>{@link TLUpdateRecentStickers}: updateRecentStickers#0</li>
 * <li>{@link TLUpdateSavedGifs}: updateSavedGifs#0</li>
 * <li>{@link TLUpdateServiceNotification}: updateServiceNotification#0</li>
 * <li>{@link TLUpdateStickerSets}: updateStickerSets#0</li>
 * <li>{@link TLUpdateStickerSetsOrder}: updateStickerSetsOrder#0</li>
 * <li>{@link TLUpdateTheme}: updateTheme#0</li>
 * <li>{@link TLUpdateUserBlocked}: updateUserBlocked#0</li>
 * <li>{@link TLUpdateUserName}: updateUserName#0</li>
 * <li>{@link TLUpdateUserPhone}: updateUserPhone#0</li>
 * <li>{@link TLUpdateUserPhoto}: updateUserPhoto#0</li>
 * <li>{@link TLUpdateUserPinnedMessage}: updateUserPinnedMessage#0</li>
 * <li>{@link TLUpdateUserStatus}: updateUserStatus#0</li>
 * <li>{@link TLUpdateUserTyping}: updateUserTyping#0</li>
 * <li>{@link TLUpdateWebPage}: updateWebPage#0</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsUpdate extends TLObject {
    public TLAbsUpdate() {
    }
}
