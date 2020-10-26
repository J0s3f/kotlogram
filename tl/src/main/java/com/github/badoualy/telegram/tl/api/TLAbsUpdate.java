package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLUpdateBotCallbackQuery}: updateBotCallbackQuery#e73547e1</li>
 * <li>{@link TLUpdateBotInlineQuery}: updateBotInlineQuery#54826690</li>
 * <li>{@link TLUpdateBotInlineSend}: updateBotInlineSend#e48f964</li>
 * <li>{@link TLUpdateBotPrecheckoutQuery}: updateBotPrecheckoutQuery#5d2f3aa9</li>
 * <li>{@link TLUpdateBotShippingQuery}: updateBotShippingQuery#e0cdc940</li>
 * <li>{@link TLUpdateBotWebhookJSON}: updateBotWebhookJSON#8317c0c3</li>
 * <li>{@link TLUpdateBotWebhookJSONQuery}: updateBotWebhookJSONQuery#9b9240a6</li>
 * <li>{@link TLUpdateChannel}: updateChannel#b6d45656</li>
 * <li>{@link TLUpdateChannelAvailableMessages}: updateChannelAvailableMessages#70db6837</li>
 * <li>{@link TLUpdateChannelMessageViews}: updateChannelMessageViews#98a12b4b</li>
 * <li>{@link TLUpdateChannelPinnedMessage}: updateChannelPinnedMessage#98592475</li>
 * <li>{@link TLUpdateChannelReadMessagesContents}: updateChannelReadMessagesContents#89893b45</li>
 * <li>{@link TLUpdateChannelTooLong}: updateChannelTooLong#eb0467fb</li>
 * <li>{@link TLUpdateChannelWebPage}: updateChannelWebPage#40771900</li>
 * <li>{@link TLUpdateChatDefaultBannedRights}: updateChatDefaultBannedRights#54c01850</li>
 * <li>{@link TLUpdateChatParticipantAdd}: updateChatParticipantAdd#ea4b0e5c</li>
 * <li>{@link TLUpdateChatParticipantAdmin}: updateChatParticipantAdmin#b6901959</li>
 * <li>{@link TLUpdateChatParticipantDelete}: updateChatParticipantDelete#6e5f8c22</li>
 * <li>{@link TLUpdateChatParticipants}: updateChatParticipants#7761198</li>
 * <li>{@link TLUpdateChatPinnedMessage}: updateChatPinnedMessage#e10db349</li>
 * <li>{@link TLUpdateChatUserTyping}: updateChatUserTyping#9a65ea1f</li>
 * <li>{@link TLUpdateConfig}: updateConfig#a229dd06</li>
 * <li>{@link TLUpdateContactsReset}: updateContactsReset#7084a7be</li>
 * <li>{@link TLUpdateDcOptions}: updateDcOptions#8e5e9873</li>
 * <li>{@link TLUpdateDeleteChannelMessages}: updateDeleteChannelMessages#c37521c9</li>
 * <li>{@link TLUpdateDeleteMessages}: updateDeleteMessages#a20db0e5</li>
 * <li>{@link TLUpdateDeleteScheduledMessages}: updateDeleteScheduledMessages#90866cee</li>
 * <li>{@link TLUpdateDialogFilter}: updateDialogFilter#26ffde7d</li>
 * <li>{@link TLUpdateDialogFilterOrder}: updateDialogFilterOrder#a5d72105</li>
 * <li>{@link TLUpdateDialogFilters}: updateDialogFilters#3504914f</li>
 * <li>{@link TLUpdateDialogPinned}: updateDialogPinned#6e6fe51c</li>
 * <li>{@link TLUpdateDialogUnreadMark}: updateDialogUnreadMark#e16459c3</li>
 * <li>{@link TLUpdateDraftMessage}: updateDraftMessage#ee2bb969</li>
 * <li>{@link TLUpdateEditChannelMessage}: updateEditChannelMessage#1b3f4df7</li>
 * <li>{@link TLUpdateEditMessage}: updateEditMessage#e40370a3</li>
 * <li>{@link TLUpdateEncryptedChatTyping}: updateEncryptedChatTyping#1710f156</li>
 * <li>{@link TLUpdateEncryptedMessagesRead}: updateEncryptedMessagesRead#38fe25b7</li>
 * <li>{@link TLUpdateEncryption}: updateEncryption#b4a2e88d</li>
 * <li>{@link TLUpdateFavedStickers}: updateFavedStickers#e511996d</li>
 * <li>{@link TLUpdateFolderPeers}: updateFolderPeers#19360dc0</li>
 * <li>{@link TLUpdateGeoLiveViewed}: updateGeoLiveViewed#871fb939</li>
 * <li>{@link TLUpdateInlineBotCallbackQuery}: updateInlineBotCallbackQuery#f9d27a5a</li>
 * <li>{@link TLUpdateLangPack}: updateLangPack#56022f4d</li>
 * <li>{@link TLUpdateLangPackTooLong}: updateLangPackTooLong#46560264</li>
 * <li>{@link TLUpdateLoginToken}: updateLoginToken#564fe691</li>
 * <li>{@link TLUpdateMessageID}: updateMessageID#4e90bfd6</li>
 * <li>{@link TLUpdateMessagePoll}: updateMessagePoll#aca1657b</li>
 * <li>{@link TLUpdateMessagePollVote}: updateMessagePollVote#42f88f2c</li>
 * <li>{@link TLUpdateNewChannelMessage}: updateNewChannelMessage#62ba04d9</li>
 * <li>{@link TLUpdateNewEncryptedMessage}: updateNewEncryptedMessage#12bcbd9a</li>
 * <li>{@link TLUpdateNewMessage}: updateNewMessage#1f2b0afd</li>
 * <li>{@link TLUpdateNewScheduledMessage}: updateNewScheduledMessage#39a51dfb</li>
 * <li>{@link TLUpdateNewStickerSet}: updateNewStickerSet#688a30aa</li>
 * <li>{@link TLUpdateNotifySettings}: updateNotifySettings#bec268ef</li>
 * <li>{@link TLUpdatePeerLocated}: updatePeerLocated#b4afcfb0</li>
 * <li>{@link TLUpdatePeerSettings}: updatePeerSettings#6a7e7366</li>
 * <li>{@link TLUpdatePhoneCall}: updatePhoneCall#ab0f6b1e</li>
 * <li>{@link TLUpdatePinnedDialogs}: updatePinnedDialogs#fa0f3ca2</li>
 * <li>{@link TLUpdatePrivacy}: updatePrivacy#ee3b272a</li>
 * <li>{@link TLUpdatePtsChanged}: updatePtsChanged#3354678f</li>
 * <li>{@link TLUpdateReadChannelInbox}: updateReadChannelInbox#330b5424</li>
 * <li>{@link TLUpdateReadChannelOutbox}: updateReadChannelOutbox#25d6c9c7</li>
 * <li>{@link TLUpdateReadFeaturedStickers}: updateReadFeaturedStickers#571d2742</li>
 * <li>{@link TLUpdateReadHistoryInbox}: updateReadHistoryInbox#9c974fdf</li>
 * <li>{@link TLUpdateReadHistoryOutbox}: updateReadHistoryOutbox#2f2f21bf</li>
 * <li>{@link TLUpdateReadMessagesContents}: updateReadMessagesContents#68c13933</li>
 * <li>{@link TLUpdateRecentStickers}: updateRecentStickers#9a422c20</li>
 * <li>{@link TLUpdateSavedGifs}: updateSavedGifs#9375341e</li>
 * <li>{@link TLUpdateServiceNotification}: updateServiceNotification#ebe46819</li>
 * <li>{@link TLUpdateStickerSets}: updateStickerSets#43ae3dec</li>
 * <li>{@link TLUpdateStickerSetsOrder}: updateStickerSetsOrder#bb2d201</li>
 * <li>{@link TLUpdateTheme}: updateTheme#8216fba3</li>
 * <li>{@link TLUpdateUserBlocked}: updateUserBlocked#80ece81a</li>
 * <li>{@link TLUpdateUserName}: updateUserName#a7332b73</li>
 * <li>{@link TLUpdateUserPhone}: updateUserPhone#12b9417b</li>
 * <li>{@link TLUpdateUserPhoto}: updateUserPhoto#95313b0c</li>
 * <li>{@link TLUpdateUserPinnedMessage}: updateUserPinnedMessage#4c43da18</li>
 * <li>{@link TLUpdateUserStatus}: updateUserStatus#1bfbd823</li>
 * <li>{@link TLUpdateUserTyping}: updateUserTyping#5c486927</li>
 * <li>{@link TLUpdateWebPage}: updateWebPage#7f891213</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsUpdate extends TLObject {
    public TLAbsUpdate() {
    }
}
