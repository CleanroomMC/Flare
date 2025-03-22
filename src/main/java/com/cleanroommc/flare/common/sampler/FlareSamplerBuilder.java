package com.cleanroommc.flare.common.sampler;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.api.sampler.Sampler;
import com.cleanroommc.flare.api.sampler.SamplerBuilder;
import com.cleanroommc.flare.api.sampler.SamplerMode;
import com.cleanroommc.flare.api.sampler.thread.ThreadDumper;
import com.cleanroommc.flare.api.sampler.thread.ThreadGrouper;
import com.cleanroommc.flare.api.tick.TickRoutine;
import com.cleanroommc.flare.common.sampler.async.AsyncProfilerAccess;
import com.cleanroommc.flare.common.sampler.async.AsyncSampler;
import com.cleanroommc.flare.common.sampler.async.SampleCollector;
import com.cleanroommc.flare.common.sampler.java.JavaSampler;
import com.google.common.base.Preconditions;
import net.minecraftforge.fml.relauncher.Side;

import java.util.concurrent.TimeUnit;

public class FlareSamplerBuilder implements SamplerBuilder {

    private final FlareAPI flare;

    private SamplerMode mode = SamplerMode.EXECUTION;
    private double interval = mode.interval();
    private long endTime = -1L;
    private boolean ignoreSleeping, ignoreNative, forceJavaSampler, runningInBackground = true;
    private boolean allocLiveOnly = false;
    private ThreadDumper threadDumper = ThreadDumper.ALL;
    private ThreadGrouper threadGrouper = ThreadGrouper.AS_ONE;
    private int ticksOver = -1;
    private TickRoutine tickRoutine = null;
    private Side side = Side.SERVER;

    public FlareSamplerBuilder(FlareAPI flare) {
        this.flare = flare;
    }

    @Override
    public SamplerBuilder side(Side side) {
        Preconditions.checkNotNull(side, "Side must be provided");
        this.side = side;
        return this;
    }

    @Override
    public SamplerBuilder mode(SamplerMode mode) {
        Preconditions.checkNotNull(mode, "SamplerMode must be provided");
        this.mode = mode;
        return this;
    }

    @Override
    public SamplerBuilder interval(double interval) {
        Preconditions.checkArgument(interval > 0D, "Interval value should be more than 0");
        this.interval = interval;
        return this;
    }

    @Override
    public SamplerBuilder completeAfter(long timeout, TimeUnit unit) {
        Preconditions.checkArgument(timeout > 0, "Timeout value should be more than 0");
        Preconditions.checkNotNull(unit, "Timeout value's unit must be provided");
        this.endTime = System.currentTimeMillis() + unit.toMillis(timeout);
        return this;
    }

    @Override
    public SamplerBuilder runningInBackground(boolean runningInBackground) {
        this.runningInBackground = runningInBackground;
        return this;
    }

    @Override
    public SamplerBuilder threadDumper(ThreadDumper threadDumper) {
        Preconditions.checkNotNull(threadDumper, "ThreadDumper must be provided");
        this.threadDumper = threadDumper;
        return this;
    }

    @Override
    public SamplerBuilder threadGrouper(ThreadGrouper threadGrouper) {
        Preconditions.checkNotNull(threadGrouper, "ThreadGrouper must be provided");
        this.threadGrouper = threadGrouper;
        return this;
    }

    @Override
    public SamplerBuilder ticksOver(int ticksOver, TickRoutine tickRoutine) {
        Preconditions.checkArgument(ticksOver > 0, "TicksOver value should be more than 0");
        Preconditions.checkNotNull(tickRoutine, "TickRoutine must be provided");
        this.ticksOver = ticksOver;
        this.tickRoutine = tickRoutine;
        return this;
    }

    @Override
    public SamplerBuilder ignoreSleeping(boolean ignoreSleeping) {
        this.ignoreSleeping = ignoreSleeping;
        return this;
    }

    @Override
    public SamplerBuilder ignoreNative(boolean ignoreNative) {
        this.ignoreNative = ignoreNative;
        return this;
    }

    @Override
    public SamplerBuilder forceJavaSampler(boolean forceJavaSampler) {
        this.forceJavaSampler = forceJavaSampler;
        return this;
    }

    @Override
    public SamplerBuilder allocLiveOnly(boolean allocLiveOnly) {
        this.allocLiveOnly = allocLiveOnly;
        return this;
    }

    @Override
    public Sampler build() throws UnsupportedOperationException {
        boolean onlyTicksOverMode = this.ticksOver != -1 && this.tickRoutine != null;
        boolean canUseAsyncProfiler = !this.forceJavaSampler && !onlyTicksOverMode &&
                !(this.ignoreSleeping || this.ignoreNative) && AsyncProfilerAccess.getInstance(this.flare).checkSupported(this.flare);

        if (this.mode == SamplerMode.ALLOCATION && (!canUseAsyncProfiler ||
                !AsyncProfilerAccess.getInstance(this.flare).checkAllocationProfilingSupported(this.flare))) {
            throw new UnsupportedOperationException("Allocation profiling is not supported on your system. Check the console for more info.");
        }

        // Convert to microseconds
        int interval = (int) (this.mode == SamplerMode.EXECUTION ? this.interval * 1000D : this.interval);

        Sampler sampler;
        if (this.mode == SamplerMode.ALLOCATION) {
            sampler = new AsyncSampler(this.flare, this.side, this.threadGrouper, new SampleCollector.Allocation(interval, this.allocLiveOnly),
                    interval, this.threadDumper, this.endTime, this.runningInBackground);
        } else if (canUseAsyncProfiler) {
            sampler = new AsyncSampler(this.flare, this.side, this.threadGrouper, new SampleCollector.Execution(interval), interval,
                    this.threadDumper, this.endTime, this.runningInBackground);
        } else if (onlyTicksOverMode) {
            sampler = new JavaSampler(this.flare, this.side, interval, this.threadDumper, this.endTime, this.runningInBackground,
                    this.ignoreSleeping, this.ignoreNative, this.threadGrouper, this.tickRoutine, this.ticksOver);
        } else {
            sampler = new JavaSampler(this.flare, this.side, interval, this.threadDumper, this.endTime, this.runningInBackground,
                    this.threadGrouper, this.ignoreSleeping, this.ignoreNative);
        }
        return sampler;
    }

}
