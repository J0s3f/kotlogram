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
public class TLPhoneCall extends TLAbsPhoneCall {
    public static final int CONSTRUCTOR_ID = 0x0;

    protected int flags;

    protected boolean p2pAllowed;

    protected boolean video;

    protected long accessHash;

    protected int date;

    protected int adminId;

    protected int participantId;

    protected TLBytes gAOrB;

    protected long keyFingerprint;

    protected TLPhoneCallProtocol protocol;

    protected TLVector<TLPhoneConnection> connections;

    protected int startDate;

    private final String _constructor = "phoneCall#0";

    public TLPhoneCall() {
    }

    public TLPhoneCall(boolean p2pAllowed, boolean video, long id, long accessHash, int date, int adminId, int participantId, TLBytes gAOrB, long keyFingerprint, TLPhoneCallProtocol protocol, TLVector<TLPhoneConnection> connections, int startDate) {
        this.p2pAllowed = p2pAllowed;
        this.video = video;
        this.id = id;
        this.accessHash = accessHash;
        this.date = date;
        this.adminId = adminId;
        this.participantId = participantId;
        this.gAOrB = gAOrB;
        this.keyFingerprint = keyFingerprint;
        this.protocol = protocol;
        this.connections = connections;
        this.startDate = startDate;
    }

    private void computeFlags() {
        flags = 0;
        flags = p2pAllowed ? (flags | 32) : (flags & ~32);
        flags = video ? (flags | 64) : (flags & ~64);
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        computeFlags();

        writeInt(flags, stream);
        writeLong(id, stream);
        writeLong(accessHash, stream);
        writeInt(date, stream);
        writeInt(adminId, stream);
        writeInt(participantId, stream);
        writeTLBytes(gAOrB, stream);
        writeLong(keyFingerprint, stream);
        writeTLObject(protocol, stream);
        writeTLVector(connections, stream);
        writeInt(startDate, stream);
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        flags = readInt(stream);
        p2pAllowed = (flags & 32) != 0;
        video = (flags & 64) != 0;
        id = readLong(stream);
        accessHash = readLong(stream);
        date = readInt(stream);
        adminId = readInt(stream);
        participantId = readInt(stream);
        gAOrB = readTLBytes(stream, context);
        keyFingerprint = readLong(stream);
        protocol = readTLObject(stream, context, TLPhoneCallProtocol.class, TLPhoneCallProtocol.CONSTRUCTOR_ID);
        connections = readTLVector(stream, context);
        startDate = readInt(stream);
    }

    @Override
    public int computeSerializedSize() {
        computeFlags();

        int size = SIZE_CONSTRUCTOR_ID;
        size += SIZE_INT32;
        size += SIZE_INT64;
        size += SIZE_INT64;
        size += SIZE_INT32;
        size += SIZE_INT32;
        size += SIZE_INT32;
        size += computeTLBytesSerializedSize(gAOrB);
        size += SIZE_INT64;
        size += protocol.computeSerializedSize();
        size += connections.computeSerializedSize();
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

    public boolean getP2pAllowed() {
        return p2pAllowed;
    }

    public void setP2pAllowed(boolean p2pAllowed) {
        this.p2pAllowed = p2pAllowed;
    }

    public boolean getVideo() {
        return video;
    }

    public void setVideo(boolean video) {
        this.video = video;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getAccessHash() {
        return accessHash;
    }

    public void setAccessHash(long accessHash) {
        this.accessHash = accessHash;
    }

    public int getDate() {
        return date;
    }

    public void setDate(int date) {
        this.date = date;
    }

    public int getAdminId() {
        return adminId;
    }

    public void setAdminId(int adminId) {
        this.adminId = adminId;
    }

    public int getParticipantId() {
        return participantId;
    }

    public void setParticipantId(int participantId) {
        this.participantId = participantId;
    }

    public TLBytes getGAOrB() {
        return gAOrB;
    }

    public void setGAOrB(TLBytes gAOrB) {
        this.gAOrB = gAOrB;
    }

    public long getKeyFingerprint() {
        return keyFingerprint;
    }

    public void setKeyFingerprint(long keyFingerprint) {
        this.keyFingerprint = keyFingerprint;
    }

    public TLPhoneCallProtocol getProtocol() {
        return protocol;
    }

    public void setProtocol(TLPhoneCallProtocol protocol) {
        this.protocol = protocol;
    }

    public TLVector<TLPhoneConnection> getConnections() {
        return connections;
    }

    public void setConnections(TLVector<TLPhoneConnection> connections) {
        this.connections = connections;
    }

    public int getStartDate() {
        return startDate;
    }

    public void setStartDate(int startDate) {
        this.startDate = startDate;
    }
}
