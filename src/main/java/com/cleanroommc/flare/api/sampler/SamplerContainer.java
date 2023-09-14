package com.cleanroommc.flare.api.sampler;

import com.cleanroommc.flare.common.sampler.ExportProps;

import java.util.concurrent.atomic.AtomicReference;

public class SamplerContainer<T extends Sampler> {

    protected final AtomicReference<T> activeSampler = new AtomicReference<>();

    protected ExportProps exportProps = new ExportProps();

    public Sampler activeSampler() {
        return activeSampler.get();
    }

    public void setSampler(T sampler) {
        if (!this.activeSampler.compareAndSet(null, sampler)) {
            throw new IllegalStateException("Attempted to set active sampler when another was already active!");
        }
    }

    public void unsetSampler(T sampler) {
        this.activeSampler.compareAndSet(sampler, null);
    }

    public void stopSampler(boolean cancelled) {
        T sampler = this.activeSampler.getAndSet(null);
        if (sampler != null) {
            sampler.stop(cancelled);
        }
    }

    public ExportProps getExportProps() {
        return this.exportProps;
    }

}
