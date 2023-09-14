package com.cleanroommc.flare.api.sampler.window;

import java.util.function.IntPredicate;

public final class ProfilingWindowUtils {

    /**
     * The size/duration of a profiling window in seconds.
     * (1 window = 1 minute)
     */
    public static final int WINDOW_SIZE_SECONDS = 10;

    /**
     * The number of windows to record in continuous profiling before data is dropped.
     * (60 windows * 1 minute = 1 hour of profiling data)
     */
    public static final int HISTORY_SIZE = Integer.getInteger("flare.continuousProfilingHistorySize", 60);

    /**
     * Gets the profiling window for the given time in unix-millis.
     *
     * @param time the time in milliseconds
     * @return the window
     */
    public static int unixMillisToWindow(long time) {
        return (int) (time / (WINDOW_SIZE_SECONDS * 1000L));
    }

    /**
     * Gets the window at the current time.
     *
     * @return the window
     */
    public static int windowNow() {
        return unixMillisToWindow(System.currentTimeMillis());
    }

    /**
     * Gets a prune predicate that can be passed to
     *  {@link com.cleanroommc.flare.common.sampler.aggregator.DataAggregator#pruneData(IntPredicate)}.
     *
     * @return the prune predicate
     */
    public static IntPredicate keepHistoryBefore(int currentWindow) {
        // Windows that were earlier than (currentWindow minus history size) should be pruned
        return window -> window < (currentWindow - HISTORY_SIZE);
    }

    private ProfilingWindowUtils() { }

}
