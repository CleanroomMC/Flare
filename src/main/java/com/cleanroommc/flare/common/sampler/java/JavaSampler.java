package com.cleanroommc.flare.common.sampler.java;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.api.sampler.SamplerMode;
import com.cleanroommc.flare.api.sampler.thread.ThreadDumper;
import com.cleanroommc.flare.api.sampler.thread.ThreadGrouper;
import com.cleanroommc.flare.api.sampler.window.ProfilingWindowUtils;
import com.cleanroommc.flare.api.tick.TickRoutine;
import com.cleanroommc.flare.common.sampler.AbstractSampler;
import com.cleanroommc.flare.common.sampler.ExportProps;
import com.cleanroommc.flare.common.sampler.window.WindowStatisticsCollector.ExplicitTickCounter;
import com.cleanroommc.flare.common.websocket.ViewerSocket;
import com.cleanroommc.flare.proto.FlareSamplerProtos.SamplerData;
import com.cleanroommc.flare.util.FlareThreadFactory;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntPredicate;

public class JavaSampler extends AbstractSampler implements Runnable {

    /** The thread management interface for the current JVM */
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    /** Responsible for aggregating and then outputting collected sampling data */
    private final JavaDataAggregator dataAggregator;
    /** The last window that was profiled */
    private final AtomicInteger lastWindow = new AtomicInteger();

    /** The worker pool for inserting stack nodes */
    ScheduledExecutorService workerPool = service();
    /** The worker for dealing with socket connections */
    ScheduledExecutorService socketService = socketService();
    /** The main sampling task */
    private ScheduledFuture<?> samplingTask;
    /** The task to send statistics to the viewer socket */
    private ScheduledFuture<?> socketStatisticsTask;

    public JavaSampler(FlareAPI flare, Side side, int interval, ThreadDumper threadDumper, long endTime, boolean runningInBackground,
                       ThreadGrouper grouper, boolean ignoreSleeping, boolean ignoreNative) {
        super(flare, side, interval, threadDumper, endTime, runningInBackground);
        this.dataAggregator = JavaDataAggregator.simple(this, grouper, interval, ignoreSleeping, ignoreNative);
    }

    public JavaSampler(FlareAPI flare, Side side, int interval, ThreadDumper threadDumper, long endTime, boolean runningInBackground,
                       boolean ignoreSleeping, boolean ignoreNative, ThreadGrouper threadGrouper, TickRoutine tickRoutine, int tickLengthThreshold) {
        super(flare, side, interval, threadDumper, endTime, runningInBackground);
        this.dataAggregator = JavaDataAggregator.ticked(this, threadGrouper, interval, ignoreSleeping, ignoreNative,
                tickRoutine, tickLengthThreshold);
    }

    @Override
    public SamplerMode mode() {
        return SamplerMode.EXECUTION;
    }

    @Override
    protected void startWork() {
        TickRoutine tickRoutine = this.flare.tickRoutine(this.side);
        if (tickRoutine != null) {
            if (this.dataAggregator.ticked()) {
                ExplicitTickCounter counter = this.windowStatisticsCollector.startCountingTicksExplicit(tickRoutine);
                ((JavaDataAggregator.Ticked) this.dataAggregator).setTickCounter(counter);
            } else {
                this.windowStatisticsCollector.startCountingTicks(tickRoutine);
            }
        }
        this.windowStatisticsCollector.recordWindowStartTime(ProfilingWindowUtils.unixMillisToWindow(this.startTime));
        this.samplingTask = this.workerPool.scheduleAtFixedRate(this, 0, this.interval, TimeUnit.MICROSECONDS);
    }

    @Override
    protected void stopWork(boolean cancelled) {
        this.samplingTask.cancel(false);
        if (this.socketStatisticsTask != null) {
            this.socketStatisticsTask.cancel(false);
        }
        if (!cancelled) {
            // Collect statistics for the final window
            this.windowStatisticsCollector.measureNow(this.lastWindow.get());
        }
        this.workerPool.shutdown();
    }

    @Override
    public void attachSocket(ViewerSocket socket) {
        super.attachSocket(socket);
        if (this.socketStatisticsTask == null) {
            this.socketStatisticsTask = this.socketService.scheduleAtFixedRate(this::sendStatisticsToSocket, 0, 10, TimeUnit.SECONDS);
        }
    }

    @Override
    public SamplerData toProto(FlareAPI flare, ExportProps exportProps, boolean stop) {
        // Collect statistics for the final window
        this.windowStatisticsCollector.measureNow(this.lastWindow.get());

        SamplerData.Builder builder = SamplerData.newBuilder();

        if (exportProps.channelInfo() != null) {
            builder.setChannelInfo(exportProps.channelInfo());
        }

        writeMetadataToProto(builder, exportProps, this.dataAggregator);

        // Wait for all pending data to be inserted
        this.stopService();

        writeDataToProto(builder, this.dataAggregator, exportProps);

        if (!stop) {
            this.resumeService();
        }

        return builder.build();
    }

    @Override
    public void run() {
        try {
            long time = System.currentTimeMillis();
            if (this.autoEndTime != -1 && this.autoEndTime <= time) {
                stop(false);
                this.future.complete(this);
                return;
            }
            int window = ProfilingWindowUtils.unixMillisToWindow(time);
            ThreadInfo[] threadDumps = this.threadDumper.dump(this.threadBean);
            this.workerPool.execute(new GatherDataTask(threadDumps, window));
        } catch (Throwable t) {
            stop(false);
            this.future.completeExceptionally(t);
        }
    }

    void stopService() {
        this.workerPool.shutdown();
        try {
            this.workerPool.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void resumeService() {
        this.workerPool = service();
        this.samplingTask = samplingTask();
    }

    // TODO configure thread count
    private ScheduledExecutorService service() {
        return Executors.newScheduledThreadPool(6, new FlareThreadFactory(FlareAPI.getInstance(), "flare-java-sampler"));
    }

    private ScheduledExecutorService socketService() {
        return Executors.newSingleThreadScheduledExecutor(new FlareThreadFactory(FlareAPI.getInstance(), "flare-viewer"));
    }

    private ScheduledFuture<?> samplingTask() {
        return this.workerPool.scheduleAtFixedRate(this, 0, this.interval, TimeUnit.MICROSECONDS);
    }

    private final class GatherDataTask implements Runnable {

        private final JavaSampler $;
        private final ThreadInfo[] threadDumps;
        private final int window;

        private GatherDataTask(ThreadInfo[] threadDumps, int window) {
            this.$ = JavaSampler.this;
            this.threadDumps = threadDumps;
            this.window = window;
        }

        @Override
        public void run() {
            for (ThreadInfo threadInfo : this.threadDumps) {
                if (threadInfo.getThreadName() == null || threadInfo.getStackTrace() == null) {
                    continue;
                }
                $.dataAggregator.insertData(threadInfo, this.window); // TODO
            }
            // If we have just stepped over into a new window...
            int previousWindow = $.lastWindow.getAndUpdate(previous -> Math.max(this.window, previous));
            if (previousWindow != 0 && previousWindow != this.window) {
                // Record the start time for the new window
                $.windowStatisticsCollector.recordWindowStartTime(this.window);
                // Collect statistics for the previous window
                $.windowStatisticsCollector.measureNow(previousWindow);
                // Prune data older than the history size
                IntPredicate predicate = ProfilingWindowUtils.keepHistoryBefore(this.window);
                $.dataAggregator.pruneData(predicate);
                $.windowStatisticsCollector.pruneStatistics(predicate);
                $.workerPool.execute($::processWindowRotate);
            }
        }
        
    }

}
