package com.cleanroommc.flare.api.sampler;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.api.activity.Activity;
import com.cleanroommc.flare.common.command.sub.sampler.sub.SamplerUtil;
import com.cleanroommc.flare.common.sampler.ExportProps;
import com.cleanroommc.flare.common.sampler.SamplingStage;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

public class SamplerContainer<T extends Sampler> {

    private final FlareAPI flare;

    protected final AtomicReference<T> activeSampler = new AtomicReference<>();

    protected SamplingStage stage;
    protected ExportProps exportProps = new ExportProps();

    public SamplerContainer(FlareAPI flare) {
        this.flare = flare;
    }

    public boolean isSamplerActive() {
        return this.activeSampler.get() != null;
    }

    public Sampler activeSampler() {
        return this.activeSampler.get();
    }

    public SamplingStage stage() {
        return stage;
    }

    public void setSampler(T sampler) {
        if (!this.activeSampler.compareAndSet(null, sampler)) {
            throw new IllegalStateException("Attempted to set active sampler when another was already active!");
        }
        this.exportProps = new ExportProps();
    }

    public void setSampler(T sampler, SamplingStage stage) {
        this.setSampler(sampler);
        this.stage = stage;
        LocalDateTime current = LocalDateTime.now();
        FlareAPI.getInstance().activityLog().write(new Activity() {
            @Override
            public LocalDateTime time() {
                return current;
            }
            @Override
            public String description() {
                return "Started Sampler for stage: " + stage;
            }
        });
    }

    public T stopSampler(boolean cancelled) {
        T sampler = this.activeSampler.getAndSet(null);
        if (sampler != null) {
            sampler.stop(cancelled);
        }
        this.stage = null;
        return sampler;
    }

    public T stopSampler(boolean cancelled, SamplingStage stage) {
        if (stage == this.stage) {
            T sampler = this.stopSampler(cancelled);
            LocalDateTime current = LocalDateTime.now();
            ExportProps.setDefault(this.flare, this.exportProps);
            FlareAPI.getInstance().activityLog().write(new Activity() {
                @Override
                public LocalDateTime time() {
                    return current;
                }
                @Override
                public String description() {
                    try {
                        String url = SamplerUtil.upload(SamplerContainer.this.flare, SamplerContainer.this.exportProps, sampler, false, true);
                        return "Sampler finished for stage: " + stage + ". Report: " + url;
                    } catch (Throwable t) {
                        SamplerContainer.this.flare.logger().fatal("Unable to upload sampler report", t);
                        return "Unable to upload sampler report! Check logs for more detail.";
                    }
                }
            });
            return sampler;
        }
        return null;
    }

    public ExportProps getExportProps() {
        return this.exportProps;
    }

}
