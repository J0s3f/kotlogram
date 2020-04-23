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
public class TLAutoDownloadSettings extends TLObject {
    public static final int CONSTRUCTOR_ID = 0x0;

    protected int flags;

    protected boolean disabled;

    protected boolean videoPreloadLarge;

    protected boolean audioPreloadNext;

    protected boolean phonecallsLessData;

    protected int photoSizeMax;

    protected int videoSizeMax;

    protected int fileSizeMax;

    protected int videoUploadMaxbitrate;

    private final String _constructor = "autoDownloadSettings#0";

    public TLAutoDownloadSettings() {
    }

    public TLAutoDownloadSettings(boolean disabled, boolean videoPreloadLarge, boolean audioPreloadNext, boolean phonecallsLessData, int photoSizeMax, int videoSizeMax, int fileSizeMax, int videoUploadMaxbitrate) {
        this.disabled = disabled;
        this.videoPreloadLarge = videoPreloadLarge;
        this.audioPreloadNext = audioPreloadNext;
        this.phonecallsLessData = phonecallsLessData;
        this.photoSizeMax = photoSizeMax;
        this.videoSizeMax = videoSizeMax;
        this.fileSizeMax = fileSizeMax;
        this.videoUploadMaxbitrate = videoUploadMaxbitrate;
    }

    private void computeFlags() {
        flags = 0;
        flags = disabled ? (flags | 1) : (flags & ~1);
        flags = videoPreloadLarge ? (flags | 2) : (flags & ~2);
        flags = audioPreloadNext ? (flags | 4) : (flags & ~4);
        flags = phonecallsLessData ? (flags | 8) : (flags & ~8);
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        computeFlags();

        writeInt(flags, stream);
        writeInt(photoSizeMax, stream);
        writeInt(videoSizeMax, stream);
        writeInt(fileSizeMax, stream);
        writeInt(videoUploadMaxbitrate, stream);
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        flags = readInt(stream);
        disabled = (flags & 1) != 0;
        videoPreloadLarge = (flags & 2) != 0;
        audioPreloadNext = (flags & 4) != 0;
        phonecallsLessData = (flags & 8) != 0;
        photoSizeMax = readInt(stream);
        videoSizeMax = readInt(stream);
        fileSizeMax = readInt(stream);
        videoUploadMaxbitrate = readInt(stream);
    }

    @Override
    public int computeSerializedSize() {
        computeFlags();

        int size = SIZE_CONSTRUCTOR_ID;
        size += SIZE_INT32;
        size += SIZE_INT32;
        size += SIZE_INT32;
        size += SIZE_INT32;
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

    public boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean getVideoPreloadLarge() {
        return videoPreloadLarge;
    }

    public void setVideoPreloadLarge(boolean videoPreloadLarge) {
        this.videoPreloadLarge = videoPreloadLarge;
    }

    public boolean getAudioPreloadNext() {
        return audioPreloadNext;
    }

    public void setAudioPreloadNext(boolean audioPreloadNext) {
        this.audioPreloadNext = audioPreloadNext;
    }

    public boolean getPhonecallsLessData() {
        return phonecallsLessData;
    }

    public void setPhonecallsLessData(boolean phonecallsLessData) {
        this.phonecallsLessData = phonecallsLessData;
    }

    public int getPhotoSizeMax() {
        return photoSizeMax;
    }

    public void setPhotoSizeMax(int photoSizeMax) {
        this.photoSizeMax = photoSizeMax;
    }

    public int getVideoSizeMax() {
        return videoSizeMax;
    }

    public void setVideoSizeMax(int videoSizeMax) {
        this.videoSizeMax = videoSizeMax;
    }

    public int getFileSizeMax() {
        return fileSizeMax;
    }

    public void setFileSizeMax(int fileSizeMax) {
        this.fileSizeMax = fileSizeMax;
    }

    public int getVideoUploadMaxbitrate() {
        return videoUploadMaxbitrate;
    }

    public void setVideoUploadMaxbitrate(int videoUploadMaxbitrate) {
        this.videoUploadMaxbitrate = videoUploadMaxbitrate;
    }
}
