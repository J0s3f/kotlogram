package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.TLContext;
import com.github.badoualy.telegram.tl.core.TLBytes;
import com.github.badoualy.telegram.tl.core.TLVector;
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
public class TLUpdateMessagePollVote extends TLAbsUpdate {
    public static final int CONSTRUCTOR_ID = 0x0;

    protected long pollId;

    protected int userId;

    protected TLVector<TLBytes> options;

    private final String _constructor = "updateMessagePollVote#0";

    public TLUpdateMessagePollVote() {
    }

    public TLUpdateMessagePollVote(long pollId, int userId, TLVector<TLBytes> options) {
        this.pollId = pollId;
        this.userId = userId;
        this.options = options;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeLong(pollId, stream);
        writeInt(userId, stream);
        writeTLVector(options, stream);
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        pollId = readLong(stream);
        userId = readInt(stream);
        options = readTLVector(stream, context);
    }

    @Override
    public int computeSerializedSize() {
        int size = SIZE_CONSTRUCTOR_ID;
        size += SIZE_INT64;
        size += SIZE_INT32;
        size += options.computeSerializedSize();
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

    public long getPollId() {
        return pollId;
    }

    public void setPollId(long pollId) {
        this.pollId = pollId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public TLVector<TLBytes> getOptions() {
        return options;
    }

    public void setOptions(TLVector<TLBytes> options) {
        this.options = options;
    }
}
