package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLTextAnchor}: textAnchor#35553762</li>
 * <li>{@link TLTextBold}: textBold#6724abc4</li>
 * <li>{@link TLTextConcat}: textConcat#7e6260d7</li>
 * <li>{@link TLTextEmail}: textEmail#de5a0dd6</li>
 * <li>{@link TLTextEmpty}: textEmpty#dc3d824f</li>
 * <li>{@link TLTextFixed}: textFixed#6c3f19b9</li>
 * <li>{@link TLTextImage}: textImage#81ccf4f</li>
 * <li>{@link TLTextItalic}: textItalic#d912a59c</li>
 * <li>{@link TLTextMarked}: textMarked#34b8621</li>
 * <li>{@link TLTextPhone}: textPhone#1ccb966a</li>
 * <li>{@link TLTextPlain}: textPlain#744694e0</li>
 * <li>{@link TLTextStrike}: textStrike#9bf8bb95</li>
 * <li>{@link TLTextSubscript}: textSubscript#ed6a8504</li>
 * <li>{@link TLTextSuperscript}: textSuperscript#c7fb5e01</li>
 * <li>{@link TLTextUnderline}: textUnderline#c12622c4</li>
 * <li>{@link TLTextUrl}: textUrl#3c2884c1</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsRichText extends TLObject {
    public TLAbsRichText() {
    }
}
