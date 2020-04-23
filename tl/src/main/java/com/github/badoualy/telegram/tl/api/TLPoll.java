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
public class TLPoll extends TLObject {
    public static final int CONSTRUCTOR_ID = 0x0;

    protected long id;

    protected int flags;

    protected boolean closed;

    protected boolean publicVoters;

    protected boolean multipleChoice;

    protected boolean quiz;

    protected String question;

    protected TLVector<TLPollAnswer> answers;

    private final String _constructor = "poll#0";

    public TLPoll() {
    }

    public TLPoll(long id, boolean closed, boolean publicVoters, boolean multipleChoice, boolean quiz, String question, TLVector<TLPollAnswer> answers) {
        this.id = id;
        this.closed = closed;
        this.publicVoters = publicVoters;
        this.multipleChoice = multipleChoice;
        this.quiz = quiz;
        this.question = question;
        this.answers = answers;
    }

    private void computeFlags() {
        flags = 0;
        flags = closed ? (flags | 1) : (flags & ~1);
        flags = publicVoters ? (flags | 2) : (flags & ~2);
        flags = multipleChoice ? (flags | 4) : (flags & ~4);
        flags = quiz ? (flags | 8) : (flags & ~8);
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        computeFlags();

        writeLong(id, stream);
        writeInt(flags, stream);
        writeString(question, stream);
        writeTLVector(answers, stream);
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        id = readLong(stream);
        flags = readInt(stream);
        closed = (flags & 1) != 0;
        publicVoters = (flags & 2) != 0;
        multipleChoice = (flags & 4) != 0;
        quiz = (flags & 8) != 0;
        question = readTLString(stream);
        answers = readTLVector(stream, context);
    }

    @Override
    public int computeSerializedSize() {
        computeFlags();

        int size = SIZE_CONSTRUCTOR_ID;
        size += SIZE_INT64;
        size += SIZE_INT32;
        size += computeTLStringSerializedSize(question);
        size += answers.computeSerializedSize();
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

    public boolean getClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public boolean getPublicVoters() {
        return publicVoters;
    }

    public void setPublicVoters(boolean publicVoters) {
        this.publicVoters = publicVoters;
    }

    public boolean getMultipleChoice() {
        return multipleChoice;
    }

    public void setMultipleChoice(boolean multipleChoice) {
        this.multipleChoice = multipleChoice;
    }

    public boolean getQuiz() {
        return quiz;
    }

    public void setQuiz(boolean quiz) {
        this.quiz = quiz;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public TLVector<TLPollAnswer> getAnswers() {
        return answers;
    }

    public void setAnswers(TLVector<TLPollAnswer> answers) {
        this.answers = answers;
    }
}
