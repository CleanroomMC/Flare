package com.cleanroommc.flare.common.sampler.window;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.api.sampler.window.ProfilingWindowUtils;
import com.cleanroommc.flare.api.tick.TickRoutine;
import com.cleanroommc.flare.api.tick.TickStatistics;
import com.cleanroommc.flare.api.tick.TickType;
import com.cleanroommc.flare.api.util.DoubleAverageInfo;
import com.cleanroommc.flare.common.component.cpu.CpuMonitor;
import com.cleanroommc.flare.proto.FlareProtos;
import net.minecraftforge.fml.relauncher.Side;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntPredicate;

/**
 * Collects statistics for each profiling window.
 */
public class WindowStatisticsCollector {

    private static final FlareProtos.WindowStatistics ZERO = FlareProtos.WindowStatistics.newBuilder()
            .setDuration(ProfilingWindowUtils.WINDOW_SIZE_SECONDS * 1000)
            .build();

    /** The platform */
    private final FlareAPI flare;
    private final Side side;
    /** Map of profiling window -> start time */
    private final Map<Integer, Long> windowStartTimes = new HashMap<>();
    /** Map of profiling window -> statistics */
    private final Map<Integer, FlareProtos.WindowStatistics> stats;

    private TickCounter tickCounter;

    public WindowStatisticsCollector(FlareAPI flare, Side side) {
        this.flare = flare;
        this.side = side;
        this.stats = new ConcurrentHashMap<>();
    }

    /**
     * Indicates to the statistics collector that it should count the number
     * of ticks in each window using the provided {@link TickRoutine}.
     *
     * @param hook the tick hook
     */
    public void startCountingTicks(TickRoutine hook) {
        this.tickCounter = new NormalTickCounter(this.flare, hook);
    }

    /**
     * Indicates to the statistics collector that it should count the number
     * of ticks in each window, according to how many times the
     * {@link ExplicitTickCounter#increment()} method is called.
     *
     * @param hook the tick hook
     * @return the counter
     */
    public ExplicitTickCounter startCountingTicksExplicit(TickRoutine hook) {
        ExplicitTickCounter counter = new ExplicitTickCounter(this.flare, hook);
        this.tickCounter = counter;
        return counter;
    }

    public void stop() {
        if (this.tickCounter != null) {
            this.tickCounter.stop();
        }
    }

    /**
     * Gets the total number of ticks that have passed between the time
     * when the profiler started and stopped.
     *
     * <p>Importantly, note that this metric is different to the total number of ticks in a window
     * (which is recorded by {@link FlareProtos.WindowStatistics#getTicks()}) or the total number
     * of observed ticks if the 'only-ticks-over' aggregator is being used
     * (which is recorded by {@link FlareProtos.WindowStatistics#getTicks()}
     * and {@link ExplicitTickCounter#getTotalCountedTicks()}.</p>
     *
     * @return the total number of ticks in the profile
     */
    public int getTotalTicks() {
        return this.tickCounter == null ? -1 : this.tickCounter.getTotalTicks();
    }

    /**
     * Records the wall-clock time when a window was started.
     *
     * @param window the window
     */
    public void recordWindowStartTime(int window) {
        this.windowStartTimes.put(window, System.currentTimeMillis());
    }

    /**
     * Measures statistics for the given window if none have been recorded yet.
     *
     * @param window the window
     */
    public void measureNow(int window) {
        this.stats.computeIfAbsent(window, this::measure);
    }

    /**
     * Ensures that the exported map has statistics (even if they are zeroed) for all windows.
     *
     * @param windows the expected windows
     */
    public void ensureHasStatisticsForAllWindows(int[] windows) {
        for (int window : windows) {
            this.stats.computeIfAbsent(window, w -> ZERO);
        }
    }

    public void pruneStatistics(IntPredicate predicate) {
        this.stats.keySet().removeIf(predicate::test);
    }

    public Map<Integer, FlareProtos.WindowStatistics> export() {
        return this.stats;
    }

    /**
     * Measures current statistics, where possible averaging over the last minute. (1 min = 1 window)
     *
     * @return the current statistics
     */
    private FlareProtos.WindowStatistics measure(int window) {
        FlareProtos.WindowStatistics.Builder builder = FlareProtos.WindowStatistics.newBuilder();

        long endTime = System.currentTimeMillis();
        Long startTime = this.windowStartTimes.get(window);
        if (startTime == null) {
            this.flare.logger().warn("Unknown start time for window {}", window);
            startTime = endTime - (ProfilingWindowUtils.WINDOW_SIZE_SECONDS * 1000); // Guess
        }

        builder.setStartTime(startTime);
        builder.setEndTime(endTime);
        builder.setDuration((int) (endTime - startTime));

        // this.flare.logger().warn("Window: {} | End Time: {} | Start Time: {} | Duration: {}", window, endTime, startTime, (int) (endTime - startTime));

        TickStatistics tickStatistics = this.flare.tickStatistics(side, TickType.ALL);
        if (tickStatistics != null) {
            builder.setTps(tickStatistics.tps1Min());

            DoubleAverageInfo mspt = tickStatistics.duration1Min();
            if (mspt != null) {
                builder.setMsptMedian(mspt.median());
                builder.setMsptMax(mspt.max());
            }
        }

        if (this.tickCounter != null) {
            int ticks = this.tickCounter.getCountedTicksThisWindowAndReset();
            builder.setTicks(ticks);
        }

        builder.setCpuProcess(CpuMonitor.processLoad1MinAvg());
        builder.setCpuSystem(CpuMonitor.systemLoad1MinAvg());

        if (this.flare.server() != null) {
            builder.setPlayers(this.flare.players().size());
            builder.setEntities(this.flare.entities().size());
            builder.setTileEntities(this.flare.tileEntities().size());
            builder.setChunks(this.flare.chunks().size());
        }

        return builder.build();
    }

    /**
     * Responsible for counting the number of ticks in a profile/window.
     */
    public interface TickCounter {

        /**
         * Stop the counter.
         */
        void stop();

        /**
         * Get the total number of ticks.
         *
         * <p>See {@link WindowStatisticsCollector#getTotalTicks()} for a longer explanation
         * of what this means exactly.</p>
         *
         * @return the total ticks
         */
        int getTotalTicks();

        /**
         * Gets the total number of ticks counted in the last window,
         * and resets the counter to zero.
         *
         * @return the number of ticks counted since the last time this method was called
         */
        int getCountedTicksThisWindowAndReset();
    }

    private static abstract class BaseTickCounter implements TickCounter {

        protected final FlareAPI flare;
        protected final TickRoutine tickHook;

        /** The game tick when sampling first began */
        private final int startTick;

        /** The game tick when sampling stopped */
        private int stopTick = -1;

        BaseTickCounter(FlareAPI flare, TickRoutine tickHook) {
            this.flare = flare;
            this.tickHook = tickHook;
            this.startTick = this.tickHook.currentTick();
        }

        @Override
        public void stop() {
            this.stopTick = this.tickHook.currentTick();
        }

        @Override
        public int getTotalTicks() {
            if (this.startTick == -1) {
                throw new IllegalStateException("start tick not recorded");
            }

            int stopTick = this.stopTick;
            if (stopTick == -1) {
                stopTick = this.tickHook.currentTick();
            }

            return stopTick - this.startTick;
        }

    }

    /**
     * Counts the number of ticks in a window using a {@link TickRoutine}.
     */
    public static final class NormalTickCounter extends BaseTickCounter {

        private int last;

        NormalTickCounter(FlareAPI flare, TickRoutine tickHook) {
            super(flare, tickHook);
            this.last = this.tickHook.currentTick();
        }

        @Override
        public int getCountedTicksThisWindowAndReset() {
            synchronized (this) {
                int now = this.tickHook.currentTick();
                int ticks = now - this.last;
                this.last = now;
                return ticks;
            }
        }

    }

    /**
     * Counts the number of ticks in a window according to the number of times
     * {@link #increment()} is called.
     *
     * Used by the {@link com.cleanroommc.flare.common.sampler.java.JavaDataAggregator.Ticked}.
     */
    public static final class ExplicitTickCounter extends BaseTickCounter {

        private final AtomicInteger counted = new AtomicInteger();
        private final AtomicInteger total = new AtomicInteger();

        ExplicitTickCounter(FlareAPI flare, TickRoutine tickHook) {
            super(flare, tickHook);
        }

        public void increment() {
            this.counted.incrementAndGet();
            this.total.incrementAndGet();
        }

        public int getTotalCountedTicks() {
            return this.total.get();
        }

        @Override
        public int getCountedTicksThisWindowAndReset() {
            return this.counted.getAndSet(0);
        }

    }

}
