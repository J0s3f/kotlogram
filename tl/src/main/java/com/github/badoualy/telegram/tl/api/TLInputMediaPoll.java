package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.TLContext;
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
public class TLInputMediaPoll extends TLAbsInputMedia {
    public static final int CONSTRUCTOR_ID = 0x0;

    protected TLPoll poll;

    private final String _constructor = "inputMediaPoll#0";

    public TLInputMediaPoll() {
    }

    public TLInputMediaPoll(TLPoll poll) {
        this.poll = poll;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeTLObject(poll, stream);
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        poll = readTLObject(stream, context, TLPoll.class, TLPoll.CONSTRUCTOR_ID);
    }

    @Override
    public int computeSerializedSize() {
        int size = SIZE_CONSTRUCTOR_ID;
        size += poll.computeSerializedSize();
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

    public TLPoll getPoll() {
        return poll;
    }

    public void setPoll(TLPoll poll) {
        this.poll = poll;
    }
}
