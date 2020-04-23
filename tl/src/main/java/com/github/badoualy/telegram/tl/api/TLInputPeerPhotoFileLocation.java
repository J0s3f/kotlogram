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
public class TLInputPeerPhotoFileLocation extends TLAbsInputFileLocation {
    public static final int CONSTRUCTOR_ID = 0x0;

    protected int flags;

    protected boolean big;

    protected TLAbsInputPeer peer;

    protected long volumeId;

    protected int localId;

    private final String _constructor = "inputPeerPhotoFileLocation#0";

    public TLInputPeerPhotoFileLocation() {
    }

    public TLInputPeerPhotoFileLocation(boolean big, TLAbsInputPeer peer, long volumeId, int localId) {
        this.big = big;
        this.peer = peer;
        this.volumeId = volumeId;
        this.localId = localId;
    }

    private void computeFlags() {
        flags = 0;
        flags = big ? (flags | 1) : (flags & ~1);
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        computeFlags();

        writeInt(flags, stream);
        writeTLObject(peer, stream);
        writeLong(volumeId, stream);
        writeInt(localId, stream);
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        flags = readInt(stream);
        big = (flags & 1) != 0;
        peer = readTLObject(stream, context, TLAbsInputPeer.class, -1);
        volumeId = readLong(stream);
        localId = readInt(stream);
    }

    @Override
    public int computeSerializedSize() {
        computeFlags();

        int size = SIZE_CONSTRUCTOR_ID;
        size += SIZE_INT32;
        size += peer.computeSerializedSize();
        size += SIZE_INT64;
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

    public boolean getBig() {
        return big;
    }

    public void setBig(boolean big) {
        this.big = big;
    }

    public TLAbsInputPeer getPeer() {
        return peer;
    }

    public void setPeer(TLAbsInputPeer peer) {
        this.peer = peer;
    }

    public long getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(long volumeId) {
        this.volumeId = volumeId;
    }

    public int getLocalId() {
        return localId;
    }

    public void setLocalId(int localId) {
        this.localId = localId;
    }
}
