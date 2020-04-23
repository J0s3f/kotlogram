package com.github.badoualy.telegram.tl.api.account;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.TLContext;
import com.github.badoualy.telegram.tl.api.TLAuthorization;
import com.github.badoualy.telegram.tl.core.TLObject;
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
public class TLAuthorizations extends TLObject {
    public static final int CONSTRUCTOR_ID = 0x0;

    protected TLVector<TLAuthorization> authorizations;

    private final String _constructor = "account.authorizations#0";

    public TLAuthorizations() {
    }

    public TLAuthorizations(TLVector<TLAuthorization> authorizations) {
        this.authorizations = authorizations;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeTLVector(authorizations, stream);
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        authorizations = readTLVector(stream, context);
    }

    @Override
    public int computeSerializedSize() {
        int size = SIZE_CONSTRUCTOR_ID;
        size += authorizations.computeSerializedSize();
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

    public TLVector<TLAuthorization> getAuthorizations() {
        return authorizations;
    }

    public void setAuthorizations(TLVector<TLAuthorization> authorizations) {
        this.authorizations = authorizations;
    }
}
