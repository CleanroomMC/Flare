package com.cleanroommc.flare.util;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.api.sampler.Sampler;
import com.cleanroommc.flare.api.sampler.SamplerMode;
import com.cleanroommc.flare.api.sampler.thread.ThreadDumper;
import com.cleanroommc.flare.api.sampler.thread.ThreadGrouper;
import com.cleanroommc.flare.common.config.FlareConfiguration;

public final class DefaultSampler {

    public static Sampler build() {
        return FlareAPI.getInstance().samplerBuilder()
                .mode(SamplerMode.EXECUTION)
                .interval(FlareConfiguration.defaultSamplerInterval)
                .threadDumper(ThreadDumper.ALL)
                .threadGrouper(ThreadGrouper.BY_POOL)
                .ignoreSleeping(true)
                .ignoreNative(true)
                .forceJavaSampler(FlareConfiguration.forceDefaultJavaSampler)
                .build();
    }

    private DefaultSampler() { }

}
