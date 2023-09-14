package com.cleanroommc.flare.common.component.memory.heap.gc;

import com.cleanroommc.flare.api.heap.gc.GarbageCollector;

import javax.annotation.Nonnull;

public class GarbageCollectorInfo implements GarbageCollector {

    private final String name;
    private final long collections;
    private final long time;
    private final double averageTime;
    private final long averageFrequency;

    public GarbageCollectorInfo(String name, GarbageCollectorStatistics stats, long serverUptime) {
        this.name = name;
        this.collections = stats.getCollectionCount();
        this.time = stats.getCollectionTime();
        this.averageTime = stats.getAverageCollectionTime();
        this.averageFrequency = stats.getAverageCollectionFrequency(serverUptime);
    }

    @Nonnull
    @Override
    public String name() {
        return name;
    }

    @Override
    public long collections() {
        return collections;
    }

    @Override
    public long time() {
        return time;
    }

    @Override
    public double averageTime() {
        return averageTime;
    }

    @Override
    public long averageFrequency() {
        return averageFrequency;
    }

}
