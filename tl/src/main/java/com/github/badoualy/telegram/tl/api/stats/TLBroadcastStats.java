package com.github.badoualy.telegram.tl.api.stats;

import static com.github.badoualy.telegram.tl.StreamUtils.*;
import static com.github.badoualy.telegram.tl.TLObjectUtils.*;

import com.github.badoualy.telegram.tl.TLContext;
import com.github.badoualy.telegram.tl.api.TLAbsStatsGraph;
import com.github.badoualy.telegram.tl.api.TLMessageInteractionCounters;
import com.github.badoualy.telegram.tl.api.TLStatsAbsValueAndPrev;
import com.github.badoualy.telegram.tl.api.TLStatsDateRangeDays;
import com.github.badoualy.telegram.tl.api.TLStatsPercentValue;
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
public class TLBroadcastStats extends TLObject {
    public static final int CONSTRUCTOR_ID = 0x0;

    protected TLStatsDateRangeDays period;

    protected TLStatsAbsValueAndPrev followers;

    protected TLStatsAbsValueAndPrev viewsPerPost;

    protected TLStatsAbsValueAndPrev sharesPerPost;

    protected TLStatsPercentValue enabledNotifications;

    protected TLAbsStatsGraph growthGraph;

    protected TLAbsStatsGraph followersGraph;

    protected TLAbsStatsGraph muteGraph;

    protected TLAbsStatsGraph topHoursGraph;

    protected TLAbsStatsGraph interactionsGraph;

    protected TLAbsStatsGraph ivInteractionsGraph;

    protected TLAbsStatsGraph viewsBySourceGraph;

    protected TLAbsStatsGraph newFollowersBySourceGraph;

    protected TLAbsStatsGraph languagesGraph;

    protected TLVector<TLMessageInteractionCounters> recentMessageInteractions;

    private final String _constructor = "stats.broadcastStats#0";

    public TLBroadcastStats() {
    }

    public TLBroadcastStats(TLStatsDateRangeDays period, TLStatsAbsValueAndPrev followers, TLStatsAbsValueAndPrev viewsPerPost, TLStatsAbsValueAndPrev sharesPerPost, TLStatsPercentValue enabledNotifications, TLAbsStatsGraph growthGraph, TLAbsStatsGraph followersGraph, TLAbsStatsGraph muteGraph, TLAbsStatsGraph topHoursGraph, TLAbsStatsGraph interactionsGraph, TLAbsStatsGraph ivInteractionsGraph, TLAbsStatsGraph viewsBySourceGraph, TLAbsStatsGraph newFollowersBySourceGraph, TLAbsStatsGraph languagesGraph, TLVector<TLMessageInteractionCounters> recentMessageInteractions) {
        this.period = period;
        this.followers = followers;
        this.viewsPerPost = viewsPerPost;
        this.sharesPerPost = sharesPerPost;
        this.enabledNotifications = enabledNotifications;
        this.growthGraph = growthGraph;
        this.followersGraph = followersGraph;
        this.muteGraph = muteGraph;
        this.topHoursGraph = topHoursGraph;
        this.interactionsGraph = interactionsGraph;
        this.ivInteractionsGraph = ivInteractionsGraph;
        this.viewsBySourceGraph = viewsBySourceGraph;
        this.newFollowersBySourceGraph = newFollowersBySourceGraph;
        this.languagesGraph = languagesGraph;
        this.recentMessageInteractions = recentMessageInteractions;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeTLObject(period, stream);
        writeTLObject(followers, stream);
        writeTLObject(viewsPerPost, stream);
        writeTLObject(sharesPerPost, stream);
        writeTLObject(enabledNotifications, stream);
        writeTLObject(growthGraph, stream);
        writeTLObject(followersGraph, stream);
        writeTLObject(muteGraph, stream);
        writeTLObject(topHoursGraph, stream);
        writeTLObject(interactionsGraph, stream);
        writeTLObject(ivInteractionsGraph, stream);
        writeTLObject(viewsBySourceGraph, stream);
        writeTLObject(newFollowersBySourceGraph, stream);
        writeTLObject(languagesGraph, stream);
        writeTLVector(recentMessageInteractions, stream);
    }

    @Override
    @SuppressWarnings({"unchecked", "SimplifiableConditionalExpression"})
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        period = readTLObject(stream, context, TLStatsDateRangeDays.class, TLStatsDateRangeDays.CONSTRUCTOR_ID);
        followers = readTLObject(stream, context, TLStatsAbsValueAndPrev.class, TLStatsAbsValueAndPrev.CONSTRUCTOR_ID);
        viewsPerPost = readTLObject(stream, context, TLStatsAbsValueAndPrev.class, TLStatsAbsValueAndPrev.CONSTRUCTOR_ID);
        sharesPerPost = readTLObject(stream, context, TLStatsAbsValueAndPrev.class, TLStatsAbsValueAndPrev.CONSTRUCTOR_ID);
        enabledNotifications = readTLObject(stream, context, TLStatsPercentValue.class, TLStatsPercentValue.CONSTRUCTOR_ID);
        growthGraph = readTLObject(stream, context, TLAbsStatsGraph.class, -1);
        followersGraph = readTLObject(stream, context, TLAbsStatsGraph.class, -1);
        muteGraph = readTLObject(stream, context, TLAbsStatsGraph.class, -1);
        topHoursGraph = readTLObject(stream, context, TLAbsStatsGraph.class, -1);
        interactionsGraph = readTLObject(stream, context, TLAbsStatsGraph.class, -1);
        ivInteractionsGraph = readTLObject(stream, context, TLAbsStatsGraph.class, -1);
        viewsBySourceGraph = readTLObject(stream, context, TLAbsStatsGraph.class, -1);
        newFollowersBySourceGraph = readTLObject(stream, context, TLAbsStatsGraph.class, -1);
        languagesGraph = readTLObject(stream, context, TLAbsStatsGraph.class, -1);
        recentMessageInteractions = readTLVector(stream, context);
    }

    @Override
    public int computeSerializedSize() {
        int size = SIZE_CONSTRUCTOR_ID;
        size += period.computeSerializedSize();
        size += followers.computeSerializedSize();
        size += viewsPerPost.computeSerializedSize();
        size += sharesPerPost.computeSerializedSize();
        size += enabledNotifications.computeSerializedSize();
        size += growthGraph.computeSerializedSize();
        size += followersGraph.computeSerializedSize();
        size += muteGraph.computeSerializedSize();
        size += topHoursGraph.computeSerializedSize();
        size += interactionsGraph.computeSerializedSize();
        size += ivInteractionsGraph.computeSerializedSize();
        size += viewsBySourceGraph.computeSerializedSize();
        size += newFollowersBySourceGraph.computeSerializedSize();
        size += languagesGraph.computeSerializedSize();
        size += recentMessageInteractions.computeSerializedSize();
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

    public TLStatsDateRangeDays getPeriod() {
        return period;
    }

    public void setPeriod(TLStatsDateRangeDays period) {
        this.period = period;
    }

    public TLStatsAbsValueAndPrev getFollowers() {
        return followers;
    }

    public void setFollowers(TLStatsAbsValueAndPrev followers) {
        this.followers = followers;
    }

    public TLStatsAbsValueAndPrev getViewsPerPost() {
        return viewsPerPost;
    }

    public void setViewsPerPost(TLStatsAbsValueAndPrev viewsPerPost) {
        this.viewsPerPost = viewsPerPost;
    }

    public TLStatsAbsValueAndPrev getSharesPerPost() {
        return sharesPerPost;
    }

    public void setSharesPerPost(TLStatsAbsValueAndPrev sharesPerPost) {
        this.sharesPerPost = sharesPerPost;
    }

    public TLStatsPercentValue getEnabledNotifications() {
        return enabledNotifications;
    }

    public void setEnabledNotifications(TLStatsPercentValue enabledNotifications) {
        this.enabledNotifications = enabledNotifications;
    }

    public TLAbsStatsGraph getGrowthGraph() {
        return growthGraph;
    }

    public void setGrowthGraph(TLAbsStatsGraph growthGraph) {
        this.growthGraph = growthGraph;
    }

    public TLAbsStatsGraph getFollowersGraph() {
        return followersGraph;
    }

    public void setFollowersGraph(TLAbsStatsGraph followersGraph) {
        this.followersGraph = followersGraph;
    }

    public TLAbsStatsGraph getMuteGraph() {
        return muteGraph;
    }

    public void setMuteGraph(TLAbsStatsGraph muteGraph) {
        this.muteGraph = muteGraph;
    }

    public TLAbsStatsGraph getTopHoursGraph() {
        return topHoursGraph;
    }

    public void setTopHoursGraph(TLAbsStatsGraph topHoursGraph) {
        this.topHoursGraph = topHoursGraph;
    }

    public TLAbsStatsGraph getInteractionsGraph() {
        return interactionsGraph;
    }

    public void setInteractionsGraph(TLAbsStatsGraph interactionsGraph) {
        this.interactionsGraph = interactionsGraph;
    }

    public TLAbsStatsGraph getIvInteractionsGraph() {
        return ivInteractionsGraph;
    }

    public void setIvInteractionsGraph(TLAbsStatsGraph ivInteractionsGraph) {
        this.ivInteractionsGraph = ivInteractionsGraph;
    }

    public TLAbsStatsGraph getViewsBySourceGraph() {
        return viewsBySourceGraph;
    }

    public void setViewsBySourceGraph(TLAbsStatsGraph viewsBySourceGraph) {
        this.viewsBySourceGraph = viewsBySourceGraph;
    }

    public TLAbsStatsGraph getNewFollowersBySourceGraph() {
        return newFollowersBySourceGraph;
    }

    public void setNewFollowersBySourceGraph(TLAbsStatsGraph newFollowersBySourceGraph) {
        this.newFollowersBySourceGraph = newFollowersBySourceGraph;
    }

    public TLAbsStatsGraph getLanguagesGraph() {
        return languagesGraph;
    }

    public void setLanguagesGraph(TLAbsStatsGraph languagesGraph) {
        this.languagesGraph = languagesGraph;
    }

    public TLVector<TLMessageInteractionCounters> getRecentMessageInteractions() {
        return recentMessageInteractions;
    }

    public void setRecentMessageInteractions(TLVector<TLMessageInteractionCounters> recentMessageInteractions) {
        this.recentMessageInteractions = recentMessageInteractions;
    }
}
