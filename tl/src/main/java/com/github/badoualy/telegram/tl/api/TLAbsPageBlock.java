package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLPageBlockAnchor}: pageBlockAnchor#ce0d37b0</li>
 * <li>{@link TLPageBlockAudio}: pageBlockAudio#804361ea</li>
 * <li>{@link TLPageBlockAuthorDate}: pageBlockAuthorDate#baafe5e0</li>
 * <li>{@link TLPageBlockBlockquote}: pageBlockBlockquote#263d7c26</li>
 * <li>{@link TLPageBlockChannel}: pageBlockChannel#ef1751b5</li>
 * <li>{@link TLPageBlockCollage}: pageBlockCollage#65a0fa4d</li>
 * <li>{@link TLPageBlockCover}: pageBlockCover#39f23300</li>
 * <li>{@link TLPageBlockDetails}: pageBlockDetails#76768bed</li>
 * <li>{@link TLPageBlockDivider}: pageBlockDivider#db20b188</li>
 * <li>{@link TLPageBlockEmbed}: pageBlockEmbed#a8718dc5</li>
 * <li>{@link TLPageBlockEmbedPost}: pageBlockEmbedPost#f259a80b</li>
 * <li>{@link TLPageBlockFooter}: pageBlockFooter#48870999</li>
 * <li>{@link TLPageBlockHeader}: pageBlockHeader#bfd064ec</li>
 * <li>{@link TLPageBlockKicker}: pageBlockKicker#1e148390</li>
 * <li>{@link TLPageBlockList}: pageBlockList#e4e88011</li>
 * <li>{@link TLPageBlockMap}: pageBlockMap#a44f3ef6</li>
 * <li>{@link TLPageBlockOrderedList}: pageBlockOrderedList#9a8ae1e1</li>
 * <li>{@link TLPageBlockParagraph}: pageBlockParagraph#467a0766</li>
 * <li>{@link TLPageBlockPhoto}: pageBlockPhoto#1759c560</li>
 * <li>{@link TLPageBlockPreformatted}: pageBlockPreformatted#c070d93e</li>
 * <li>{@link TLPageBlockPullquote}: pageBlockPullquote#4f4456d3</li>
 * <li>{@link TLPageBlockRelatedArticles}: pageBlockRelatedArticles#16115a96</li>
 * <li>{@link TLPageBlockSlideshow}: pageBlockSlideshow#31f9590</li>
 * <li>{@link TLPageBlockSubheader}: pageBlockSubheader#f12bb6e1</li>
 * <li>{@link TLPageBlockSubtitle}: pageBlockSubtitle#8ffa9a1f</li>
 * <li>{@link TLPageBlockTable}: pageBlockTable#bf4dea82</li>
 * <li>{@link TLPageBlockTitle}: pageBlockTitle#70abc3fd</li>
 * <li>{@link TLPageBlockUnsupported}: pageBlockUnsupported#13567e8a</li>
 * <li>{@link TLPageBlockVideo}: pageBlockVideo#7c8fe7b6</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsPageBlock extends TLObject {
    public TLAbsPageBlock() {
    }
}
