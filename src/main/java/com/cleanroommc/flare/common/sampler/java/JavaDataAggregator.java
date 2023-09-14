package com.cleanroommc.flare.common.sampler.java;

import com.cleanroommc.flare.common.sampler.aggregator.AbstractDataAggregator;
import com.cleanroommc.flare.api.sampler.node.description.NodeDescriber;
import com.cleanroommc.flare.api.sampler.node.description.NodeDescription;
import com.cleanroommc.flare.api.sampler.node.type.SamplingStackNode;
import com.cleanroommc.flare.api.sampler.node.type.ThreadNode;
import com.cleanroommc.flare.api.sampler.thread.ThreadGrouper;
import com.cleanroommc.flare.api.tick.TickRoutine;
import com.cleanroommc.flare.common.sampler.window.WindowStatisticsCollector;
import com.cleanroommc.flare.proto.FlareSamplerProtos.SamplerMetadata;
import com.cleanroommc.flare.util.ProtoUtil;

import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class JavaDataAggregator extends AbstractDataAggregator {

    public static JavaDataAggregator simple(JavaSampler sampler, ThreadGrouper threadGrouper, int interval,
                                            boolean ignoreSleeping, boolean ignoreNative) {
        return new JavaDataAggregator(sampler, threadGrouper, interval, ignoreSleeping, ignoreNative);
    }

    public static JavaDataAggregator ticked(JavaSampler sampler, ThreadGrouper threadGrouper, int interval,
                                            boolean ignoreSleeping, boolean ignoreNative, TickRoutine tickRoutine, int tickLengthThreshold) {
        return new Ticked(sampler, threadGrouper, interval, ignoreSleeping, ignoreNative, tickRoutine, tickLengthThreshold);
    }

    /** A describer for java stack trace elements. */
    private static final NodeDescriber<StackTraceElement> DESCRIBER = (element, parent) -> {
        int parentLineNumber = parent == null ? SamplingStackNode.NULL_LINE_NUMBER : parent.getLineNumber();
        return new NodeDescription(element.getClassName(), element.getMethodName(), element.getLineNumber(), parentLineNumber);
    };

    // TODO: may be version/vendor dependent
    private static boolean isSleeping(ThreadInfo thread) {
        if (thread.getThreadState() == Thread.State.WAITING || thread.getThreadState() == Thread.State.TIMED_WAITING) {
            return true;
        }
        StackTraceElement[] stackTrace = thread.getStackTrace();
        if (stackTrace.length == 0) {
            return false;
        }
        StackTraceElement call = stackTrace[0];
        String clazz = call.getClassName();
        String method = call.getMethodName();
        // java.lang.Thread.yield()
        // jdk.internal.misc.Unsafe.park()
        // sun.misc.Unsafe.park()
        return ("park".equals(method) && ("sun.misc.Unsafe".equals(clazz) || "jdk.internal.misc.Unsafe".equals(clazz))) ||
                ("yield".equals(method) && ("java.lang.Thread".equals(clazz)));
    }

    /** The sampler */
    protected final JavaSampler sampler;
    /** The interval to wait between sampling, in microseconds */
    protected final int interval;
    /** If sleeping threads should be ignored */
    private final boolean ignoreSleeping;
    /** If threads executing native code should be ignored */
    private final boolean ignoreNative;

    private JavaDataAggregator(JavaSampler sampler, ThreadGrouper threadGrouper, int interval, boolean ignoreSleeping, boolean ignoreNative) {
        super(threadGrouper);
        this.sampler = sampler;
        this.interval = interval;
        this.ignoreSleeping = ignoreSleeping;
        this.ignoreNative = ignoreNative;
    }

    public boolean ticked() {
        return false;
    }

    /**
     * Inserts sampling data into this aggregator
     *
     * @param threadInfo the thread info
     * @param window the window
     */
    public void insertData(ThreadInfo threadInfo, int window) {
        writeData(threadInfo, window);
    }

    @Override
    public SamplerMetadata.DataAggregator toProto() {
        return SamplerMetadata.DataAggregator.newBuilder()
                .setType(SamplerMetadata.DataAggregator.Type.SIMPLE)
                .setThreadGrouper(ProtoUtil.getThreadGrouperProto(this.threadGrouper))
                .build();
    }

    protected void writeData(ThreadInfo threadInfo, int window) {
        if (this.ignoreSleeping && isSleeping(threadInfo)) {
            return;
        }
        if (this.ignoreNative && threadInfo.isInNative()) {
            return;
        }
        try {
            ThreadNode node = getNode(this.threadGrouper.group(threadInfo.getThreadId(), threadInfo.getThreadName()));
            node.trace(DESCRIBER, threadInfo.getStackTrace(), this.interval, window);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Implementation of {@link JavaDataAggregator} which supports only including sampling data from "ticks"
     * which exceed a certain threshold in duration.
     */
    static class Ticked extends JavaDataAggregator {

        /** Used to monitor the current "tick" of the server */
        private final TickRoutine tickRoutine;

        /** Tick durations under this threshold will not be inserted, measured in microseconds */
        private final long tickLengthThreshold;

        /** The expected number of samples in each tick */
        private final int expectedSize;

        /** Counts the number of ticks aggregated */
        private WindowStatisticsCollector.ExplicitTickCounter tickCounter;

        // State
        private int currentTick = -1;
        private TickList currentData = null;

        // currentData mutex
        private final Object mutex = new Object();

        private Ticked(JavaSampler sampler, ThreadGrouper threadGrouper, int interval, boolean ignoreSleeping,
                       boolean ignoreNative, TickRoutine tickRoutine, int tickLengthThreshold) {
            super(sampler, threadGrouper, interval, ignoreSleeping, ignoreNative);
            this.tickRoutine = tickRoutine;
            this.tickLengthThreshold = TimeUnit.MILLISECONDS.toMicros(tickLengthThreshold);
            // 50 millis in a tick, + 10 so we have a bit of room to go over
            double intervalMilliseconds = interval / 1000D;
            this.expectedSize = (int) ((50 / intervalMilliseconds) + 10);
        }

        @Override
        public boolean ticked() {
            return true;
        }

        @Override
        public void insertData(ThreadInfo threadInfo, int window) {
            synchronized (this.mutex) {
                int tick = this.tickRoutine.currentTick();
                if (this.currentTick != tick || this.currentData == null) {
                    pushCurrentTick(sampler.workerPool);
                    this.currentTick = tick;
                    this.currentData = new TickList(this.expectedSize, window);
                }
                this.currentData.addData(threadInfo);
            }
        }

        @Override
        public List<ThreadNode> exportData() {
            // Push the current tick
            synchronized (this.mutex) {
                pushCurrentTick(Runnable::run);
            }
            return super.exportData();
        }

        @Override
        public SamplerMetadata.DataAggregator toProto() {
            // Push the current tick (so numberOfTicks is accurate)
            synchronized (this.mutex) {
                pushCurrentTick(Runnable::run);
                this.currentData = null;
            }
            return SamplerMetadata.DataAggregator.newBuilder()
                    .setType(SamplerMetadata.DataAggregator.Type.TICKED)
                    .setThreadGrouper(ProtoUtil.getThreadGrouperProto(this.threadGrouper))
                    .setTickLengthThreshold(this.tickLengthThreshold)
                    .setNumberOfIncludedTicks(this.tickCounter.getTotalCountedTicks())
                    .build();
        }

        public void setTickCounter(WindowStatisticsCollector.ExplicitTickCounter tickCounter) {
            this.tickCounter = tickCounter;
        }

        private void pushCurrentTick(Executor executor) {
            TickList currentData = this.currentData;
            if (currentData == null) {
                return;
            }
            // Approximate how long the tick lasted
            int tickLengthMicros = currentData.getList().size() * this.interval;
            // Don't push data below the threshold
            if (tickLengthMicros < this.tickLengthThreshold) {
                return;
            }
            executor.execute(currentData);
            this.tickCounter.increment();
        }

        private final class TickList implements Runnable {

            private final List<ThreadInfo> list;
            private final int window;

            TickList(int expectedSize, int window) {
                this.list = new ArrayList<>(expectedSize);
                this.window = window;
            }

            @Override
            public void run() {
                for (ThreadInfo data : this.list) {
                    writeData(data, this.window);
                }
            }

            public List<ThreadInfo> getList() {
                return this.list;
            }

            public void addData(ThreadInfo data) {
                this.list.add(data);
            }

        }

    }

}
