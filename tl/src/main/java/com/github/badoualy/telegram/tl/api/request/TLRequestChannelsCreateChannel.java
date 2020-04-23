package com.github.badoualy.telegram.tl.api.request;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.TLContext;
import com.github.badoualy.telegram.tl.api.TLAbsInputGeoPoint;
import com.github.badoualy.telegram.tl.api.TLAbsUpdates;
import com.github.badoualy.telegram.tl.core.TLMethod;
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
public class TLRequestChannelsCreateChannel extends TLMethod<TLAbsUpdates> {
    public static final int CONSTRUCTOR_ID = 0x0;

    protected int flags;

    protected boolean broadcast;

    protected boolean megagroup;

    protected String title;

    protected String about;

    protected TLAbsInputGeoPoint geoPoint;

    protected String address;

    private final String _constructor = "channels.createChannel#0";

    public TLRequestChannelsCreateChannel() {
    }

    public TLRequestChannelsCreateChannel(boolean broadcast, boolean megagroup, String title, String about, TLAbsInputGeoPoint geoPoint, String address) {
        this.broadcast = broadcast;
        this.megagroup = megagroup;
        this.title = title;
        this.about = about;
        this.geoPoint = geoPoint;
        this.address = address;
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public TLAbsUpdates deserializeResponse(InputStream stream, TLContext context) throws IOException {
        final TLObject response = readTLObject(stream, context);
        if (response == null) {
            throw new IOException("Unable to parse response");
        }
        if (!(response instanceof TLAbsUpdates)) {
            throw new IOException("Incorrect response type, expected " + getClass().getCanonicalName() + ", found " + response.getClass().getCanonicalName());
        }
        return (TLAbsUpdates) response;
    }

    private void computeFlags() {
        flags = 0;
        flags = broadcast ? (flags | 1) : (flags & ~1);
        flags = megagroup ? (flags | 2) : (flags & ~2);
        flags = geoPoint != null ? (flags | 4) : (flags & ~4);
        flags = address != null ? (flags | 4) : (flags & ~4);
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        computeFlags();

        writeInt(flags, stream);
        writeString(title, stream);
        writeString(about, stream);
        if ((flags & 4) != 0) {
            if (geoPoint == null) throwNullFieldException("geoPoint", flags);
            writeTLObject(geoPoint, stream);
        }
        if ((flags & 4) != 0) {
            if (address == null) throwNullFieldException("address", flags);
            writeString(address, stream);
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        flags = readInt(stream);
        broadcast = (flags & 1) != 0;
        megagroup = (flags & 2) != 0;
        title = readTLString(stream);
        about = readTLString(stream);
        geoPoint = (flags & 4) != 0 ? readTLObject(stream, context, TLAbsInputGeoPoint.class, -1) : null;
        address = (flags & 4) != 0 ? readTLString(stream) : null;
    }

    @Override
    public int computeSerializedSize() {
        computeFlags();

        int size = SIZE_CONSTRUCTOR_ID;
        size += SIZE_INT32;
        size += computeTLStringSerializedSize(title);
        size += computeTLStringSerializedSize(about);
        if ((flags & 4) != 0) {
            if (geoPoint == null) throwNullFieldException("geoPoint", flags);
            size += geoPoint.computeSerializedSize();
        }
        if ((flags & 4) != 0) {
            if (address == null) throwNullFieldException("address", flags);
            size += computeTLStringSerializedSize(address);
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

    public boolean getBroadcast() {
        return broadcast;
    }

    public void setBroadcast(boolean broadcast) {
        this.broadcast = broadcast;
    }

    public boolean getMegagroup() {
        return megagroup;
    }

    public void setMegagroup(boolean megagroup) {
        this.megagroup = megagroup;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public TLAbsInputGeoPoint getGeoPoint() {
        return geoPoint;
    }

    public void setGeoPoint(TLAbsInputGeoPoint geoPoint) {
        this.geoPoint = geoPoint;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
