package com.cleanroommc.flare.common.sampler.aggregator;

import com.cleanroommc.flare.api.sampler.node.type.ThreadNode;
import com.cleanroommc.flare.api.sampler.thread.ThreadGrouper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.IntPredicate;

/**
 * Abstract implementation of {@link DataAggregator}.
 */
public abstract class AbstractDataAggregator implements DataAggregator {

    /** A map of root stack nodes for each thread with sampling data */
    protected final ConcurrentMap<String, ThreadNode> threadData = new ConcurrentHashMap<>();

    /** The instance used to group threads together */
    protected final ThreadGrouper threadGrouper;

    protected AbstractDataAggregator(ThreadGrouper threadGrouper) {
        this.threadGrouper = threadGrouper;
    }

    protected ThreadNode getNode(String group) {
        return this.threadData.computeIfAbsent(group, ThreadNode::new);
    }

    @Override
    public void pruneData(IntPredicate timeWindowPredicate) {
        this.threadData.values().removeIf(node -> node.removeTimeWindowsRecursively(timeWindowPredicate));
    }

    @Override
    public List<ThreadNode> exportData() {
        List<ThreadNode> data = new ArrayList<>(this.threadData.values());
        for (ThreadNode node : data) {
            node.setLabel(this.threadGrouper.label(node.group()));
        }
        return data;
    }

}
