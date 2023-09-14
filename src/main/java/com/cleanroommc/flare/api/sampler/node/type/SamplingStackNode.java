package com.cleanroommc.flare.api.sampler.node.type;

import com.cleanroommc.flare.api.sampler.node.description.NodeDescription;
import com.cleanroommc.flare.common.sampler.window.ProtoTimeEncoder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.IntPredicate;

/**
 * Encapsulates a timed node in the sampling stack.
 */
public abstract class SamplingStackNode {

    /**
     * Magic number to denote "no present" line number for a node.
     */
    public static final int NULL_LINE_NUMBER = -1;

    public static int maxStackDepth(int defaultValue) {
        return Integer.getInteger("flare.maxStackDepth", defaultValue);
    }

    public static int maxStackDepth() {
        return maxStackDepth(300);
    }

    /** A map of the nodes children */
    protected final ConcurrentMap<NodeDescription, SamplingStackNode> children = new ConcurrentHashMap<>();
    /** The accumulated sample time for this node, measured in microseconds */
    // Integer key = the window (effectively System.currentTimeMillis() / 60_000)
    // LongAdder value = accumulated time in microseconds
    protected final ConcurrentMap<Integer, LongAdder> times = new ConcurrentHashMap<>();

    protected abstract SamplingStackNode resolveChild(NodeDescription description);

    /**
     * Gets the time accumulator for a given window
     *
     * @param window the window
     * @return the accumulator
     */
    protected LongAdder timeAccumulator(int window) {
        LongAdder adder = this.times.get(window);
        if (adder == null) {
            this.times.put(window, adder = new LongAdder());
        }
        return adder;
    }


    /**
     * Gets the time windows that have been logged for this node.
     *
     * @return the time windows
     */
    public Set<Integer> timeWindows() {
        return this.times.keySet();
    }

    /**
     * Removes time windows from this node if they pass the given {@code predicate} test.
     *
     * @param predicate the predicate
     * @return true if any time windows were removed
     */
    public boolean removeTimeWindows(IntPredicate predicate) {
        return this.times.keySet().removeIf(predicate::test);
    }

    public Collection<SamplingStackNode> children() {
        return this.children.values();
    }

    /**
     * Gets the encoded total sample times logged for this node in milliseconds.
     *
     * @return the total times
     */
    public double[] encodeTimesForProto(ProtoTimeEncoder encoder) {
        return encoder.encode(this.times);
    }

    /**
     * Merge {@code other} into {@code this}.
     *
     * @param other the other node
     */
    protected void merge(SamplingStackNode other) {
        other.times.forEach((k, v) -> timeAccumulator(k).add(v.longValue()));
        other.children.forEach((k, v) -> resolveChild(k).merge(v));
    }

}
