package com.cleanroommc.flare.common.sampler.async;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.api.sampler.Sampler;
import com.cleanroommc.flare.api.sampler.SamplerMode;
import com.cleanroommc.flare.api.sampler.thread.ThreadDumper;
import com.cleanroommc.flare.api.sampler.thread.ThreadGrouper;
import com.cleanroommc.flare.api.sampler.window.ProfilingWindowUtils;
import com.cleanroommc.flare.api.tick.TickRoutine;
import com.cleanroommc.flare.common.sampler.AbstractSampler;
import com.cleanroommc.flare.common.sampler.ExportProps;
import com.cleanroommc.flare.common.websocket.ViewerSocket;
import com.cleanroommc.flare.proto.FlareSamplerProtos.SamplerData;
import com.cleanroommc.flare.util.FlareThreadFactory;
import net.minecraftforge.fml.relauncher.Side;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.IntPredicate;

/**
 * A sampler implementation using async-profiler.
 */
public class AsyncSampler extends AbstractSampler {

    /** Function to collect and measure samples - either execution or allocation */
    private final SampleCollector<?> sampleCollector;
    /** Object that provides access to the async-profiler API */
    private final AsyncProfilerAccess profilerAccess;
    /** Responsible for aggregating and then outputting collected sampling data */
    private final AsyncDataAggregator dataAggregator;
    /** Mutex for the current profiler job */
    private final Object[] currentJobMutex = new Object[0];

    /** Current profiler job */
    private AsyncProfilerJob currentJob;
    /** The executor used for scheduling and management */
    private ScheduledExecutorService scheduler;
    /** The task to send statistics to the viewer socket */
    private ScheduledFuture<?> socketStatisticsTask;

    public AsyncSampler(FlareAPI flare, Side side, ThreadGrouper threadGrouper, SampleCollector<?> collector, int interval,
                        ThreadDumper threadDumper, long endTime, boolean runningInBackground) {
        super(flare, side, interval, threadDumper, endTime, runningInBackground);
        this.sampleCollector = collector;
        this.profilerAccess = AsyncProfilerAccess.getInstance(flare);
        this.dataAggregator = new AsyncDataAggregator(threadGrouper);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new FlareThreadFactory(flare, "flare-async-sampler"));
    }

    @Override
    public SamplerMode mode() {
        return this.sampleCollector.mode();
    }

    /**
     * Starts the profiler.
     */
    @Override
    public void startWork() {
        TickRoutine tickHook = this.flare.tickRoutine(this.side);
        if (tickHook != null) {
            this.windowStatisticsCollector.startCountingTicks(tickHook);
        }

        int window = ProfilingWindowUtils.windowNow();

        AsyncProfilerJob job = this.profilerAccess.startNewProfilerJob();
        job.init(this.flare, this.sampleCollector, this.threadDumper, window, this.background);
        job.start();
        this.windowStatisticsCollector.recordWindowStartTime(window);
        this.currentJob = job;

        // Rotate the sampler job to put data into a new window
        boolean shouldNotRotate = this.sampleCollector instanceof SampleCollector.Allocation &&
                ((SampleCollector.Allocation) this.sampleCollector).isLiveOnly();
        if (!shouldNotRotate) {
            this.scheduler.scheduleAtFixedRate(
                    this::rotateProfilerJob,
                    ProfilingWindowUtils.WINDOW_SIZE_SECONDS,
                    ProfilingWindowUtils.WINDOW_SIZE_SECONDS,
                    TimeUnit.SECONDS);
        }

        recordInitialGcStats();
        scheduleTimeout();
    }

    private void rotateProfilerJob() {
        try {
            synchronized (this.currentJobMutex) {
                AsyncProfilerJob previousJob = this.currentJob;
                if (previousJob == null) {
                    return;
                }
                try {
                    // Stop the previous job
                    previousJob.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Start a new job
                int window = previousJob.getWindow() + 1;
                AsyncProfilerJob newJob = this.profilerAccess.startNewProfilerJob();
                newJob.init(this.flare, this.sampleCollector, this.threadDumper, window, this.background);
                newJob.start();
                this.windowStatisticsCollector.recordWindowStartTime(window);
                this.currentJob = newJob;
                // Collect statistics for the previous window
                try {
                    this.windowStatisticsCollector.measureNow(previousJob.getWindow());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Aggregate the output of the previous job
                previousJob.aggregate(this.dataAggregator);
                // Prune data older than the history size
                IntPredicate predicate = ProfilingWindowUtils.keepHistoryBefore(window);
                this.dataAggregator.pruneData(predicate);
                this.windowStatisticsCollector.pruneStatistics(predicate);
                this.scheduler.execute(this::processWindowRotate);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void scheduleTimeout() {
        if (this.autoEndTime == -1) {
            return;
        }
        long delay = this.autoEndTime - System.currentTimeMillis();
        if (delay <= 0) {
            return;
        }
        this.scheduler.schedule(() -> stop(false), delay, TimeUnit.MILLISECONDS);
        /*
        this.scheduler.schedule(() -> {
            stop(false);
            this.future.complete(this);
        }, delay, TimeUnit.MILLISECONDS);
         */
    }

    /**
     * Stops the profiler.
     */
    @Override
    public void stopWork(boolean cancelled) {
        synchronized (this.currentJobMutex) {
            this.currentJob.stop();
            if (!cancelled) {
                this.windowStatisticsCollector.measureNow(this.currentJob.getWindow());
                this.currentJob.aggregate(this.dataAggregator);
            } else {
                this.currentJob.deleteOutputFile();
            }
            this.currentJob = null;
        }

        if (this.socketStatisticsTask != null) {
            this.socketStatisticsTask.cancel(false);
        }

        if (this.scheduler != null) {
            this.future.complete(this);
            this.scheduler.shutdown();
            this.scheduler = null;
        }
    }

    @Override
    public void attachSocket(ViewerSocket socket) {
        super.attachSocket(socket);
        if (this.socketStatisticsTask == null) {
            this.socketStatisticsTask = this.scheduler.scheduleAtFixedRate(this::sendStatisticsToSocket, 10, 10, TimeUnit.SECONDS);
        }
    }

    @Override
    public SamplerData toProto(FlareAPI flare, ExportProps exportProps, boolean stop) {
        SamplerData.Builder proto = SamplerData.newBuilder();
        if (exportProps.channelInfo() != null) {
            proto.setChannelInfo(exportProps.channelInfo());
        }
        writeMetadataToProto(proto, exportProps, this.dataAggregator);
        writeDataToProto(proto, this.dataAggregator, exportProps);
        return proto.build();
    }

}
