package com.cleanroommc.flare.api.sampler;

import com.cleanroommc.flare.api.sampler.thread.ThreadDumper;
import com.cleanroommc.flare.api.sampler.thread.ThreadGrouper;
import com.cleanroommc.flare.api.tick.TickRoutine;
import net.minecraftforge.fml.relauncher.Side;

import java.util.concurrent.TimeUnit;

public interface SamplerBuilder {

    SamplerBuilder side(Side side);

    SamplerBuilder mode(SamplerMode mode);

    SamplerBuilder interval(double interval);

    SamplerBuilder completeAfter(long timeout, TimeUnit unit);

    SamplerBuilder runningInBackground(boolean runningInBackground);

    SamplerBuilder threadDumper(ThreadDumper threadDumper);

    SamplerBuilder threadGrouper(ThreadGrouper threadGrouper);

    SamplerBuilder ticksOver(int ticksOver, TickRoutine tickRoutine);

    SamplerBuilder ignoreSleeping(boolean ignoreSleeping);

    SamplerBuilder ignoreNative(boolean ignoreNative);

    SamplerBuilder forceJavaSampler(boolean forceJavaSampler);

    SamplerBuilder allocLiveOnly(boolean allocLiveOnly);

    Sampler build();

}
