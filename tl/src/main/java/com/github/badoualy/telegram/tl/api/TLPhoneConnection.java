package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.TLContext;
import com.github.badoualy.telegram.tl.core.TLBytes;
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
public class TLPhoneConnection extends TLObject {
    public static final int CONSTRUCTOR_ID = 0x9d4c17c0;

    protected long id;

    protected String ip;

    protected String ipv6;

    protected int port;

    protected TLBytes peerTag;

    private final String _constructor = "phoneConnection#9d4c17c0";

    public TLPhoneConnection() {
    }

    public TLPhoneConnection(long id, String ip, String ipv6, int port, TLBytes peerTag) {
        this.id = id;
        this.ip = ip;
        this.ipv6 = ipv6;
        this.port = port;
        this.peerTag = peerTag;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeLong(id, stream);
        writeString(ip, stream);
        writeString(ipv6, stream);
        writeInt(port, stream);
        writeTLBytes(peerTag, stream);
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        id = readLong(stream);
        ip = readTLString(stream);
        ipv6 = readTLString(stream);
        port = readInt(stream);
        peerTag = readTLBytes(stream, context);
    }

    @Override
    public int computeSerializedSize() {
        int size = SIZE_CONSTRUCTOR_ID;
        size += SIZE_INT64;
        size += computeTLStringSerializedSize(ip);
        size += computeTLStringSerializedSize(ipv6);
        size += SIZE_INT32;
        size += computeTLBytesSerializedSize(peerTag);
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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getIpv6() {
        return ipv6;
    }

    public void setIpv6(String ipv6) {
        this.ipv6 = ipv6;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public TLBytes getPeerTag() {
        return peerTag;
    }

    public void setPeerTag(TLBytes peerTag) {
        this.peerTag = peerTag;
    }
}
