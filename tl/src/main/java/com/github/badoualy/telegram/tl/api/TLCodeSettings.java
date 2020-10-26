package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.TLContext;
import com.github.badoualy.telegram.tl.core.TLObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;

/**
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public class TLCodeSettings extends TLObject {
    public static final int CONSTRUCTOR_ID = 0xdebebe83;

    protected int flags;

    protected boolean allowFlashcall;

    protected boolean currentNumber;

    protected boolean allowAppHash;

    private final String _constructor = "codeSettings#debebe83";

    public TLCodeSettings() {
    }

    public TLCodeSettings(boolean allowFlashcall, boolean currentNumber, boolean allowAppHash) {
        this.allowFlashcall = allowFlashcall;
        this.currentNumber = currentNumber;
        this.allowAppHash = allowAppHash;
    }

    private void computeFlags() {
        flags = 0;
        flags = allowFlashcall ? (flags | 1) : (flags & ~1);
        flags = currentNumber ? (flags | 2) : (flags & ~2);
        flags = allowAppHash ? (flags | 16) : (flags & ~16);
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        computeFlags();

        writeInt(flags, stream);
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        flags = readInt(stream);
        allowFlashcall = (flags & 1) != 0;
        currentNumber = (flags & 2) != 0;
        allowAppHash = (flags & 16) != 0;
    }

    @Override
    public int computeSerializedSize() {
        computeFlags();

        int size = SIZE_CONSTRUCTOR_ID;
        size += SIZE_INT32;
        return size;
    }

    @Override
    public String toString() {
        return _constructor;
    }

    @Override
    public int getConstructorId() {
        return CONSTRUCTOR_ID;
    }

    public boolean getAllowFlashcall() {
        return allowFlashcall;
    }

    public void setAllowFlashcall(boolean allowFlashcall) {
        this.allowFlashcall = allowFlashcall;
    }

    public boolean getCurrentNumber() {
        return currentNumber;
    }

    public void setCurrentNumber(boolean currentNumber) {
        this.currentNumber = currentNumber;
    }

    public boolean getAllowAppHash() {
        return allowAppHash;
    }

    public void setAllowAppHash(boolean allowAppHash) {
        this.allowAppHash = allowAppHash;
    }
}
