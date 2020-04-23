package com.github.badoualy.telegram.tl.api;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.TLContext;
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
public class TLBotInfo extends TLObject {
    public static final int CONSTRUCTOR_ID = 0x0;

    protected int userId;

    protected String description;

    protected TLVector<TLBotCommand> commands;

    private final String _constructor = "botInfo#0";

    public TLBotInfo() {
    }

    public TLBotInfo(int userId, String description, TLVector<TLBotCommand> commands) {
        this.userId = userId;
        this.description = description;
        this.commands = commands;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeInt(userId, stream);
        writeString(description, stream);
        writeTLVector(commands, stream);
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        userId = readInt(stream);
        description = readTLString(stream);
        commands = readTLVector(stream, context);
    }

    @Override
    public int computeSerializedSize() {
        int size = SIZE_CONSTRUCTOR_ID;
        size += SIZE_INT32;
        size += computeTLStringSerializedSize(description);
        size += commands.computeSerializedSize();
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

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TLVector<TLBotCommand> getCommands() {
        return commands;
    }

    public void setCommands(TLVector<TLBotCommand> commands) {
        this.commands = commands;
    }
}
