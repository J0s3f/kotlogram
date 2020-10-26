package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLInputWallPaper}: inputWallPaper#e630b979</li>
 * <li>{@link TLInputWallPaperNoFile}: inputWallPaperNoFile#8427bbac</li>
 * <li>{@link TLInputWallPaperSlug}: inputWallPaperSlug#72091c80</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsInputWallPaper extends TLObject {
    public TLAbsInputWallPaper() {
    }
}
