package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.api.account.TLAbsThemes;
import com.github.badoualy.telegram.tl.api.account.TLAbsWallPapers;
import com.github.badoualy.telegram.tl.api.account.TLAuthorizationForm;
import com.github.badoualy.telegram.tl.api.account.TLAuthorizations;
import com.github.badoualy.telegram.tl.api.account.TLContentSettings;
import com.github.badoualy.telegram.tl.api.account.TLPassword;
import com.github.badoualy.telegram.tl.api.account.TLPasswordInputSettings;
import com.github.badoualy.telegram.tl.api.account.TLPasswordSettings;
import com.github.badoualy.telegram.tl.api.account.TLPrivacyRules;
import com.github.badoualy.telegram.tl.api.account.TLSentEmailCode;
import com.github.badoualy.telegram.tl.api.account.TLTakeout;
import com.github.badoualy.telegram.tl.api.account.TLTmpPassword;
import com.github.badoualy.telegram.tl.api.account.TLWebAuthorizations;
import com.github.badoualy.telegram.tl.api.auth.TLAbsAuthorization;
import com.github.badoualy.telegram.tl.api.auth.TLAbsLoginToken;
import com.github.badoualy.telegram.tl.api.auth.TLExportedAuthorization;
import com.github.badoualy.telegram.tl.api.auth.TLPasswordRecovery;
import com.github.badoualy.telegram.tl.api.auth.TLSentCode;
import com.github.badoualy.telegram.tl.api.channels.TLAbsChannelParticipants;
import com.github.badoualy.telegram.tl.api.channels.TLAdminLogResults;
import com.github.badoualy.telegram.tl.api.channels.TLChannelParticipant;
import com.github.badoualy.telegram.tl.api.contacts.TLAbsBlocked;
import com.github.badoualy.telegram.tl.api.contacts.TLAbsContacts;
import com.github.badoualy.telegram.tl.api.contacts.TLAbsTopPeers;
import com.github.badoualy.telegram.tl.api.contacts.TLFound;
import com.github.badoualy.telegram.tl.api.contacts.TLImportedContacts;
import com.github.badoualy.telegram.tl.api.contacts.TLResolvedPeer;
import com.github.badoualy.telegram.tl.api.help.TLAbsAppUpdate;
import com.github.badoualy.telegram.tl.api.help.TLAbsDeepLinkInfo;
import com.github.badoualy.telegram.tl.api.help.TLAbsPassportConfig;
import com.github.badoualy.telegram.tl.api.help.TLAbsPromoData;
import com.github.badoualy.telegram.tl.api.help.TLAbsTermsOfServiceUpdate;
import com.github.badoualy.telegram.tl.api.help.TLAbsUserInfo;
import com.github.badoualy.telegram.tl.api.help.TLInviteText;
import com.github.badoualy.telegram.tl.api.help.TLRecentMeUrls;
import com.github.badoualy.telegram.tl.api.help.TLSupport;
import com.github.badoualy.telegram.tl.api.help.TLSupportName;
import com.github.badoualy.telegram.tl.api.messages.TLAbsAllStickers;
import com.github.badoualy.telegram.tl.api.messages.TLAbsChats;
import com.github.badoualy.telegram.tl.api.messages.TLAbsDhConfig;
import com.github.badoualy.telegram.tl.api.messages.TLAbsDialogs;
import com.github.badoualy.telegram.tl.api.messages.TLAbsFavedStickers;
import com.github.badoualy.telegram.tl.api.messages.TLAbsFeaturedStickers;
import com.github.badoualy.telegram.tl.api.messages.TLAbsFoundStickerSets;
import com.github.badoualy.telegram.tl.api.messages.TLAbsMessages;
import com.github.badoualy.telegram.tl.api.messages.TLAbsRecentStickers;
import com.github.badoualy.telegram.tl.api.messages.TLAbsSavedGifs;
import com.github.badoualy.telegram.tl.api.messages.TLAbsSentEncryptedMessage;
import com.github.badoualy.telegram.tl.api.messages.TLAbsStickerSetInstallResult;
import com.github.badoualy.telegram.tl.api.messages.TLAbsStickers;
import com.github.badoualy.telegram.tl.api.messages.TLAffectedHistory;
import com.github.badoualy.telegram.tl.api.messages.TLAffectedMessages;
import com.github.badoualy.telegram.tl.api.messages.TLArchivedStickers;
import com.github.badoualy.telegram.tl.api.messages.TLBotCallbackAnswer;
import com.github.badoualy.telegram.tl.api.messages.TLBotResults;
import com.github.badoualy.telegram.tl.api.messages.TLChatFull;
import com.github.badoualy.telegram.tl.api.messages.TLFoundGifs;
import com.github.badoualy.telegram.tl.api.messages.TLHighScores;
import com.github.badoualy.telegram.tl.api.messages.TLInactiveChats;
import com.github.badoualy.telegram.tl.api.messages.TLMessageEditData;
import com.github.badoualy.telegram.tl.api.messages.TLPeerDialogs;
import com.github.badoualy.telegram.tl.api.messages.TLSearchCounter;
import com.github.badoualy.telegram.tl.api.messages.TLStickerSet;
import com.github.badoualy.telegram.tl.api.messages.TLVotesList;
import com.github.badoualy.telegram.tl.api.payments.TLAbsPaymentResult;
import com.github.badoualy.telegram.tl.api.payments.TLBankCardData;
import com.github.badoualy.telegram.tl.api.payments.TLPaymentForm;
import com.github.badoualy.telegram.tl.api.payments.TLPaymentReceipt;
import com.github.badoualy.telegram.tl.api.payments.TLSavedInfo;
import com.github.badoualy.telegram.tl.api.payments.TLValidatedRequestedInfo;
import com.github.badoualy.telegram.tl.api.phone.TLPhoneCall;
import com.github.badoualy.telegram.tl.api.photos.TLAbsPhotos;
import com.github.badoualy.telegram.tl.api.photos.TLPhoto;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountAcceptAuthorization;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountCancelPasswordEmail;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountChangePhone;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountCheckUsername;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountConfirmPasswordEmail;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountConfirmPhone;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountCreateTheme;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountDeleteAccount;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountDeleteSecureValue;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountFinishTakeoutSession;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountGetAccountTTL;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountGetAllSecureValues;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountGetAuthorizationForm;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountGetAuthorizations;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountGetAutoDownloadSettings;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountGetContactSignUpNotification;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountGetContentSettings;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountGetMultiWallPapers;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountGetNotifyExceptions;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountGetNotifySettings;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountGetPassword;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountGetPasswordSettings;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountGetPrivacy;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountGetSecureValue;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountGetTheme;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountGetThemes;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountGetTmpPassword;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountGetWallPaper;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountGetWallPapers;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountGetWebAuthorizations;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountInitTakeoutSession;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountInstallTheme;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountInstallWallPaper;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountRegisterDevice;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountReportPeer;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountResendPasswordEmail;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountResetAuthorization;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountResetNotifySettings;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountResetWallPapers;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountResetWebAuthorization;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountResetWebAuthorizations;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountSaveAutoDownloadSettings;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountSaveSecureValue;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountSaveTheme;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountSaveWallPaper;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountSendChangePhoneCode;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountSendConfirmPhoneCode;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountSendVerifyEmailCode;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountSendVerifyPhoneCode;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountSetAccountTTL;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountSetContactSignUpNotification;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountSetContentSettings;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountSetPrivacy;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountUnregisterDevice;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountUpdateDeviceLocked;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountUpdateNotifySettings;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountUpdatePasswordSettings;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountUpdateProfile;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountUpdateStatus;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountUpdateTheme;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountUpdateUsername;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountUploadTheme;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountUploadWallPaper;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountVerifyEmail;
import com.github.badoualy.telegram.tl.api.request.TLRequestAccountVerifyPhone;
import com.github.badoualy.telegram.tl.api.request.TLRequestAuthAcceptLoginToken;
import com.github.badoualy.telegram.tl.api.request.TLRequestAuthBindTempAuthKey;
import com.github.badoualy.telegram.tl.api.request.TLRequestAuthCancelCode;
import com.github.badoualy.telegram.tl.api.request.TLRequestAuthCheckPassword;
import com.github.badoualy.telegram.tl.api.request.TLRequestAuthDropTempAuthKeys;
import com.github.badoualy.telegram.tl.api.request.TLRequestAuthExportAuthorization;
import com.github.badoualy.telegram.tl.api.request.TLRequestAuthExportLoginToken;
import com.github.badoualy.telegram.tl.api.request.TLRequestAuthImportAuthorization;
import com.github.badoualy.telegram.tl.api.request.TLRequestAuthImportBotAuthorization;
import com.github.badoualy.telegram.tl.api.request.TLRequestAuthImportLoginToken;
import com.github.badoualy.telegram.tl.api.request.TLRequestAuthLogOut;
import com.github.badoualy.telegram.tl.api.request.TLRequestAuthRecoverPassword;
import com.github.badoualy.telegram.tl.api.request.TLRequestAuthRequestPasswordRecovery;
import com.github.badoualy.telegram.tl.api.request.TLRequestAuthResendCode;
import com.github.badoualy.telegram.tl.api.request.TLRequestAuthResetAuthorizations;
import com.github.badoualy.telegram.tl.api.request.TLRequestAuthSendCode;
import com.github.badoualy.telegram.tl.api.request.TLRequestAuthSignIn;
import com.github.badoualy.telegram.tl.api.request.TLRequestAuthSignUp;
import com.github.badoualy.telegram.tl.api.request.TLRequestBotsAnswerWebhookJSONQuery;
import com.github.badoualy.telegram.tl.api.request.TLRequestBotsSendCustomRequest;
import com.github.badoualy.telegram.tl.api.request.TLRequestBotsSetBotCommands;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsCheckUsername;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsCreateChannel;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsDeleteChannel;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsDeleteHistory;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsDeleteMessages;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsDeleteUserHistory;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsEditAdmin;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsEditBanned;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsEditCreator;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsEditLocation;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsEditPhoto;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsEditTitle;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsExportMessageLink;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsGetAdminLog;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsGetAdminedPublicChannels;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsGetChannels;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsGetFullChannel;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsGetGroupsForDiscussion;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsGetInactiveChannels;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsGetLeftChannels;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsGetMessages;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsGetParticipant;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsGetParticipants;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsInviteToChannel;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsJoinChannel;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsLeaveChannel;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsReadHistory;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsReadMessageContents;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsReportSpam;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsSetDiscussionGroup;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsSetStickers;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsTogglePreHistoryHidden;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsToggleSignatures;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsToggleSlowMode;
import com.github.badoualy.telegram.tl.api.request.TLRequestChannelsUpdateUsername;
import com.github.badoualy.telegram.tl.api.request.TLRequestContactsAcceptContact;
import com.github.badoualy.telegram.tl.api.request.TLRequestContactsAddContact;
import com.github.badoualy.telegram.tl.api.request.TLRequestContactsBlock;
import com.github.badoualy.telegram.tl.api.request.TLRequestContactsDeleteByPhones;
import com.github.badoualy.telegram.tl.api.request.TLRequestContactsDeleteContacts;
import com.github.badoualy.telegram.tl.api.request.TLRequestContactsGetBlocked;
import com.github.badoualy.telegram.tl.api.request.TLRequestContactsGetContactIDs;
import com.github.badoualy.telegram.tl.api.request.TLRequestContactsGetContacts;
import com.github.badoualy.telegram.tl.api.request.TLRequestContactsGetLocated;
import com.github.badoualy.telegram.tl.api.request.TLRequestContactsGetSaved;
import com.github.badoualy.telegram.tl.api.request.TLRequestContactsGetStatuses;
import com.github.badoualy.telegram.tl.api.request.TLRequestContactsGetTopPeers;
import com.github.badoualy.telegram.tl.api.request.TLRequestContactsImportContacts;
import com.github.badoualy.telegram.tl.api.request.TLRequestContactsResetSaved;
import com.github.badoualy.telegram.tl.api.request.TLRequestContactsResetTopPeerRating;
import com.github.badoualy.telegram.tl.api.request.TLRequestContactsResolveUsername;
import com.github.badoualy.telegram.tl.api.request.TLRequestContactsSearch;
import com.github.badoualy.telegram.tl.api.request.TLRequestContactsToggleTopPeers;
import com.github.badoualy.telegram.tl.api.request.TLRequestContactsUnblock;
import com.github.badoualy.telegram.tl.api.request.TLRequestFoldersDeleteFolder;
import com.github.badoualy.telegram.tl.api.request.TLRequestFoldersEditPeerFolders;
import com.github.badoualy.telegram.tl.api.request.TLRequestHelpAcceptTermsOfService;
import com.github.badoualy.telegram.tl.api.request.TLRequestHelpEditUserInfo;
import com.github.badoualy.telegram.tl.api.request.TLRequestHelpGetAppChangelog;
import com.github.badoualy.telegram.tl.api.request.TLRequestHelpGetAppConfig;
import com.github.badoualy.telegram.tl.api.request.TLRequestHelpGetAppUpdate;
import com.github.badoualy.telegram.tl.api.request.TLRequestHelpGetCdnConfig;
import com.github.badoualy.telegram.tl.api.request.TLRequestHelpGetConfig;
import com.github.badoualy.telegram.tl.api.request.TLRequestHelpGetDeepLinkInfo;
import com.github.badoualy.telegram.tl.api.request.TLRequestHelpGetInviteText;
import com.github.badoualy.telegram.tl.api.request.TLRequestHelpGetNearestDc;
import com.github.badoualy.telegram.tl.api.request.TLRequestHelpGetPassportConfig;
import com.github.badoualy.telegram.tl.api.request.TLRequestHelpGetPromoData;
import com.github.badoualy.telegram.tl.api.request.TLRequestHelpGetRecentMeUrls;
import com.github.badoualy.telegram.tl.api.request.TLRequestHelpGetSupport;
import com.github.badoualy.telegram.tl.api.request.TLRequestHelpGetSupportName;
import com.github.badoualy.telegram.tl.api.request.TLRequestHelpGetTermsOfServiceUpdate;
import com.github.badoualy.telegram.tl.api.request.TLRequestHelpGetUserInfo;
import com.github.badoualy.telegram.tl.api.request.TLRequestHelpHidePromoData;
import com.github.badoualy.telegram.tl.api.request.TLRequestHelpSaveAppLog;
import com.github.badoualy.telegram.tl.api.request.TLRequestHelpSetBotUpdatesStatus;
import com.github.badoualy.telegram.tl.api.request.TLRequestInitConnection;
import com.github.badoualy.telegram.tl.api.request.TLRequestInvokeAfterMsg;
import com.github.badoualy.telegram.tl.api.request.TLRequestInvokeAfterMsgs;
import com.github.badoualy.telegram.tl.api.request.TLRequestInvokeWithLayer;
import com.github.badoualy.telegram.tl.api.request.TLRequestInvokeWithMessagesRange;
import com.github.badoualy.telegram.tl.api.request.TLRequestInvokeWithTakeout;
import com.github.badoualy.telegram.tl.api.request.TLRequestInvokeWithoutUpdates;
import com.github.badoualy.telegram.tl.api.request.TLRequestLangpackGetDifference;
import com.github.badoualy.telegram.tl.api.request.TLRequestLangpackGetLangPack;
import com.github.badoualy.telegram.tl.api.request.TLRequestLangpackGetLanguage;
import com.github.badoualy.telegram.tl.api.request.TLRequestLangpackGetLanguages;
import com.github.badoualy.telegram.tl.api.request.TLRequestLangpackGetStrings;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesAcceptEncryption;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesAcceptUrlAuth;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesAddChatUser;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesCheckChatInvite;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesClearAllDrafts;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesClearRecentStickers;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesCreateChat;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesDeleteChatUser;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesDeleteHistory;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesDeleteMessages;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesDeleteScheduledMessages;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesDiscardEncryption;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesEditChatAbout;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesEditChatAdmin;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesEditChatDefaultBannedRights;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesEditChatPhoto;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesEditChatTitle;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesEditInlineBotMessage;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesEditMessage;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesExportChatInvite;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesFaveSticker;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesForwardMessages;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetAllChats;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetAllDrafts;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetAllStickers;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetArchivedStickers;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetAttachedStickers;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetBotCallbackAnswer;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetChats;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetCommonChats;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetDhConfig;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetDialogFilters;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetDialogUnreadMarks;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetDialogs;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetDocumentByHash;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetEmojiKeywords;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetEmojiKeywordsDifference;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetEmojiKeywordsLanguages;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetEmojiURL;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetFavedStickers;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetFeaturedStickers;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetFullChat;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetGameHighScores;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetHistory;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetInlineBotResults;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetInlineGameHighScores;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetMaskStickers;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetMessageEditData;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetMessages;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetMessagesViews;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetOldFeaturedStickers;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetOnlines;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetPeerDialogs;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetPeerSettings;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetPinnedDialogs;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetPollResults;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetPollVotes;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetRecentLocations;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetRecentStickers;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetSavedGifs;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetScheduledHistory;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetScheduledMessages;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetSearchCounters;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetSplitRanges;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetStatsURL;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetStickerSet;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetStickers;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetSuggestedDialogFilters;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetUnreadMentions;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetWebPage;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesGetWebPagePreview;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesHidePeerSettingsBar;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesImportChatInvite;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesInstallStickerSet;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesMarkDialogUnread;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesMigrateChat;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesReadEncryptedHistory;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesReadFeaturedStickers;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesReadHistory;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesReadMentions;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesReadMessageContents;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesReceivedMessages;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesReceivedQueue;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesReorderPinnedDialogs;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesReorderStickerSets;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesReport;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesReportEncryptedSpam;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesReportSpam;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesRequestEncryption;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesRequestUrlAuth;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesSaveDraft;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesSaveGif;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesSaveRecentSticker;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesSearch;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesSearchGifs;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesSearchGlobal;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesSearchStickerSets;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesSendEncrypted;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesSendEncryptedFile;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesSendEncryptedService;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesSendInlineBotResult;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesSendMedia;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesSendMessage;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesSendMultiMedia;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesSendScheduledMessages;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesSendScreenshotNotification;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesSendVote;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesSetBotCallbackAnswer;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesSetBotPrecheckoutResults;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesSetBotShippingResults;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesSetEncryptedTyping;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesSetGameScore;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesSetInlineBotResults;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesSetInlineGameScore;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesSetTyping;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesStartBot;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesToggleDialogPin;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesToggleStickerSets;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesUninstallStickerSet;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesUpdateDialogFilter;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesUpdateDialogFiltersOrder;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesUpdatePinnedMessage;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesUploadEncryptedFile;
import com.github.badoualy.telegram.tl.api.request.TLRequestMessagesUploadMedia;
import com.github.badoualy.telegram.tl.api.request.TLRequestPaymentsClearSavedInfo;
import com.github.badoualy.telegram.tl.api.request.TLRequestPaymentsGetBankCardData;
import com.github.badoualy.telegram.tl.api.request.TLRequestPaymentsGetPaymentForm;
import com.github.badoualy.telegram.tl.api.request.TLRequestPaymentsGetPaymentReceipt;
import com.github.badoualy.telegram.tl.api.request.TLRequestPaymentsGetSavedInfo;
import com.github.badoualy.telegram.tl.api.request.TLRequestPaymentsSendPaymentForm;
import com.github.badoualy.telegram.tl.api.request.TLRequestPaymentsValidateRequestedInfo;
import com.github.badoualy.telegram.tl.api.request.TLRequestPhoneAcceptCall;
import com.github.badoualy.telegram.tl.api.request.TLRequestPhoneConfirmCall;
import com.github.badoualy.telegram.tl.api.request.TLRequestPhoneDiscardCall;
import com.github.badoualy.telegram.tl.api.request.TLRequestPhoneGetCallConfig;
import com.github.badoualy.telegram.tl.api.request.TLRequestPhoneReceivedCall;
import com.github.badoualy.telegram.tl.api.request.TLRequestPhoneRequestCall;
import com.github.badoualy.telegram.tl.api.request.TLRequestPhoneSaveCallDebug;
import com.github.badoualy.telegram.tl.api.request.TLRequestPhoneSetCallRating;
import com.github.badoualy.telegram.tl.api.request.TLRequestPhotosDeletePhotos;
import com.github.badoualy.telegram.tl.api.request.TLRequestPhotosGetUserPhotos;
import com.github.badoualy.telegram.tl.api.request.TLRequestPhotosUpdateProfilePhoto;
import com.github.badoualy.telegram.tl.api.request.TLRequestPhotosUploadProfilePhoto;
import com.github.badoualy.telegram.tl.api.request.TLRequestStatsGetBroadcastStats;
import com.github.badoualy.telegram.tl.api.request.TLRequestStatsLoadAsyncGraph;
import com.github.badoualy.telegram.tl.api.request.TLRequestStickersAddStickerToSet;
import com.github.badoualy.telegram.tl.api.request.TLRequestStickersChangeStickerPosition;
import com.github.badoualy.telegram.tl.api.request.TLRequestStickersCreateStickerSet;
import com.github.badoualy.telegram.tl.api.request.TLRequestStickersRemoveStickerFromSet;
import com.github.badoualy.telegram.tl.api.request.TLRequestStickersSetStickerSetThumb;
import com.github.badoualy.telegram.tl.api.request.TLRequestUpdatesGetChannelDifference;
import com.github.badoualy.telegram.tl.api.request.TLRequestUpdatesGetDifference;
import com.github.badoualy.telegram.tl.api.request.TLRequestUpdatesGetState;
import com.github.badoualy.telegram.tl.api.request.TLRequestUploadGetCdnFile;
import com.github.badoualy.telegram.tl.api.request.TLRequestUploadGetCdnFileHashes;
import com.github.badoualy.telegram.tl.api.request.TLRequestUploadGetFile;
import com.github.badoualy.telegram.tl.api.request.TLRequestUploadGetFileHashes;
import com.github.badoualy.telegram.tl.api.request.TLRequestUploadGetWebFile;
import com.github.badoualy.telegram.tl.api.request.TLRequestUploadReuploadCdnFile;
import com.github.badoualy.telegram.tl.api.request.TLRequestUploadSaveBigFilePart;
import com.github.badoualy.telegram.tl.api.request.TLRequestUploadSaveFilePart;
import com.github.badoualy.telegram.tl.api.request.TLRequestUsersGetFullUser;
import com.github.badoualy.telegram.tl.api.request.TLRequestUsersGetUsers;
import com.github.badoualy.telegram.tl.api.request.TLRequestUsersSetSecureValueErrors;
import com.github.badoualy.telegram.tl.api.stats.TLBroadcastStats;
import com.github.badoualy.telegram.tl.api.updates.TLAbsChannelDifference;
import com.github.badoualy.telegram.tl.api.updates.TLAbsDifference;
import com.github.badoualy.telegram.tl.api.updates.TLState;
import com.github.badoualy.telegram.tl.api.upload.TLAbsCdnFile;
import com.github.badoualy.telegram.tl.api.upload.TLAbsFile;
import com.github.badoualy.telegram.tl.api.upload.TLWebFile;
import com.github.badoualy.telegram.tl.core.TLBool;
import com.github.badoualy.telegram.tl.core.TLBytes;
import com.github.badoualy.telegram.tl.core.TLIntVector;
import com.github.badoualy.telegram.tl.core.TLLongVector;
import com.github.badoualy.telegram.tl.core.TLMethod;
import com.github.badoualy.telegram.tl.core.TLObject;
import com.github.badoualy.telegram.tl.core.TLStringVector;
import com.github.badoualy.telegram.tl.core.TLVector;
import com.github.badoualy.telegram.tl.exception.RpcErrorException;
import java.io.IOException;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;

/**
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
@SuppressWarnings({"unused", "unchecked", "RedundantCast"})
public abstract class TelegramApiWrapper implements TelegramApi {
    public abstract <T extends TLObject> T executeRpcQuery(TLMethod<T> method) throws RpcErrorException, IOException;

    @Override
    public TLBool accountAcceptAuthorization(int botId, String scope, String publicKey, TLVector<TLSecureValueHash> valueHashes, TLSecureCredentialsEncrypted credentials) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountAcceptAuthorization(botId, scope, publicKey, valueHashes, credentials));
    }

    @Override
    public TLBool accountCancelPasswordEmail() throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountCancelPasswordEmail());
    }

    @Override
    public TLAbsUser accountChangePhone(String phoneNumber, String phoneCodeHash, String phoneCode) throws RpcErrorException, IOException {
        return (TLAbsUser) executeRpcQuery(new TLRequestAccountChangePhone(phoneNumber, phoneCodeHash, phoneCode));
    }

    @Override
    public TLBool accountCheckUsername(String username) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountCheckUsername(username));
    }

    @Override
    public TLBool accountConfirmPasswordEmail(String code) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountConfirmPasswordEmail(code));
    }

    @Override
    public TLBool accountConfirmPhone(String phoneCodeHash, String phoneCode) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountConfirmPhone(phoneCodeHash, phoneCode));
    }

    @Override
    public TLTheme accountCreateTheme(String slug, String title, TLAbsInputDocument document, TLInputThemeSettings settings) throws RpcErrorException, IOException {
        return (TLTheme) executeRpcQuery(new TLRequestAccountCreateTheme(slug, title, document, settings));
    }

    @Override
    public TLBool accountDeleteAccount(String reason) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountDeleteAccount(reason));
    }

    @Override
    public TLBool accountDeleteSecureValue(TLVector<TLAbsSecureValueType> types) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountDeleteSecureValue(types));
    }

    @Override
    public TLBool accountFinishTakeoutSession(boolean success) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountFinishTakeoutSession(success));
    }

    @Override
    public TLAccountDaysTTL accountGetAccountTTL() throws RpcErrorException, IOException {
        return (TLAccountDaysTTL) executeRpcQuery(new TLRequestAccountGetAccountTTL());
    }

    @Override
    public TLVector<TLSecureValue> accountGetAllSecureValues() throws RpcErrorException, IOException {
        return (TLVector<TLSecureValue>) executeRpcQuery(new TLRequestAccountGetAllSecureValues());
    }

    @Override
    public TLAuthorizationForm accountGetAuthorizationForm(int botId, String scope, String publicKey) throws RpcErrorException, IOException {
        return (TLAuthorizationForm) executeRpcQuery(new TLRequestAccountGetAuthorizationForm(botId, scope, publicKey));
    }

    @Override
    public TLAuthorizations accountGetAuthorizations() throws RpcErrorException, IOException {
        return (TLAuthorizations) executeRpcQuery(new TLRequestAccountGetAuthorizations());
    }

    @Override
    public com.github.badoualy.telegram.tl.api.account.TLAutoDownloadSettings accountGetAutoDownloadSettings() throws RpcErrorException, IOException {
        return (com.github.badoualy.telegram.tl.api.account.TLAutoDownloadSettings) executeRpcQuery(new TLRequestAccountGetAutoDownloadSettings());
    }

    @Override
    public TLBool accountGetContactSignUpNotification() throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountGetContactSignUpNotification());
    }

    @Override
    public TLContentSettings accountGetContentSettings() throws RpcErrorException, IOException {
        return (TLContentSettings) executeRpcQuery(new TLRequestAccountGetContentSettings());
    }

    @Override
    public TLVector<TLAbsWallPaper> accountGetMultiWallPapers(TLVector<TLAbsInputWallPaper> wallpapers) throws RpcErrorException, IOException {
        return (TLVector<TLAbsWallPaper>) executeRpcQuery(new TLRequestAccountGetMultiWallPapers(wallpapers));
    }

    @Override
    public TLAbsUpdates accountGetNotifyExceptions(boolean compareSound, TLAbsInputNotifyPeer peer) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestAccountGetNotifyExceptions(compareSound, peer));
    }

    @Override
    public TLPeerNotifySettings accountGetNotifySettings(TLAbsInputNotifyPeer peer) throws RpcErrorException, IOException {
        return (TLPeerNotifySettings) executeRpcQuery(new TLRequestAccountGetNotifySettings(peer));
    }

    @Override
    public TLPassword accountGetPassword() throws RpcErrorException, IOException {
        return (TLPassword) executeRpcQuery(new TLRequestAccountGetPassword());
    }

    @Override
    public TLPasswordSettings accountGetPasswordSettings(TLAbsInputCheckPasswordSRP password) throws RpcErrorException, IOException {
        return (TLPasswordSettings) executeRpcQuery(new TLRequestAccountGetPasswordSettings(password));
    }

    @Override
    public TLPrivacyRules accountGetPrivacy(TLAbsInputPrivacyKey key) throws RpcErrorException, IOException {
        return (TLPrivacyRules) executeRpcQuery(new TLRequestAccountGetPrivacy(key));
    }

    @Override
    public TLVector<TLSecureValue> accountGetSecureValue(TLVector<TLAbsSecureValueType> types) throws RpcErrorException, IOException {
        return (TLVector<TLSecureValue>) executeRpcQuery(new TLRequestAccountGetSecureValue(types));
    }

    @Override
    public TLTheme accountGetTheme(String format, TLAbsInputTheme theme, long documentId) throws RpcErrorException, IOException {
        return (TLTheme) executeRpcQuery(new TLRequestAccountGetTheme(format, theme, documentId));
    }

    @Override
    public TLAbsThemes accountGetThemes(String format, int hash) throws RpcErrorException, IOException {
        return (TLAbsThemes) executeRpcQuery(new TLRequestAccountGetThemes(format, hash));
    }

    @Override
    public TLTmpPassword accountGetTmpPassword(TLAbsInputCheckPasswordSRP password, int period) throws RpcErrorException, IOException {
        return (TLTmpPassword) executeRpcQuery(new TLRequestAccountGetTmpPassword(password, period));
    }

    @Override
    public TLAbsWallPaper accountGetWallPaper(TLAbsInputWallPaper wallpaper) throws RpcErrorException, IOException {
        return (TLAbsWallPaper) executeRpcQuery(new TLRequestAccountGetWallPaper(wallpaper));
    }

    @Override
    public TLAbsWallPapers accountGetWallPapers(int hash) throws RpcErrorException, IOException {
        return (TLAbsWallPapers) executeRpcQuery(new TLRequestAccountGetWallPapers(hash));
    }

    @Override
    public TLWebAuthorizations accountGetWebAuthorizations() throws RpcErrorException, IOException {
        return (TLWebAuthorizations) executeRpcQuery(new TLRequestAccountGetWebAuthorizations());
    }

    @Override
    public TLTakeout accountInitTakeoutSession(boolean contacts, boolean messageUsers, boolean messageChats, boolean messageMegagroups, boolean messageChannels, boolean files, Integer fileMaxSize) throws RpcErrorException, IOException {
        return (TLTakeout) executeRpcQuery(new TLRequestAccountInitTakeoutSession(contacts, messageUsers, messageChats, messageMegagroups, messageChannels, files, fileMaxSize));
    }

    @Override
    public TLBool accountInstallTheme(boolean dark, String format, TLAbsInputTheme theme) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountInstallTheme(dark, format, theme));
    }

    @Override
    public TLBool accountInstallWallPaper(TLAbsInputWallPaper wallpaper, TLWallPaperSettings settings) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountInstallWallPaper(wallpaper, settings));
    }

    @Override
    public TLBool accountRegisterDevice(boolean noMuted, int tokenType, String token, boolean appSandbox, TLBytes secret, TLIntVector otherUids) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountRegisterDevice(noMuted, tokenType, token, appSandbox, secret, otherUids));
    }

    @Override
    public TLBool accountReportPeer(TLAbsInputPeer peer, TLAbsReportReason reason) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountReportPeer(peer, reason));
    }

    @Override
    public TLBool accountResendPasswordEmail() throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountResendPasswordEmail());
    }

    @Override
    public TLBool accountResetAuthorization(long hash) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountResetAuthorization(hash));
    }

    @Override
    public TLBool accountResetNotifySettings() throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountResetNotifySettings());
    }

    @Override
    public TLBool accountResetWallPapers() throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountResetWallPapers());
    }

    @Override
    public TLBool accountResetWebAuthorization(long hash) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountResetWebAuthorization(hash));
    }

    @Override
    public TLBool accountResetWebAuthorizations() throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountResetWebAuthorizations());
    }

    @Override
    public TLBool accountSaveAutoDownloadSettings(boolean low, boolean high, TLAutoDownloadSettings settings) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountSaveAutoDownloadSettings(low, high, settings));
    }

    @Override
    public TLSecureValue accountSaveSecureValue(TLInputSecureValue value, long secureSecretId) throws RpcErrorException, IOException {
        return (TLSecureValue) executeRpcQuery(new TLRequestAccountSaveSecureValue(value, secureSecretId));
    }

    @Override
    public TLBool accountSaveTheme(TLAbsInputTheme theme, boolean unsave) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountSaveTheme(theme, unsave));
    }

    @Override
    public TLBool accountSaveWallPaper(TLAbsInputWallPaper wallpaper, boolean unsave, TLWallPaperSettings settings) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountSaveWallPaper(wallpaper, unsave, settings));
    }

    @Override
    public TLSentCode accountSendChangePhoneCode(String phoneNumber, TLCodeSettings settings) throws RpcErrorException, IOException {
        return (TLSentCode) executeRpcQuery(new TLRequestAccountSendChangePhoneCode(phoneNumber, settings));
    }

    @Override
    public TLSentCode accountSendConfirmPhoneCode(String hash, TLCodeSettings settings) throws RpcErrorException, IOException {
        return (TLSentCode) executeRpcQuery(new TLRequestAccountSendConfirmPhoneCode(hash, settings));
    }

    @Override
    public TLSentEmailCode accountSendVerifyEmailCode(String email) throws RpcErrorException, IOException {
        return (TLSentEmailCode) executeRpcQuery(new TLRequestAccountSendVerifyEmailCode(email));
    }

    @Override
    public TLSentCode accountSendVerifyPhoneCode(String phoneNumber, TLCodeSettings settings) throws RpcErrorException, IOException {
        return (TLSentCode) executeRpcQuery(new TLRequestAccountSendVerifyPhoneCode(phoneNumber, settings));
    }

    @Override
    public TLBool accountSetAccountTTL(TLAccountDaysTTL ttl) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountSetAccountTTL(ttl));
    }

    @Override
    public TLBool accountSetContactSignUpNotification(boolean silent) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountSetContactSignUpNotification(silent));
    }

    @Override
    public TLBool accountSetContentSettings(boolean sensitiveEnabled) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountSetContentSettings(sensitiveEnabled));
    }

    @Override
    public TLPrivacyRules accountSetPrivacy(TLAbsInputPrivacyKey key, TLVector<TLAbsInputPrivacyRule> rules) throws RpcErrorException, IOException {
        return (TLPrivacyRules) executeRpcQuery(new TLRequestAccountSetPrivacy(key, rules));
    }

    @Override
    public TLBool accountUnregisterDevice(int tokenType, String token, TLIntVector otherUids) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountUnregisterDevice(tokenType, token, otherUids));
    }

    @Override
    public TLBool accountUpdateDeviceLocked(int period) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountUpdateDeviceLocked(period));
    }

    @Override
    public TLBool accountUpdateNotifySettings(TLAbsInputNotifyPeer peer, TLInputPeerNotifySettings settings) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountUpdateNotifySettings(peer, settings));
    }

    @Override
    public TLBool accountUpdatePasswordSettings(TLAbsInputCheckPasswordSRP password, TLPasswordInputSettings newSettings) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountUpdatePasswordSettings(password, newSettings));
    }

    @Override
    public TLAbsUser accountUpdateProfile(String firstName, String lastName, String about) throws RpcErrorException, IOException {
        return (TLAbsUser) executeRpcQuery(new TLRequestAccountUpdateProfile(firstName, lastName, about));
    }

    @Override
    public TLBool accountUpdateStatus(boolean offline) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountUpdateStatus(offline));
    }

    @Override
    public TLTheme accountUpdateTheme(String format, TLAbsInputTheme theme, String slug, String title, TLAbsInputDocument document, TLInputThemeSettings settings) throws RpcErrorException, IOException {
        return (TLTheme) executeRpcQuery(new TLRequestAccountUpdateTheme(format, theme, slug, title, document, settings));
    }

    @Override
    public TLAbsUser accountUpdateUsername(String username) throws RpcErrorException, IOException {
        return (TLAbsUser) executeRpcQuery(new TLRequestAccountUpdateUsername(username));
    }

    @Override
    public TLAbsDocument accountUploadTheme(TLAbsInputFile file, TLAbsInputFile thumb, String fileName, String mimeType) throws RpcErrorException, IOException {
        return (TLAbsDocument) executeRpcQuery(new TLRequestAccountUploadTheme(file, thumb, fileName, mimeType));
    }

    @Override
    public TLAbsWallPaper accountUploadWallPaper(TLAbsInputFile file, String mimeType, TLWallPaperSettings settings) throws RpcErrorException, IOException {
        return (TLAbsWallPaper) executeRpcQuery(new TLRequestAccountUploadWallPaper(file, mimeType, settings));
    }

    @Override
    public TLBool accountVerifyEmail(String email, String code) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountVerifyEmail(email, code));
    }

    @Override
    public TLBool accountVerifyPhone(String phoneNumber, String phoneCodeHash, String phoneCode) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAccountVerifyPhone(phoneNumber, phoneCodeHash, phoneCode));
    }

    @Override
    public TLAuthorization authAcceptLoginToken(TLBytes token) throws RpcErrorException, IOException {
        return (TLAuthorization) executeRpcQuery(new TLRequestAuthAcceptLoginToken(token));
    }

    @Override
    public TLBool authBindTempAuthKey(long permAuthKeyId, long nonce, int expiresAt, TLBytes encryptedMessage) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAuthBindTempAuthKey(permAuthKeyId, nonce, expiresAt, encryptedMessage));
    }

    @Override
    public TLBool authCancelCode(String phoneNumber, String phoneCodeHash) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAuthCancelCode(phoneNumber, phoneCodeHash));
    }

    @Override
    public TLAbsAuthorization authCheckPassword(TLAbsInputCheckPasswordSRP password) throws RpcErrorException, IOException {
        return (TLAbsAuthorization) executeRpcQuery(new TLRequestAuthCheckPassword(password));
    }

    @Override
    public TLBool authDropTempAuthKeys(TLLongVector exceptAuthKeys) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAuthDropTempAuthKeys(exceptAuthKeys));
    }

    @Override
    public TLExportedAuthorization authExportAuthorization(int dcId) throws RpcErrorException, IOException {
        return (TLExportedAuthorization) executeRpcQuery(new TLRequestAuthExportAuthorization(dcId));
    }

    @Override
    public TLAbsLoginToken authExportLoginToken(int apiId, String apiHash, TLIntVector exceptIds) throws RpcErrorException, IOException {
        return (TLAbsLoginToken) executeRpcQuery(new TLRequestAuthExportLoginToken(apiId, apiHash, exceptIds));
    }

    @Override
    public TLAbsAuthorization authImportAuthorization(int id, TLBytes bytes) throws RpcErrorException, IOException {
        return (TLAbsAuthorization) executeRpcQuery(new TLRequestAuthImportAuthorization(id, bytes));
    }

    @Override
    public TLAbsAuthorization authImportBotAuthorization(int flags, int apiId, String apiHash, String botAuthToken) throws RpcErrorException, IOException {
        return (TLAbsAuthorization) executeRpcQuery(new TLRequestAuthImportBotAuthorization(flags, apiId, apiHash, botAuthToken));
    }

    @Override
    public TLAbsLoginToken authImportLoginToken(TLBytes token) throws RpcErrorException, IOException {
        return (TLAbsLoginToken) executeRpcQuery(new TLRequestAuthImportLoginToken(token));
    }

    @Override
    public TLBool authLogOut() throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAuthLogOut());
    }

    @Override
    public TLAbsAuthorization authRecoverPassword(String code) throws RpcErrorException, IOException {
        return (TLAbsAuthorization) executeRpcQuery(new TLRequestAuthRecoverPassword(code));
    }

    @Override
    public TLPasswordRecovery authRequestPasswordRecovery() throws RpcErrorException, IOException {
        return (TLPasswordRecovery) executeRpcQuery(new TLRequestAuthRequestPasswordRecovery());
    }

    @Override
    public TLSentCode authResendCode(String phoneNumber, String phoneCodeHash) throws RpcErrorException, IOException {
        return (TLSentCode) executeRpcQuery(new TLRequestAuthResendCode(phoneNumber, phoneCodeHash));
    }

    @Override
    public TLBool authResetAuthorizations() throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestAuthResetAuthorizations());
    }

    @Override
    public TLSentCode authSendCode(String phoneNumber, int apiId, String apiHash, TLCodeSettings settings) throws RpcErrorException, IOException {
        return (TLSentCode) executeRpcQuery(new TLRequestAuthSendCode(phoneNumber, apiId, apiHash, settings));
    }

    @Override
    public TLAbsAuthorization authSignIn(String phoneNumber, String phoneCodeHash, String phoneCode) throws RpcErrorException, IOException {
        return (TLAbsAuthorization) executeRpcQuery(new TLRequestAuthSignIn(phoneNumber, phoneCodeHash, phoneCode));
    }

    @Override
    public TLAbsAuthorization authSignUp(String phoneNumber, String phoneCodeHash, String firstName, String lastName) throws RpcErrorException, IOException {
        return (TLAbsAuthorization) executeRpcQuery(new TLRequestAuthSignUp(phoneNumber, phoneCodeHash, firstName, lastName));
    }

    @Override
    public TLBool botsAnswerWebhookJSONQuery(long queryId, TLDataJSON data) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestBotsAnswerWebhookJSONQuery(queryId, data));
    }

    @Override
    public TLDataJSON botsSendCustomRequest(String customMethod, TLDataJSON params) throws RpcErrorException, IOException {
        return (TLDataJSON) executeRpcQuery(new TLRequestBotsSendCustomRequest(customMethod, params));
    }

    @Override
    public TLBool botsSetBotCommands(TLVector<TLBotCommand> commands) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestBotsSetBotCommands(commands));
    }

    @Override
    public TLBool channelsCheckUsername(TLAbsInputChannel channel, String username) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestChannelsCheckUsername(channel, username));
    }

    @Override
    public TLAbsUpdates channelsCreateChannel(boolean broadcast, boolean megagroup, String title, String about, TLAbsInputGeoPoint geoPoint, String address) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestChannelsCreateChannel(broadcast, megagroup, title, about, geoPoint, address));
    }

    @Override
    public TLAbsUpdates channelsDeleteChannel(TLAbsInputChannel channel) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestChannelsDeleteChannel(channel));
    }

    @Override
    public TLBool channelsDeleteHistory(TLAbsInputChannel channel, int maxId) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestChannelsDeleteHistory(channel, maxId));
    }

    @Override
    public TLAffectedMessages channelsDeleteMessages(TLAbsInputChannel channel, TLIntVector id) throws RpcErrorException, IOException {
        return (TLAffectedMessages) executeRpcQuery(new TLRequestChannelsDeleteMessages(channel, id));
    }

    @Override
    public TLAffectedHistory channelsDeleteUserHistory(TLAbsInputChannel channel, TLAbsInputUser userId) throws RpcErrorException, IOException {
        return (TLAffectedHistory) executeRpcQuery(new TLRequestChannelsDeleteUserHistory(channel, userId));
    }

    @Override
    public TLAbsUpdates channelsEditAdmin(TLAbsInputChannel channel, TLAbsInputUser userId, TLChatAdminRights adminRights, String rank) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestChannelsEditAdmin(channel, userId, adminRights, rank));
    }

    @Override
    public TLAbsUpdates channelsEditBanned(TLAbsInputChannel channel, TLAbsInputUser userId, TLChatBannedRights bannedRights) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestChannelsEditBanned(channel, userId, bannedRights));
    }

    @Override
    public TLAbsUpdates channelsEditCreator(TLAbsInputChannel channel, TLAbsInputUser userId, TLAbsInputCheckPasswordSRP password) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestChannelsEditCreator(channel, userId, password));
    }

    @Override
    public TLBool channelsEditLocation(TLAbsInputChannel channel, TLAbsInputGeoPoint geoPoint, String address) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestChannelsEditLocation(channel, geoPoint, address));
    }

    @Override
    public TLAbsUpdates channelsEditPhoto(TLAbsInputChannel channel, TLAbsInputChatPhoto photo) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestChannelsEditPhoto(channel, photo));
    }

    @Override
    public TLAbsUpdates channelsEditTitle(TLAbsInputChannel channel, String title) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestChannelsEditTitle(channel, title));
    }

    @Override
    public TLExportedMessageLink channelsExportMessageLink(TLAbsInputChannel channel, int id, boolean grouped) throws RpcErrorException, IOException {
        return (TLExportedMessageLink) executeRpcQuery(new TLRequestChannelsExportMessageLink(channel, id, grouped));
    }

    @Override
    public TLAdminLogResults channelsGetAdminLog(TLAbsInputChannel channel, String q, TLChannelAdminLogEventsFilter eventsFilter, TLVector<TLAbsInputUser> admins, long maxId, long minId, int limit) throws RpcErrorException, IOException {
        return (TLAdminLogResults) executeRpcQuery(new TLRequestChannelsGetAdminLog(channel, q, eventsFilter, admins, maxId, minId, limit));
    }

    @Override
    public TLAbsChats channelsGetAdminedPublicChannels(boolean byLocation, boolean checkLimit) throws RpcErrorException, IOException {
        return (TLAbsChats) executeRpcQuery(new TLRequestChannelsGetAdminedPublicChannels(byLocation, checkLimit));
    }

    @Override
    public TLAbsChats channelsGetChannels(TLVector<TLAbsInputChannel> id) throws RpcErrorException, IOException {
        return (TLAbsChats) executeRpcQuery(new TLRequestChannelsGetChannels(id));
    }

    @Override
    public TLChatFull channelsGetFullChannel(TLAbsInputChannel channel) throws RpcErrorException, IOException {
        return (TLChatFull) executeRpcQuery(new TLRequestChannelsGetFullChannel(channel));
    }

    @Override
    public TLAbsChats channelsGetGroupsForDiscussion() throws RpcErrorException, IOException {
        return (TLAbsChats) executeRpcQuery(new TLRequestChannelsGetGroupsForDiscussion());
    }

    @Override
    public TLInactiveChats channelsGetInactiveChannels() throws RpcErrorException, IOException {
        return (TLInactiveChats) executeRpcQuery(new TLRequestChannelsGetInactiveChannels());
    }

    @Override
    public TLAbsChats channelsGetLeftChannels(int offset) throws RpcErrorException, IOException {
        return (TLAbsChats) executeRpcQuery(new TLRequestChannelsGetLeftChannels(offset));
    }

    @Override
    public TLAbsMessages channelsGetMessages(TLAbsInputChannel channel, TLVector<TLAbsInputMessage> id) throws RpcErrorException, IOException {
        return (TLAbsMessages) executeRpcQuery(new TLRequestChannelsGetMessages(channel, id));
    }

    @Override
    public TLChannelParticipant channelsGetParticipant(TLAbsInputChannel channel, TLAbsInputUser userId) throws RpcErrorException, IOException {
        return (TLChannelParticipant) executeRpcQuery(new TLRequestChannelsGetParticipant(channel, userId));
    }

    @Override
    public TLAbsChannelParticipants channelsGetParticipants(TLAbsInputChannel channel, TLAbsChannelParticipantsFilter filter, int offset, int limit, int hash) throws RpcErrorException, IOException {
        return (TLAbsChannelParticipants) executeRpcQuery(new TLRequestChannelsGetParticipants(channel, filter, offset, limit, hash));
    }

    @Override
    public TLAbsUpdates channelsInviteToChannel(TLAbsInputChannel channel, TLVector<TLAbsInputUser> users) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestChannelsInviteToChannel(channel, users));
    }

    @Override
    public TLAbsUpdates channelsJoinChannel(TLAbsInputChannel channel) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestChannelsJoinChannel(channel));
    }

    @Override
    public TLAbsUpdates channelsLeaveChannel(TLAbsInputChannel channel) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestChannelsLeaveChannel(channel));
    }

    @Override
    public TLBool channelsReadHistory(TLAbsInputChannel channel, int maxId) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestChannelsReadHistory(channel, maxId));
    }

    @Override
    public TLBool channelsReadMessageContents(TLAbsInputChannel channel, TLIntVector id) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestChannelsReadMessageContents(channel, id));
    }

    @Override
    public TLBool channelsReportSpam(TLAbsInputChannel channel, TLAbsInputUser userId, TLIntVector id) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestChannelsReportSpam(channel, userId, id));
    }

    @Override
    public TLBool channelsSetDiscussionGroup(TLAbsInputChannel broadcast, TLAbsInputChannel group) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestChannelsSetDiscussionGroup(broadcast, group));
    }

    @Override
    public TLBool channelsSetStickers(TLAbsInputChannel channel, TLAbsInputStickerSet stickerset) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestChannelsSetStickers(channel, stickerset));
    }

    @Override
    public TLAbsUpdates channelsTogglePreHistoryHidden(TLAbsInputChannel channel, boolean enabled) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestChannelsTogglePreHistoryHidden(channel, enabled));
    }

    @Override
    public TLAbsUpdates channelsToggleSignatures(TLAbsInputChannel channel, boolean enabled) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestChannelsToggleSignatures(channel, enabled));
    }

    @Override
    public TLAbsUpdates channelsToggleSlowMode(TLAbsInputChannel channel, int seconds) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestChannelsToggleSlowMode(channel, seconds));
    }

    @Override
    public TLBool channelsUpdateUsername(TLAbsInputChannel channel, String username) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestChannelsUpdateUsername(channel, username));
    }

    @Override
    public TLAbsUpdates contactsAcceptContact(TLAbsInputUser id) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestContactsAcceptContact(id));
    }

    @Override
    public TLAbsUpdates contactsAddContact(boolean addPhonePrivacyException, TLAbsInputUser id, String firstName, String lastName, String phone) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestContactsAddContact(addPhonePrivacyException, id, firstName, lastName, phone));
    }

    @Override
    public TLBool contactsBlock(TLAbsInputUser id) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestContactsBlock(id));
    }

    @Override
    public TLBool contactsDeleteByPhones(TLStringVector phones) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestContactsDeleteByPhones(phones));
    }

    @Override
    public TLAbsUpdates contactsDeleteContacts(TLVector<TLAbsInputUser> id) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestContactsDeleteContacts(id));
    }

    @Override
    public TLAbsBlocked contactsGetBlocked(int offset, int limit) throws RpcErrorException, IOException {
        return (TLAbsBlocked) executeRpcQuery(new TLRequestContactsGetBlocked(offset, limit));
    }

    @Override
    public TLIntVector contactsGetContactIDs(int hash) throws RpcErrorException, IOException {
        return (TLIntVector) executeRpcQuery(new TLRequestContactsGetContactIDs(hash));
    }

    @Override
    public TLAbsContacts contactsGetContacts(int hash) throws RpcErrorException, IOException {
        return (TLAbsContacts) executeRpcQuery(new TLRequestContactsGetContacts(hash));
    }

    @Override
    public TLAbsUpdates contactsGetLocated(boolean background, TLAbsInputGeoPoint geoPoint, Integer selfExpires) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestContactsGetLocated(background, geoPoint, selfExpires));
    }

    @Override
    public TLVector<TLSavedPhoneContact> contactsGetSaved() throws RpcErrorException, IOException {
        return (TLVector<TLSavedPhoneContact>) executeRpcQuery(new TLRequestContactsGetSaved());
    }

    @Override
    public TLVector<TLContactStatus> contactsGetStatuses() throws RpcErrorException, IOException {
        return (TLVector<TLContactStatus>) executeRpcQuery(new TLRequestContactsGetStatuses());
    }

    @Override
    public TLAbsTopPeers contactsGetTopPeers(boolean correspondents, boolean botsPm, boolean botsInline, boolean phoneCalls, boolean forwardUsers, boolean forwardChats, boolean groups, boolean channels, int offset, int limit, int hash) throws RpcErrorException, IOException {
        return (TLAbsTopPeers) executeRpcQuery(new TLRequestContactsGetTopPeers(correspondents, botsPm, botsInline, phoneCalls, forwardUsers, forwardChats, groups, channels, offset, limit, hash));
    }

    @Override
    public TLImportedContacts contactsImportContacts(TLVector<TLInputPhoneContact> contacts) throws RpcErrorException, IOException {
        return (TLImportedContacts) executeRpcQuery(new TLRequestContactsImportContacts(contacts));
    }

    @Override
    public TLBool contactsResetSaved() throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestContactsResetSaved());
    }

    @Override
    public TLBool contactsResetTopPeerRating(TLAbsTopPeerCategory category, TLAbsInputPeer peer) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestContactsResetTopPeerRating(category, peer));
    }

    @Override
    public TLResolvedPeer contactsResolveUsername(String username) throws RpcErrorException, IOException {
        return (TLResolvedPeer) executeRpcQuery(new TLRequestContactsResolveUsername(username));
    }

    @Override
    public TLFound contactsSearch(String q, int limit) throws RpcErrorException, IOException {
        return (TLFound) executeRpcQuery(new TLRequestContactsSearch(q, limit));
    }

    @Override
    public TLBool contactsToggleTopPeers(boolean enabled) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestContactsToggleTopPeers(enabled));
    }

    @Override
    public TLBool contactsUnblock(TLAbsInputUser id) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestContactsUnblock(id));
    }

    @Override
    public TLAbsUpdates foldersDeleteFolder(int folderId) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestFoldersDeleteFolder(folderId));
    }

    @Override
    public TLAbsUpdates foldersEditPeerFolders(TLVector<TLInputFolderPeer> folderPeers) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestFoldersEditPeerFolders(folderPeers));
    }

    @Override
    public TLBool helpAcceptTermsOfService(TLDataJSON id) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestHelpAcceptTermsOfService(id));
    }

    @Override
    public TLAbsUserInfo helpEditUserInfo(TLAbsInputUser userId, String message, TLVector<TLAbsMessageEntity> entities) throws RpcErrorException, IOException {
        return (TLAbsUserInfo) executeRpcQuery(new TLRequestHelpEditUserInfo(userId, message, entities));
    }

    @Override
    public TLAbsUpdates helpGetAppChangelog(String prevAppVersion) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestHelpGetAppChangelog(prevAppVersion));
    }

    @Override
    public TLAbsJSONValue helpGetAppConfig() throws RpcErrorException, IOException {
        return (TLAbsJSONValue) executeRpcQuery(new TLRequestHelpGetAppConfig());
    }

    @Override
    public TLAbsAppUpdate helpGetAppUpdate(String source) throws RpcErrorException, IOException {
        return (TLAbsAppUpdate) executeRpcQuery(new TLRequestHelpGetAppUpdate(source));
    }

    @Override
    public TLCdnConfig helpGetCdnConfig() throws RpcErrorException, IOException {
        return (TLCdnConfig) executeRpcQuery(new TLRequestHelpGetCdnConfig());
    }

    @Override
    public TLConfig helpGetConfig() throws RpcErrorException, IOException {
        return (TLConfig) executeRpcQuery(new TLRequestHelpGetConfig());
    }

    @Override
    public TLAbsDeepLinkInfo helpGetDeepLinkInfo(String path) throws RpcErrorException, IOException {
        return (TLAbsDeepLinkInfo) executeRpcQuery(new TLRequestHelpGetDeepLinkInfo(path));
    }

    @Override
    public TLInviteText helpGetInviteText() throws RpcErrorException, IOException {
        return (TLInviteText) executeRpcQuery(new TLRequestHelpGetInviteText());
    }

    @Override
    public TLNearestDc helpGetNearestDc() throws RpcErrorException, IOException {
        return (TLNearestDc) executeRpcQuery(new TLRequestHelpGetNearestDc());
    }

    @Override
    public TLAbsPassportConfig helpGetPassportConfig(int hash) throws RpcErrorException, IOException {
        return (TLAbsPassportConfig) executeRpcQuery(new TLRequestHelpGetPassportConfig(hash));
    }

    @Override
    public TLAbsPromoData helpGetPromoData() throws RpcErrorException, IOException {
        return (TLAbsPromoData) executeRpcQuery(new TLRequestHelpGetPromoData());
    }

    @Override
    public TLRecentMeUrls helpGetRecentMeUrls(String referer) throws RpcErrorException, IOException {
        return (TLRecentMeUrls) executeRpcQuery(new TLRequestHelpGetRecentMeUrls(referer));
    }

    @Override
    public TLSupport helpGetSupport() throws RpcErrorException, IOException {
        return (TLSupport) executeRpcQuery(new TLRequestHelpGetSupport());
    }

    @Override
    public TLSupportName helpGetSupportName() throws RpcErrorException, IOException {
        return (TLSupportName) executeRpcQuery(new TLRequestHelpGetSupportName());
    }

    @Override
    public TLAbsTermsOfServiceUpdate helpGetTermsOfServiceUpdate() throws RpcErrorException, IOException {
        return (TLAbsTermsOfServiceUpdate) executeRpcQuery(new TLRequestHelpGetTermsOfServiceUpdate());
    }

    @Override
    public TLAbsUserInfo helpGetUserInfo(TLAbsInputUser userId) throws RpcErrorException, IOException {
        return (TLAbsUserInfo) executeRpcQuery(new TLRequestHelpGetUserInfo(userId));
    }

    @Override
    public TLBool helpHidePromoData(TLAbsInputPeer peer) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestHelpHidePromoData(peer));
    }

    @Override
    public TLBool helpSaveAppLog(TLVector<TLInputAppEvent> events) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestHelpSaveAppLog(events));
    }

    @Override
    public TLBool helpSetBotUpdatesStatus(int pendingUpdatesCount, String message) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestHelpSetBotUpdatesStatus(pendingUpdatesCount, message));
    }

    @Override
    public <T extends TLObject> T initConnection(int apiId, String deviceModel, String systemVersion, String appVersion, String systemLangCode, String langPack, String langCode, TLInputClientProxy proxy, TLAbsJSONValue params, TLMethod<T> query) throws RpcErrorException, IOException {
        return (T) executeRpcQuery(new TLRequestInitConnection(apiId, deviceModel, systemVersion, appVersion, systemLangCode, langPack, langCode, proxy, params, query));
    }

    @Override
    public <T extends TLObject> T invokeAfterMsg(long msgId, TLMethod<T> query) throws RpcErrorException, IOException {
        return (T) executeRpcQuery(new TLRequestInvokeAfterMsg(msgId, query));
    }

    @Override
    public <T extends TLObject> T invokeAfterMsgs(TLLongVector msgIds, TLMethod<T> query) throws RpcErrorException, IOException {
        return (T) executeRpcQuery(new TLRequestInvokeAfterMsgs(msgIds, query));
    }

    @Override
    public <T extends TLObject> T invokeWithLayer(int layer, TLMethod<T> query) throws RpcErrorException, IOException {
        return (T) executeRpcQuery(new TLRequestInvokeWithLayer(layer, query));
    }

    @Override
    public <T extends TLObject> T invokeWithMessagesRange(TLMessageRange range, TLMethod<T> query) throws RpcErrorException, IOException {
        return (T) executeRpcQuery(new TLRequestInvokeWithMessagesRange(range, query));
    }

    @Override
    public <T extends TLObject> T invokeWithTakeout(long takeoutId, TLMethod<T> query) throws RpcErrorException, IOException {
        return (T) executeRpcQuery(new TLRequestInvokeWithTakeout(takeoutId, query));
    }

    @Override
    public <T extends TLObject> T invokeWithoutUpdates(TLMethod<T> query) throws RpcErrorException, IOException {
        return (T) executeRpcQuery(new TLRequestInvokeWithoutUpdates(query));
    }

    @Override
    public TLLangPackDifference langpackGetDifference(String langPack, String langCode, int fromVersion) throws RpcErrorException, IOException {
        return (TLLangPackDifference) executeRpcQuery(new TLRequestLangpackGetDifference(langPack, langCode, fromVersion));
    }

    @Override
    public TLLangPackDifference langpackGetLangPack(String langPack, String langCode) throws RpcErrorException, IOException {
        return (TLLangPackDifference) executeRpcQuery(new TLRequestLangpackGetLangPack(langPack, langCode));
    }

    @Override
    public TLLangPackLanguage langpackGetLanguage(String langPack, String langCode) throws RpcErrorException, IOException {
        return (TLLangPackLanguage) executeRpcQuery(new TLRequestLangpackGetLanguage(langPack, langCode));
    }

    @Override
    public TLVector<TLLangPackLanguage> langpackGetLanguages(String langPack) throws RpcErrorException, IOException {
        return (TLVector<TLLangPackLanguage>) executeRpcQuery(new TLRequestLangpackGetLanguages(langPack));
    }

    @Override
    public TLVector<TLAbsLangPackString> langpackGetStrings(String langPack, String langCode, TLStringVector keys) throws RpcErrorException, IOException {
        return (TLVector<TLAbsLangPackString>) executeRpcQuery(new TLRequestLangpackGetStrings(langPack, langCode, keys));
    }

    @Override
    public TLAbsEncryptedChat messagesAcceptEncryption(TLInputEncryptedChat peer, TLBytes gB, long keyFingerprint) throws RpcErrorException, IOException {
        return (TLAbsEncryptedChat) executeRpcQuery(new TLRequestMessagesAcceptEncryption(peer, gB, keyFingerprint));
    }

    @Override
    public TLAbsUrlAuthResult messagesAcceptUrlAuth(boolean writeAllowed, TLAbsInputPeer peer, int msgId, int buttonId) throws RpcErrorException, IOException {
        return (TLAbsUrlAuthResult) executeRpcQuery(new TLRequestMessagesAcceptUrlAuth(writeAllowed, peer, msgId, buttonId));
    }

    @Override
    public TLAbsUpdates messagesAddChatUser(int chatId, TLAbsInputUser userId, int fwdLimit) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestMessagesAddChatUser(chatId, userId, fwdLimit));
    }

    @Override
    public TLAbsChatInvite messagesCheckChatInvite(String hash) throws RpcErrorException, IOException {
        return (TLAbsChatInvite) executeRpcQuery(new TLRequestMessagesCheckChatInvite(hash));
    }

    @Override
    public TLBool messagesClearAllDrafts() throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesClearAllDrafts());
    }

    @Override
    public TLBool messagesClearRecentStickers(boolean attached) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesClearRecentStickers(attached));
    }

    @Override
    public TLAbsUpdates messagesCreateChat(TLVector<TLAbsInputUser> users, String title) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestMessagesCreateChat(users, title));
    }

    @Override
    public TLAbsUpdates messagesDeleteChatUser(int chatId, TLAbsInputUser userId) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestMessagesDeleteChatUser(chatId, userId));
    }

    @Override
    public TLAffectedHistory messagesDeleteHistory(boolean justClear, boolean revoke, TLAbsInputPeer peer, int maxId) throws RpcErrorException, IOException {
        return (TLAffectedHistory) executeRpcQuery(new TLRequestMessagesDeleteHistory(justClear, revoke, peer, maxId));
    }

    @Override
    public TLAffectedMessages messagesDeleteMessages(boolean revoke, TLIntVector id) throws RpcErrorException, IOException {
        return (TLAffectedMessages) executeRpcQuery(new TLRequestMessagesDeleteMessages(revoke, id));
    }

    @Override
    public TLAbsUpdates messagesDeleteScheduledMessages(TLAbsInputPeer peer, TLIntVector id) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestMessagesDeleteScheduledMessages(peer, id));
    }

    @Override
    public TLBool messagesDiscardEncryption(int chatId) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesDiscardEncryption(chatId));
    }

    @Override
    public TLBool messagesEditChatAbout(TLAbsInputPeer peer, String about) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesEditChatAbout(peer, about));
    }

    @Override
    public TLBool messagesEditChatAdmin(int chatId, TLAbsInputUser userId, boolean isAdmin) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesEditChatAdmin(chatId, userId, isAdmin));
    }

    @Override
    public TLAbsUpdates messagesEditChatDefaultBannedRights(TLAbsInputPeer peer, TLChatBannedRights bannedRights) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestMessagesEditChatDefaultBannedRights(peer, bannedRights));
    }

    @Override
    public TLAbsUpdates messagesEditChatPhoto(int chatId, TLAbsInputChatPhoto photo) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestMessagesEditChatPhoto(chatId, photo));
    }

    @Override
    public TLAbsUpdates messagesEditChatTitle(int chatId, String title) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestMessagesEditChatTitle(chatId, title));
    }

    @Override
    public TLBool messagesEditInlineBotMessage(boolean noWebpage, TLInputBotInlineMessageID id, String message, TLAbsInputMedia media, TLAbsReplyMarkup replyMarkup, TLVector<TLAbsMessageEntity> entities) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesEditInlineBotMessage(noWebpage, id, message, media, replyMarkup, entities));
    }

    @Override
    public TLAbsUpdates messagesEditMessage(boolean noWebpage, TLAbsInputPeer peer, int id, String message, TLAbsInputMedia media, TLAbsReplyMarkup replyMarkup, TLVector<TLAbsMessageEntity> entities, Integer scheduleDate) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestMessagesEditMessage(noWebpage, peer, id, message, media, replyMarkup, entities, scheduleDate));
    }

    @Override
    public TLAbsExportedChatInvite messagesExportChatInvite(TLAbsInputPeer peer) throws RpcErrorException, IOException {
        return (TLAbsExportedChatInvite) executeRpcQuery(new TLRequestMessagesExportChatInvite(peer));
    }

    @Override
    public TLBool messagesFaveSticker(TLAbsInputDocument id, boolean unfave) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesFaveSticker(id, unfave));
    }

    @Override
    public TLAbsUpdates messagesForwardMessages(boolean silent, boolean background, boolean withMyScore, TLAbsInputPeer fromPeer, TLIntVector id, TLLongVector randomId, TLAbsInputPeer toPeer, Integer scheduleDate) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestMessagesForwardMessages(silent, background, withMyScore, fromPeer, id, randomId, toPeer, scheduleDate));
    }

    @Override
    public TLAbsChats messagesGetAllChats(TLIntVector exceptIds) throws RpcErrorException, IOException {
        return (TLAbsChats) executeRpcQuery(new TLRequestMessagesGetAllChats(exceptIds));
    }

    @Override
    public TLAbsUpdates messagesGetAllDrafts() throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestMessagesGetAllDrafts());
    }

    @Override
    public TLAbsAllStickers messagesGetAllStickers(int hash) throws RpcErrorException, IOException {
        return (TLAbsAllStickers) executeRpcQuery(new TLRequestMessagesGetAllStickers(hash));
    }

    @Override
    public TLArchivedStickers messagesGetArchivedStickers(boolean masks, long offsetId, int limit) throws RpcErrorException, IOException {
        return (TLArchivedStickers) executeRpcQuery(new TLRequestMessagesGetArchivedStickers(masks, offsetId, limit));
    }

    @Override
    public TLVector<TLAbsStickerSetCovered> messagesGetAttachedStickers(TLAbsInputStickeredMedia media) throws RpcErrorException, IOException {
        return (TLVector<TLAbsStickerSetCovered>) executeRpcQuery(new TLRequestMessagesGetAttachedStickers(media));
    }

    @Override
    public TLBotCallbackAnswer messagesGetBotCallbackAnswer(boolean game, TLAbsInputPeer peer, int msgId, TLBytes data) throws RpcErrorException, IOException {
        return (TLBotCallbackAnswer) executeRpcQuery(new TLRequestMessagesGetBotCallbackAnswer(game, peer, msgId, data));
    }

    @Override
    public TLAbsChats messagesGetChats(TLIntVector id) throws RpcErrorException, IOException {
        return (TLAbsChats) executeRpcQuery(new TLRequestMessagesGetChats(id));
    }

    @Override
    public TLAbsChats messagesGetCommonChats(TLAbsInputUser userId, int maxId, int limit) throws RpcErrorException, IOException {
        return (TLAbsChats) executeRpcQuery(new TLRequestMessagesGetCommonChats(userId, maxId, limit));
    }

    @Override
    public TLAbsDhConfig messagesGetDhConfig(int version, int randomLength) throws RpcErrorException, IOException {
        return (TLAbsDhConfig) executeRpcQuery(new TLRequestMessagesGetDhConfig(version, randomLength));
    }

    @Override
    public TLVector<TLDialogFilter> messagesGetDialogFilters() throws RpcErrorException, IOException {
        return (TLVector<TLDialogFilter>) executeRpcQuery(new TLRequestMessagesGetDialogFilters());
    }

    @Override
    public TLVector<TLAbsDialogPeer> messagesGetDialogUnreadMarks() throws RpcErrorException, IOException {
        return (TLVector<TLAbsDialogPeer>) executeRpcQuery(new TLRequestMessagesGetDialogUnreadMarks());
    }

    @Override
    public TLAbsDialogs messagesGetDialogs(boolean excludePinned, Integer folderId, int offsetDate, int offsetId, TLAbsInputPeer offsetPeer, int limit, int hash) throws RpcErrorException, IOException {
        return (TLAbsDialogs) executeRpcQuery(new TLRequestMessagesGetDialogs(excludePinned, folderId, offsetDate, offsetId, offsetPeer, limit, hash));
    }

    @Override
    public TLAbsDocument messagesGetDocumentByHash(TLBytes sha256, int size, String mimeType) throws RpcErrorException, IOException {
        return (TLAbsDocument) executeRpcQuery(new TLRequestMessagesGetDocumentByHash(sha256, size, mimeType));
    }

    @Override
    public TLEmojiKeywordsDifference messagesGetEmojiKeywords(String langCode) throws RpcErrorException, IOException {
        return (TLEmojiKeywordsDifference) executeRpcQuery(new TLRequestMessagesGetEmojiKeywords(langCode));
    }

    @Override
    public TLEmojiKeywordsDifference messagesGetEmojiKeywordsDifference(String langCode, int fromVersion) throws RpcErrorException, IOException {
        return (TLEmojiKeywordsDifference) executeRpcQuery(new TLRequestMessagesGetEmojiKeywordsDifference(langCode, fromVersion));
    }

    @Override
    public TLVector<TLEmojiLanguage> messagesGetEmojiKeywordsLanguages(TLStringVector langCodes) throws RpcErrorException, IOException {
        return (TLVector<TLEmojiLanguage>) executeRpcQuery(new TLRequestMessagesGetEmojiKeywordsLanguages(langCodes));
    }

    @Override
    public TLEmojiURL messagesGetEmojiURL(String langCode) throws RpcErrorException, IOException {
        return (TLEmojiURL) executeRpcQuery(new TLRequestMessagesGetEmojiURL(langCode));
    }

    @Override
    public TLAbsFavedStickers messagesGetFavedStickers(int hash) throws RpcErrorException, IOException {
        return (TLAbsFavedStickers) executeRpcQuery(new TLRequestMessagesGetFavedStickers(hash));
    }

    @Override
    public TLAbsFeaturedStickers messagesGetFeaturedStickers(int hash) throws RpcErrorException, IOException {
        return (TLAbsFeaturedStickers) executeRpcQuery(new TLRequestMessagesGetFeaturedStickers(hash));
    }

    @Override
    public TLChatFull messagesGetFullChat(int chatId) throws RpcErrorException, IOException {
        return (TLChatFull) executeRpcQuery(new TLRequestMessagesGetFullChat(chatId));
    }

    @Override
    public TLHighScores messagesGetGameHighScores(TLAbsInputPeer peer, int id, TLAbsInputUser userId) throws RpcErrorException, IOException {
        return (TLHighScores) executeRpcQuery(new TLRequestMessagesGetGameHighScores(peer, id, userId));
    }

    @Override
    public TLAbsMessages messagesGetHistory(TLAbsInputPeer peer, int offsetId, int offsetDate, int addOffset, int limit, int maxId, int minId, int hash) throws RpcErrorException, IOException {
        return (TLAbsMessages) executeRpcQuery(new TLRequestMessagesGetHistory(peer, offsetId, offsetDate, addOffset, limit, maxId, minId, hash));
    }

    @Override
    public TLBotResults messagesGetInlineBotResults(TLAbsInputUser bot, TLAbsInputPeer peer, TLAbsInputGeoPoint geoPoint, String query, String offset) throws RpcErrorException, IOException {
        return (TLBotResults) executeRpcQuery(new TLRequestMessagesGetInlineBotResults(bot, peer, geoPoint, query, offset));
    }

    @Override
    public TLHighScores messagesGetInlineGameHighScores(TLInputBotInlineMessageID id, TLAbsInputUser userId) throws RpcErrorException, IOException {
        return (TLHighScores) executeRpcQuery(new TLRequestMessagesGetInlineGameHighScores(id, userId));
    }

    @Override
    public TLAbsAllStickers messagesGetMaskStickers(int hash) throws RpcErrorException, IOException {
        return (TLAbsAllStickers) executeRpcQuery(new TLRequestMessagesGetMaskStickers(hash));
    }

    @Override
    public TLMessageEditData messagesGetMessageEditData(TLAbsInputPeer peer, int id) throws RpcErrorException, IOException {
        return (TLMessageEditData) executeRpcQuery(new TLRequestMessagesGetMessageEditData(peer, id));
    }

    @Override
    public TLAbsMessages messagesGetMessages(TLVector<TLAbsInputMessage> id) throws RpcErrorException, IOException {
        return (TLAbsMessages) executeRpcQuery(new TLRequestMessagesGetMessages(id));
    }

    @Override
    public TLIntVector messagesGetMessagesViews(TLAbsInputPeer peer, TLIntVector id, boolean increment) throws RpcErrorException, IOException {
        return (TLIntVector) executeRpcQuery(new TLRequestMessagesGetMessagesViews(peer, id, increment));
    }

    @Override
    public TLAbsFeaturedStickers messagesGetOldFeaturedStickers(int offset, int limit, int hash) throws RpcErrorException, IOException {
        return (TLAbsFeaturedStickers) executeRpcQuery(new TLRequestMessagesGetOldFeaturedStickers(offset, limit, hash));
    }

    @Override
    public TLChatOnlines messagesGetOnlines(TLAbsInputPeer peer) throws RpcErrorException, IOException {
        return (TLChatOnlines) executeRpcQuery(new TLRequestMessagesGetOnlines(peer));
    }

    @Override
    public TLPeerDialogs messagesGetPeerDialogs(TLVector<TLAbsInputDialogPeer> peers) throws RpcErrorException, IOException {
        return (TLPeerDialogs) executeRpcQuery(new TLRequestMessagesGetPeerDialogs(peers));
    }

    @Override
    public TLPeerSettings messagesGetPeerSettings(TLAbsInputPeer peer) throws RpcErrorException, IOException {
        return (TLPeerSettings) executeRpcQuery(new TLRequestMessagesGetPeerSettings(peer));
    }

    @Override
    public TLPeerDialogs messagesGetPinnedDialogs(int folderId) throws RpcErrorException, IOException {
        return (TLPeerDialogs) executeRpcQuery(new TLRequestMessagesGetPinnedDialogs(folderId));
    }

    @Override
    public TLAbsUpdates messagesGetPollResults(TLAbsInputPeer peer, int msgId) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestMessagesGetPollResults(peer, msgId));
    }

    @Override
    public TLVotesList messagesGetPollVotes(TLAbsInputPeer peer, int id, TLBytes option, String offset, int limit) throws RpcErrorException, IOException {
        return (TLVotesList) executeRpcQuery(new TLRequestMessagesGetPollVotes(peer, id, option, offset, limit));
    }

    @Override
    public TLAbsMessages messagesGetRecentLocations(TLAbsInputPeer peer, int limit, int hash) throws RpcErrorException, IOException {
        return (TLAbsMessages) executeRpcQuery(new TLRequestMessagesGetRecentLocations(peer, limit, hash));
    }

    @Override
    public TLAbsRecentStickers messagesGetRecentStickers(boolean attached, int hash) throws RpcErrorException, IOException {
        return (TLAbsRecentStickers) executeRpcQuery(new TLRequestMessagesGetRecentStickers(attached, hash));
    }

    @Override
    public TLAbsSavedGifs messagesGetSavedGifs(int hash) throws RpcErrorException, IOException {
        return (TLAbsSavedGifs) executeRpcQuery(new TLRequestMessagesGetSavedGifs(hash));
    }

    @Override
    public TLAbsMessages messagesGetScheduledHistory(TLAbsInputPeer peer, int hash) throws RpcErrorException, IOException {
        return (TLAbsMessages) executeRpcQuery(new TLRequestMessagesGetScheduledHistory(peer, hash));
    }

    @Override
    public TLAbsMessages messagesGetScheduledMessages(TLAbsInputPeer peer, TLIntVector id) throws RpcErrorException, IOException {
        return (TLAbsMessages) executeRpcQuery(new TLRequestMessagesGetScheduledMessages(peer, id));
    }

    @Override
    public TLVector<TLSearchCounter> messagesGetSearchCounters(TLAbsInputPeer peer, TLVector<TLAbsMessagesFilter> filters) throws RpcErrorException, IOException {
        return (TLVector<TLSearchCounter>) executeRpcQuery(new TLRequestMessagesGetSearchCounters(peer, filters));
    }

    @Override
    public TLVector<TLMessageRange> messagesGetSplitRanges() throws RpcErrorException, IOException {
        return (TLVector<TLMessageRange>) executeRpcQuery(new TLRequestMessagesGetSplitRanges());
    }

    @Override
    public TLStatsURL messagesGetStatsURL(boolean dark, TLAbsInputPeer peer, String params) throws RpcErrorException, IOException {
        return (TLStatsURL) executeRpcQuery(new TLRequestMessagesGetStatsURL(dark, peer, params));
    }

    @Override
    public TLStickerSet messagesGetStickerSet(TLAbsInputStickerSet stickerset) throws RpcErrorException, IOException {
        return (TLStickerSet) executeRpcQuery(new TLRequestMessagesGetStickerSet(stickerset));
    }

    @Override
    public TLAbsStickers messagesGetStickers(String emoticon, int hash) throws RpcErrorException, IOException {
        return (TLAbsStickers) executeRpcQuery(new TLRequestMessagesGetStickers(emoticon, hash));
    }

    @Override
    public TLVector<TLDialogFilterSuggested> messagesGetSuggestedDialogFilters() throws RpcErrorException, IOException {
        return (TLVector<TLDialogFilterSuggested>) executeRpcQuery(new TLRequestMessagesGetSuggestedDialogFilters());
    }

    @Override
    public TLAbsMessages messagesGetUnreadMentions(TLAbsInputPeer peer, int offsetId, int addOffset, int limit, int maxId, int minId) throws RpcErrorException, IOException {
        return (TLAbsMessages) executeRpcQuery(new TLRequestMessagesGetUnreadMentions(peer, offsetId, addOffset, limit, maxId, minId));
    }

    @Override
    public TLAbsWebPage messagesGetWebPage(String url, int hash) throws RpcErrorException, IOException {
        return (TLAbsWebPage) executeRpcQuery(new TLRequestMessagesGetWebPage(url, hash));
    }

    @Override
    public TLAbsMessageMedia messagesGetWebPagePreview(String message, TLVector<TLAbsMessageEntity> entities) throws RpcErrorException, IOException {
        return (TLAbsMessageMedia) executeRpcQuery(new TLRequestMessagesGetWebPagePreview(message, entities));
    }

    @Override
    public TLBool messagesHidePeerSettingsBar(TLAbsInputPeer peer) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesHidePeerSettingsBar(peer));
    }

    @Override
    public TLAbsUpdates messagesImportChatInvite(String hash) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestMessagesImportChatInvite(hash));
    }

    @Override
    public TLAbsStickerSetInstallResult messagesInstallStickerSet(TLAbsInputStickerSet stickerset, boolean archived) throws RpcErrorException, IOException {
        return (TLAbsStickerSetInstallResult) executeRpcQuery(new TLRequestMessagesInstallStickerSet(stickerset, archived));
    }

    @Override
    public TLBool messagesMarkDialogUnread(boolean unread, TLAbsInputDialogPeer peer) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesMarkDialogUnread(unread, peer));
    }

    @Override
    public TLAbsUpdates messagesMigrateChat(int chatId) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestMessagesMigrateChat(chatId));
    }

    @Override
    public TLBool messagesReadEncryptedHistory(TLInputEncryptedChat peer, int maxDate) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesReadEncryptedHistory(peer, maxDate));
    }

    @Override
    public TLBool messagesReadFeaturedStickers(TLLongVector id) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesReadFeaturedStickers(id));
    }

    @Override
    public TLAffectedMessages messagesReadHistory(TLAbsInputPeer peer, int maxId) throws RpcErrorException, IOException {
        return (TLAffectedMessages) executeRpcQuery(new TLRequestMessagesReadHistory(peer, maxId));
    }

    @Override
    public TLAffectedHistory messagesReadMentions(TLAbsInputPeer peer) throws RpcErrorException, IOException {
        return (TLAffectedHistory) executeRpcQuery(new TLRequestMessagesReadMentions(peer));
    }

    @Override
    public TLAffectedMessages messagesReadMessageContents(TLIntVector id) throws RpcErrorException, IOException {
        return (TLAffectedMessages) executeRpcQuery(new TLRequestMessagesReadMessageContents(id));
    }

    @Override
    public TLVector<TLReceivedNotifyMessage> messagesReceivedMessages(int maxId) throws RpcErrorException, IOException {
        return (TLVector<TLReceivedNotifyMessage>) executeRpcQuery(new TLRequestMessagesReceivedMessages(maxId));
    }

    @Override
    public TLLongVector messagesReceivedQueue(int maxQts) throws RpcErrorException, IOException {
        return (TLLongVector) executeRpcQuery(new TLRequestMessagesReceivedQueue(maxQts));
    }

    @Override
    public TLBool messagesReorderPinnedDialogs(boolean force, int folderId, TLVector<TLAbsInputDialogPeer> order) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesReorderPinnedDialogs(force, folderId, order));
    }

    @Override
    public TLBool messagesReorderStickerSets(boolean masks, TLLongVector order) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesReorderStickerSets(masks, order));
    }

    @Override
    public TLBool messagesReport(TLAbsInputPeer peer, TLIntVector id, TLAbsReportReason reason) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesReport(peer, id, reason));
    }

    @Override
    public TLBool messagesReportEncryptedSpam(TLInputEncryptedChat peer) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesReportEncryptedSpam(peer));
    }

    @Override
    public TLBool messagesReportSpam(TLAbsInputPeer peer) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesReportSpam(peer));
    }

    @Override
    public TLAbsEncryptedChat messagesRequestEncryption(TLAbsInputUser userId, int randomId, TLBytes gA) throws RpcErrorException, IOException {
        return (TLAbsEncryptedChat) executeRpcQuery(new TLRequestMessagesRequestEncryption(userId, randomId, gA));
    }

    @Override
    public TLAbsUrlAuthResult messagesRequestUrlAuth(TLAbsInputPeer peer, int msgId, int buttonId) throws RpcErrorException, IOException {
        return (TLAbsUrlAuthResult) executeRpcQuery(new TLRequestMessagesRequestUrlAuth(peer, msgId, buttonId));
    }

    @Override
    public TLBool messagesSaveDraft(boolean noWebpage, Integer replyToMsgId, TLAbsInputPeer peer, String message, TLVector<TLAbsMessageEntity> entities) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesSaveDraft(noWebpage, replyToMsgId, peer, message, entities));
    }

    @Override
    public TLBool messagesSaveGif(TLAbsInputDocument id, boolean unsave) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesSaveGif(id, unsave));
    }

    @Override
    public TLBool messagesSaveRecentSticker(boolean attached, TLAbsInputDocument id, boolean unsave) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesSaveRecentSticker(attached, id, unsave));
    }

    @Override
    public TLAbsMessages messagesSearch(TLAbsInputPeer peer, String q, TLAbsInputUser fromId, TLAbsMessagesFilter filter, int minDate, int maxDate, int offsetId, int addOffset, int limit, int maxId, int minId, int hash) throws RpcErrorException, IOException {
        return (TLAbsMessages) executeRpcQuery(new TLRequestMessagesSearch(peer, q, fromId, filter, minDate, maxDate, offsetId, addOffset, limit, maxId, minId, hash));
    }

    @Override
    public TLFoundGifs messagesSearchGifs(String q, int offset) throws RpcErrorException, IOException {
        return (TLFoundGifs) executeRpcQuery(new TLRequestMessagesSearchGifs(q, offset));
    }

    @Override
    public TLAbsMessages messagesSearchGlobal(Integer folderId, String q, int offsetRate, TLAbsInputPeer offsetPeer, int offsetId, int limit) throws RpcErrorException, IOException {
        return (TLAbsMessages) executeRpcQuery(new TLRequestMessagesSearchGlobal(folderId, q, offsetRate, offsetPeer, offsetId, limit));
    }

    @Override
    public TLAbsFoundStickerSets messagesSearchStickerSets(boolean excludeFeatured, String q, int hash) throws RpcErrorException, IOException {
        return (TLAbsFoundStickerSets) executeRpcQuery(new TLRequestMessagesSearchStickerSets(excludeFeatured, q, hash));
    }

    @Override
    public TLAbsSentEncryptedMessage messagesSendEncrypted(TLInputEncryptedChat peer, long randomId, TLBytes data) throws RpcErrorException, IOException {
        return (TLAbsSentEncryptedMessage) executeRpcQuery(new TLRequestMessagesSendEncrypted(peer, randomId, data));
    }

    @Override
    public TLAbsSentEncryptedMessage messagesSendEncryptedFile(TLInputEncryptedChat peer, long randomId, TLBytes data, TLAbsInputEncryptedFile file) throws RpcErrorException, IOException {
        return (TLAbsSentEncryptedMessage) executeRpcQuery(new TLRequestMessagesSendEncryptedFile(peer, randomId, data, file));
    }

    @Override
    public TLAbsSentEncryptedMessage messagesSendEncryptedService(TLInputEncryptedChat peer, long randomId, TLBytes data) throws RpcErrorException, IOException {
        return (TLAbsSentEncryptedMessage) executeRpcQuery(new TLRequestMessagesSendEncryptedService(peer, randomId, data));
    }

    @Override
    public TLAbsUpdates messagesSendInlineBotResult(boolean silent, boolean background, boolean clearDraft, boolean hideVia, TLAbsInputPeer peer, Integer replyToMsgId, long randomId, long queryId, String id, Integer scheduleDate) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestMessagesSendInlineBotResult(silent, background, clearDraft, hideVia, peer, replyToMsgId, randomId, queryId, id, scheduleDate));
    }

    @Override
    public TLAbsUpdates messagesSendMedia(boolean silent, boolean background, boolean clearDraft, TLAbsInputPeer peer, Integer replyToMsgId, TLAbsInputMedia media, String message, long randomId, TLAbsReplyMarkup replyMarkup, TLVector<TLAbsMessageEntity> entities, Integer scheduleDate) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestMessagesSendMedia(silent, background, clearDraft, peer, replyToMsgId, media, message, randomId, replyMarkup, entities, scheduleDate));
    }

    @Override
    public TLAbsUpdates messagesSendMessage(boolean noWebpage, boolean silent, boolean background, boolean clearDraft, TLAbsInputPeer peer, Integer replyToMsgId, String message, long randomId, TLAbsReplyMarkup replyMarkup, TLVector<TLAbsMessageEntity> entities, Integer scheduleDate) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestMessagesSendMessage(noWebpage, silent, background, clearDraft, peer, replyToMsgId, message, randomId, replyMarkup, entities, scheduleDate));
    }

    @Override
    public TLAbsUpdates messagesSendMultiMedia(boolean silent, boolean background, boolean clearDraft, TLAbsInputPeer peer, Integer replyToMsgId, TLVector<TLInputSingleMedia> multiMedia, Integer scheduleDate) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestMessagesSendMultiMedia(silent, background, clearDraft, peer, replyToMsgId, multiMedia, scheduleDate));
    }

    @Override
    public TLAbsUpdates messagesSendScheduledMessages(TLAbsInputPeer peer, TLIntVector id) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestMessagesSendScheduledMessages(peer, id));
    }

    @Override
    public TLAbsUpdates messagesSendScreenshotNotification(TLAbsInputPeer peer, int replyToMsgId, long randomId) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestMessagesSendScreenshotNotification(peer, replyToMsgId, randomId));
    }

    @Override
    public TLAbsUpdates messagesSendVote(TLAbsInputPeer peer, int msgId, TLVector<TLBytes> options) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestMessagesSendVote(peer, msgId, options));
    }

    @Override
    public TLBool messagesSetBotCallbackAnswer(boolean alert, long queryId, String message, String url, int cacheTime) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesSetBotCallbackAnswer(alert, queryId, message, url, cacheTime));
    }

    @Override
    public TLBool messagesSetBotPrecheckoutResults(boolean success, long queryId, String error) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesSetBotPrecheckoutResults(success, queryId, error));
    }

    @Override
    public TLBool messagesSetBotShippingResults(long queryId, String error, TLVector<TLShippingOption> shippingOptions) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesSetBotShippingResults(queryId, error, shippingOptions));
    }

    @Override
    public TLBool messagesSetEncryptedTyping(TLInputEncryptedChat peer, boolean typing) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesSetEncryptedTyping(peer, typing));
    }

    @Override
    public TLAbsUpdates messagesSetGameScore(boolean editMessage, boolean force, TLAbsInputPeer peer, int id, TLAbsInputUser userId, int score) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestMessagesSetGameScore(editMessage, force, peer, id, userId, score));
    }

    @Override
    public TLBool messagesSetInlineBotResults(boolean gallery, boolean _private, long queryId, TLVector<TLAbsInputBotInlineResult> results, int cacheTime, String nextOffset, TLInlineBotSwitchPM switchPm) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesSetInlineBotResults(gallery, _private, queryId, results, cacheTime, nextOffset, switchPm));
    }

    @Override
    public TLBool messagesSetInlineGameScore(boolean editMessage, boolean force, TLInputBotInlineMessageID id, TLAbsInputUser userId, int score) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesSetInlineGameScore(editMessage, force, id, userId, score));
    }

    @Override
    public TLBool messagesSetTyping(TLAbsInputPeer peer, TLAbsSendMessageAction action) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesSetTyping(peer, action));
    }

    @Override
    public TLAbsUpdates messagesStartBot(TLAbsInputUser bot, TLAbsInputPeer peer, long randomId, String startParam) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestMessagesStartBot(bot, peer, randomId, startParam));
    }

    @Override
    public TLBool messagesToggleDialogPin(boolean pinned, TLAbsInputDialogPeer peer) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesToggleDialogPin(pinned, peer));
    }

    @Override
    public TLBool messagesToggleStickerSets(boolean uninstall, boolean archive, boolean unarchive, TLVector<TLAbsInputStickerSet> stickersets) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesToggleStickerSets(uninstall, archive, unarchive, stickersets));
    }

    @Override
    public TLBool messagesUninstallStickerSet(TLAbsInputStickerSet stickerset) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesUninstallStickerSet(stickerset));
    }

    @Override
    public TLBool messagesUpdateDialogFilter(int id, TLDialogFilter filter) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesUpdateDialogFilter(id, filter));
    }

    @Override
    public TLBool messagesUpdateDialogFiltersOrder(TLIntVector order) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestMessagesUpdateDialogFiltersOrder(order));
    }

    @Override
    public TLAbsUpdates messagesUpdatePinnedMessage(boolean silent, TLAbsInputPeer peer, int id) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestMessagesUpdatePinnedMessage(silent, peer, id));
    }

    @Override
    public TLAbsEncryptedFile messagesUploadEncryptedFile(TLInputEncryptedChat peer, TLAbsInputEncryptedFile file) throws RpcErrorException, IOException {
        return (TLAbsEncryptedFile) executeRpcQuery(new TLRequestMessagesUploadEncryptedFile(peer, file));
    }

    @Override
    public TLAbsMessageMedia messagesUploadMedia(TLAbsInputPeer peer, TLAbsInputMedia media) throws RpcErrorException, IOException {
        return (TLAbsMessageMedia) executeRpcQuery(new TLRequestMessagesUploadMedia(peer, media));
    }

    @Override
    public TLBool paymentsClearSavedInfo(boolean credentials, boolean info) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestPaymentsClearSavedInfo(credentials, info));
    }

    @Override
    public TLBankCardData paymentsGetBankCardData(String number) throws RpcErrorException, IOException {
        return (TLBankCardData) executeRpcQuery(new TLRequestPaymentsGetBankCardData(number));
    }

    @Override
    public TLPaymentForm paymentsGetPaymentForm(int msgId) throws RpcErrorException, IOException {
        return (TLPaymentForm) executeRpcQuery(new TLRequestPaymentsGetPaymentForm(msgId));
    }

    @Override
    public TLPaymentReceipt paymentsGetPaymentReceipt(int msgId) throws RpcErrorException, IOException {
        return (TLPaymentReceipt) executeRpcQuery(new TLRequestPaymentsGetPaymentReceipt(msgId));
    }

    @Override
    public TLSavedInfo paymentsGetSavedInfo() throws RpcErrorException, IOException {
        return (TLSavedInfo) executeRpcQuery(new TLRequestPaymentsGetSavedInfo());
    }

    @Override
    public TLAbsPaymentResult paymentsSendPaymentForm(int msgId, String requestedInfoId, String shippingOptionId, TLAbsInputPaymentCredentials credentials) throws RpcErrorException, IOException {
        return (TLAbsPaymentResult) executeRpcQuery(new TLRequestPaymentsSendPaymentForm(msgId, requestedInfoId, shippingOptionId, credentials));
    }

    @Override
    public TLValidatedRequestedInfo paymentsValidateRequestedInfo(boolean save, int msgId, TLPaymentRequestedInfo info) throws RpcErrorException, IOException {
        return (TLValidatedRequestedInfo) executeRpcQuery(new TLRequestPaymentsValidateRequestedInfo(save, msgId, info));
    }

    @Override
    public TLPhoneCall phoneAcceptCall(TLInputPhoneCall peer, TLBytes gB, TLPhoneCallProtocol protocol) throws RpcErrorException, IOException {
        return (TLPhoneCall) executeRpcQuery(new TLRequestPhoneAcceptCall(peer, gB, protocol));
    }

    @Override
    public TLPhoneCall phoneConfirmCall(TLInputPhoneCall peer, TLBytes gA, long keyFingerprint, TLPhoneCallProtocol protocol) throws RpcErrorException, IOException {
        return (TLPhoneCall) executeRpcQuery(new TLRequestPhoneConfirmCall(peer, gA, keyFingerprint, protocol));
    }

    @Override
    public TLAbsUpdates phoneDiscardCall(boolean video, TLInputPhoneCall peer, int duration, TLAbsPhoneCallDiscardReason reason, long connectionId) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestPhoneDiscardCall(video, peer, duration, reason, connectionId));
    }

    @Override
    public TLDataJSON phoneGetCallConfig() throws RpcErrorException, IOException {
        return (TLDataJSON) executeRpcQuery(new TLRequestPhoneGetCallConfig());
    }

    @Override
    public TLBool phoneReceivedCall(TLInputPhoneCall peer) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestPhoneReceivedCall(peer));
    }

    @Override
    public TLPhoneCall phoneRequestCall(boolean video, TLAbsInputUser userId, int randomId, TLBytes gAHash, TLPhoneCallProtocol protocol) throws RpcErrorException, IOException {
        return (TLPhoneCall) executeRpcQuery(new TLRequestPhoneRequestCall(video, userId, randomId, gAHash, protocol));
    }

    @Override
    public TLBool phoneSaveCallDebug(TLInputPhoneCall peer, TLDataJSON debug) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestPhoneSaveCallDebug(peer, debug));
    }

    @Override
    public TLAbsUpdates phoneSetCallRating(boolean userInitiative, TLInputPhoneCall peer, int rating, String comment) throws RpcErrorException, IOException {
        return (TLAbsUpdates) executeRpcQuery(new TLRequestPhoneSetCallRating(userInitiative, peer, rating, comment));
    }

    @Override
    public TLLongVector photosDeletePhotos(TLVector<TLAbsInputPhoto> id) throws RpcErrorException, IOException {
        return (TLLongVector) executeRpcQuery(new TLRequestPhotosDeletePhotos(id));
    }

    @Override
    public TLAbsPhotos photosGetUserPhotos(TLAbsInputUser userId, int offset, long maxId, int limit) throws RpcErrorException, IOException {
        return (TLAbsPhotos) executeRpcQuery(new TLRequestPhotosGetUserPhotos(userId, offset, maxId, limit));
    }

    @Override
    public TLAbsUserProfilePhoto photosUpdateProfilePhoto(TLAbsInputPhoto id) throws RpcErrorException, IOException {
        return (TLAbsUserProfilePhoto) executeRpcQuery(new TLRequestPhotosUpdateProfilePhoto(id));
    }

    @Override
    public TLPhoto photosUploadProfilePhoto(TLAbsInputFile file) throws RpcErrorException, IOException {
        return (TLPhoto) executeRpcQuery(new TLRequestPhotosUploadProfilePhoto(file));
    }

    @Override
    public TLBroadcastStats statsGetBroadcastStats(boolean dark, TLAbsInputChannel channel) throws RpcErrorException, IOException {
        return (TLBroadcastStats) executeRpcQuery(new TLRequestStatsGetBroadcastStats(dark, channel));
    }

    @Override
    public TLAbsStatsGraph statsLoadAsyncGraph(String token, Long x) throws RpcErrorException, IOException {
        return (TLAbsStatsGraph) executeRpcQuery(new TLRequestStatsLoadAsyncGraph(token, x));
    }

    @Override
    public TLStickerSet stickersAddStickerToSet(TLAbsInputStickerSet stickerset, TLInputStickerSetItem sticker) throws RpcErrorException, IOException {
        return (TLStickerSet) executeRpcQuery(new TLRequestStickersAddStickerToSet(stickerset, sticker));
    }

    @Override
    public TLStickerSet stickersChangeStickerPosition(TLAbsInputDocument sticker, int position) throws RpcErrorException, IOException {
        return (TLStickerSet) executeRpcQuery(new TLRequestStickersChangeStickerPosition(sticker, position));
    }

    @Override
    public TLStickerSet stickersCreateStickerSet(boolean masks, boolean animated, TLAbsInputUser userId, String title, String shortName, TLAbsInputDocument thumb, TLVector<TLInputStickerSetItem> stickers) throws RpcErrorException, IOException {
        return (TLStickerSet) executeRpcQuery(new TLRequestStickersCreateStickerSet(masks, animated, userId, title, shortName, thumb, stickers));
    }

    @Override
    public TLStickerSet stickersRemoveStickerFromSet(TLAbsInputDocument sticker) throws RpcErrorException, IOException {
        return (TLStickerSet) executeRpcQuery(new TLRequestStickersRemoveStickerFromSet(sticker));
    }

    @Override
    public TLStickerSet stickersSetStickerSetThumb(TLAbsInputStickerSet stickerset, TLAbsInputDocument thumb) throws RpcErrorException, IOException {
        return (TLStickerSet) executeRpcQuery(new TLRequestStickersSetStickerSetThumb(stickerset, thumb));
    }

    @Override
    public TLAbsChannelDifference updatesGetChannelDifference(boolean force, TLAbsInputChannel channel, TLAbsChannelMessagesFilter filter, int pts, int limit) throws RpcErrorException, IOException {
        return (TLAbsChannelDifference) executeRpcQuery(new TLRequestUpdatesGetChannelDifference(force, channel, filter, pts, limit));
    }

    @Override
    public TLAbsDifference updatesGetDifference(int pts, Integer ptsTotalLimit, int date, int qts) throws RpcErrorException, IOException {
        return (TLAbsDifference) executeRpcQuery(new TLRequestUpdatesGetDifference(pts, ptsTotalLimit, date, qts));
    }

    @Override
    public TLState updatesGetState() throws RpcErrorException, IOException {
        return (TLState) executeRpcQuery(new TLRequestUpdatesGetState());
    }

    @Override
    public TLAbsCdnFile uploadGetCdnFile(TLBytes fileToken, int offset, int limit) throws RpcErrorException, IOException {
        return (TLAbsCdnFile) executeRpcQuery(new TLRequestUploadGetCdnFile(fileToken, offset, limit));
    }

    @Override
    public TLVector<TLFileHash> uploadGetCdnFileHashes(TLBytes fileToken, int offset) throws RpcErrorException, IOException {
        return (TLVector<TLFileHash>) executeRpcQuery(new TLRequestUploadGetCdnFileHashes(fileToken, offset));
    }

    @Override
    public TLAbsFile uploadGetFile(boolean precise, boolean cdnSupported, TLAbsInputFileLocation location, int offset, int limit) throws RpcErrorException, IOException {
        return (TLAbsFile) executeRpcQuery(new TLRequestUploadGetFile(precise, cdnSupported, location, offset, limit));
    }

    @Override
    public TLVector<TLFileHash> uploadGetFileHashes(TLAbsInputFileLocation location, int offset) throws RpcErrorException, IOException {
        return (TLVector<TLFileHash>) executeRpcQuery(new TLRequestUploadGetFileHashes(location, offset));
    }

    @Override
    public TLWebFile uploadGetWebFile(TLAbsInputWebFileLocation location, int offset, int limit) throws RpcErrorException, IOException {
        return (TLWebFile) executeRpcQuery(new TLRequestUploadGetWebFile(location, offset, limit));
    }

    @Override
    public TLVector<TLFileHash> uploadReuploadCdnFile(TLBytes fileToken, TLBytes requestToken) throws RpcErrorException, IOException {
        return (TLVector<TLFileHash>) executeRpcQuery(new TLRequestUploadReuploadCdnFile(fileToken, requestToken));
    }

    @Override
    public TLBool uploadSaveBigFilePart(long fileId, int filePart, int fileTotalParts, TLBytes bytes) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestUploadSaveBigFilePart(fileId, filePart, fileTotalParts, bytes));
    }

    @Override
    public TLBool uploadSaveFilePart(long fileId, int filePart, TLBytes bytes) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestUploadSaveFilePart(fileId, filePart, bytes));
    }

    @Override
    public TLUserFull usersGetFullUser(TLAbsInputUser id) throws RpcErrorException, IOException {
        return (TLUserFull) executeRpcQuery(new TLRequestUsersGetFullUser(id));
    }

    @Override
    public TLVector<TLAbsUser> usersGetUsers(TLVector<TLAbsInputUser> id) throws RpcErrorException, IOException {
        return (TLVector<TLAbsUser>) executeRpcQuery(new TLRequestUsersGetUsers(id));
    }

    @Override
    public TLBool usersSetSecureValueErrors(TLAbsInputUser id, TLVector<TLAbsSecureValueError> errors) throws RpcErrorException, IOException {
        return (TLBool) executeRpcQuery(new TLRequestUsersSetSecureValueErrors(id, errors));
    }
}
