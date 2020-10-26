package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLInputMediaContact}: inputMediaContact#f8ab7dfb</li>
 * <li>{@link TLInputMediaDice}: inputMediaDice#e66fbf7b</li>
 * <li>{@link TLInputMediaDocument}: inputMediaDocument#23ab23d2</li>
 * <li>{@link TLInputMediaDocumentExternal}: inputMediaDocumentExternal#fb52dc99</li>
 * <li>{@link TLInputMediaEmpty}: inputMediaEmpty#9664f57f</li>
 * <li>{@link TLInputMediaGame}: inputMediaGame#d33f43f3</li>
 * <li>{@link TLInputMediaGeoLive}: inputMediaGeoLive#ce4e82fd</li>
 * <li>{@link TLInputMediaGeoPoint}: inputMediaGeoPoint#f9c44144</li>
 * <li>{@link TLInputMediaGifExternal}: inputMediaGifExternal#4843b0fd</li>
 * <li>{@link TLInputMediaInvoice}: inputMediaInvoice#f4e096c3</li>
 * <li>{@link TLInputMediaPhoto}: inputMediaPhoto#b3ba0635</li>
 * <li>{@link TLInputMediaPhotoExternal}: inputMediaPhotoExternal#e5bbfe1a</li>
 * <li>{@link TLInputMediaPoll}: inputMediaPoll#f94e5f1</li>
 * <li>{@link TLInputMediaUploadedDocument}: inputMediaUploadedDocument#5b38c6c1</li>
 * <li>{@link TLInputMediaUploadedPhoto}: inputMediaUploadedPhoto#1e287d04</li>
 * <li>{@link TLInputMediaVenue}: inputMediaVenue#c13d1c11</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsInputMedia extends TLObject {
    public TLAbsInputMedia() {
    }
}
