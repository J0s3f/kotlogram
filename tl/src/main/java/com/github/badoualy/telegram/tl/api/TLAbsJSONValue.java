package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.core.TLObject;

/**
 * Abstraction level for the following constructors:
 * <ul>
 * <li>{@link TLJsonArray}: jsonArray#f7444763</li>
 * <li>{@link TLJsonBool}: jsonBool#c7345e6a</li>
 * <li>{@link TLJsonNull}: jsonNull#3f6d7b68</li>
 * <li>{@link TLJsonNumber}: jsonNumber#2be0dfa4</li>
 * <li>{@link TLJsonObject}: jsonObject#99c1d49d</li>
 * <li>{@link TLJsonString}: jsonString#b71e767a</li>
 * </ul>
 *
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public abstract class TLAbsJSONValue extends TLObject {
    public TLAbsJSONValue() {
    }
}
