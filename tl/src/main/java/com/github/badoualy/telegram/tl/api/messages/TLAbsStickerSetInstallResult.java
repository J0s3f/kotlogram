package com.github.badoualy.telegram.tl.api.messages;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLStickerSetInstallResultArchive}: messages.stickerSetInstallResultArchive#35e410a8</li>
 * <li>{@link TLStickerSetInstallResultSuccess}: messages.stickerSetInstallResultSuccess#38641628</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsStickerSetInstallResult extends TLObject {
    public TLAbsStickerSetInstallResult() {
    }
}
