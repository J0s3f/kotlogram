package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.TLContext;
import com.github.badoualy.telegram.tl.core.TLObject;
import com.github.badoualy.telegram.tl.core.TLVector;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;

/**
 * @author Yannick Badoual yann.badoual@gmail.com
 * @see <a href="http://github.com/badoualy/kotlogram">http://github.com/badoualy/kotlogram</a>
 */
public class TLPollResults extends TLObject {
    public static final int CONSTRUCTOR_ID = 0x0;

    protected int flags;

    protected boolean min;

    protected TLVector<TLPollAnswerVoters> results;

    protected Integer totalVoters;

    private final String _constructor = "pollResults#0";

    public TLPollResults() {
    }

    public TLPollResults(boolean min, TLVector<TLPollAnswerVoters> results, Integer totalVoters) {
        this.min = min;
        this.results = results;
        this.totalVoters = totalVoters;
    }

    private void computeFlags() {
        flags = 0;
        flags = min ? (flags | 1) : (flags & ~1);
        flags = results != null ? (flags | 2) : (flags & ~2);
        flags = totalVoters != null ? (flags | 4) : (flags & ~4);
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        computeFlags();

        writeInt(flags, stream);
        if ((flags & 2) != 0) {
            if (results == null) throwNullFieldException("results", flags);
            writeTLVector(results, stream);
        }
        if ((flags & 4) != 0) {
            if (totalVoters == null) throwNullFieldException("totalVoters", flags);
            writeInt(totalVoters, stream);
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        flags = readInt(stream);
        min = (flags & 1) != 0;
        results = (flags & 2) != 0 ? readTLVector(stream, context) : null;
        totalVoters = (flags & 4) != 0 ? readInt(stream) : null;
    }

    @Override
    public int computeSerializedSize() {
        computeFlags();

        int size = SIZE_CONSTRUCTOR_ID;
        size += SIZE_INT32;
        if ((flags & 2) != 0) {
            if (results == null) throwNullFieldException("results", flags);
            size += results.computeSerializedSize();
        }
        if ((flags & 4) != 0) {
            if (totalVoters == null) throwNullFieldException("totalVoters", flags);
            size += SIZE_INT32;
        }
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

    public boolean getMin() {
        return min;
    }

    public void setMin(boolean min) {
        this.min = min;
    }

    public TLVector<TLPollAnswerVoters> getResults() {
        return results;
    }

    public void setResults(TLVector<TLPollAnswerVoters> results) {
        this.results = results;
    }

    public Integer getTotalVoters() {
        return totalVoters;
    }

    public void setTotalVoters(Integer totalVoters) {
        this.totalVoters = totalVoters;
    }
}
