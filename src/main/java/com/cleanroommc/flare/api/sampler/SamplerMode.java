package com.cleanroommc.flare.api.sampler;

public enum SamplerMode {

    EXECUTION(4), // ms
    ALLOCATION(1024 * 512); // bytes (512KiB);

    private final int interval;

    SamplerMode(int interval) {
        this.interval = interval;
    }

    public int interval() {
        return this.interval;
    }

}
