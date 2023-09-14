package com.cleanroommc.flare.common.component.memory.heap.gc;

import com.google.common.collect.ImmutableMap;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Map;

/**
 * Holder for {@link GarbageCollectorMXBean} statistics.
 */
public class GarbageCollectorStatistics {

    public static final GarbageCollectorStatistics ZERO = new GarbageCollectorStatistics(0, 0);

    /**
     * Polls a set of statistics from the {@link GarbageCollectorMXBean}.
     *
     * @return the polled statistics
     */
    public static Map<String, GarbageCollectorStatistics> pollStats() {
        ImmutableMap.Builder<String, GarbageCollectorStatistics> stats = ImmutableMap.builder();
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            stats.put(bean.getName(), new GarbageCollectorStatistics(bean));
        }
        return stats.build();
    }

    /**
     * Polls a set of statistics from the {@link GarbageCollectorMXBean}, then subtracts
     * {@code initial} from them.
     *
     * <p>The reason for subtracting the initial statistics is to ignore GC activity
     * that took place before the server/client fully started.</p>
     *
     * @return the polled statistics
     */
    public static Map<String, GarbageCollectorStatistics> pollStatsSubtractInitial(Map<String, GarbageCollectorStatistics> initial) {
        ImmutableMap.Builder<String, GarbageCollectorStatistics> stats = ImmutableMap.builder();
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            stats.put(bean.getName(), new GarbageCollectorStatistics(bean).subtract(initial.getOrDefault(bean.getName(), ZERO)));
        }
        return stats.build();
    }

    private final long collectionCount;
    private final long collectionTime;

    public GarbageCollectorStatistics(long collectionCount, long collectionTime) {
        this.collectionCount = collectionCount;
        this.collectionTime = collectionTime;
    }

    public GarbageCollectorStatistics(GarbageCollectorMXBean bean) {
        this(bean.getCollectionCount(), bean.getCollectionTime());
    }

    // All times in milliseconds

    public long getCollectionCount() {
        return this.collectionCount;
    }

    public long getCollectionTime() {
        return this.collectionTime;
    }

    public double getAverageCollectionTime() {
        return this.collectionCount == 0 ? 0 : (double) this.collectionTime / this.collectionCount;
    }

    public long getAverageCollectionFrequency(long serverUptime) {
        return this.collectionCount == 0 ? 0 : (long) ((serverUptime - (double) this.collectionTime) / this.collectionCount);
    }

    public GarbageCollectorStatistics subtract(GarbageCollectorStatistics other) {
        if (other == ZERO || (other.collectionCount == 0 && other.collectionTime == 0)) {
            return this;
        }
        return new GarbageCollectorStatistics(this.collectionCount - other.collectionCount, this.collectionTime - other.collectionTime);
    }
}
