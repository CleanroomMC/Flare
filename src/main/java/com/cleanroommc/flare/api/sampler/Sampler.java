package com.cleanroommc.flare.api.sampler;

import java.util.function.BiConsumer;

public interface Sampler {

    /**
     * Mode of the sampler.
     */
    SamplerMode mode();

    /**
     * Starts the sampler.
     */
    void start();

    /**
     * Stops the sampler.
     */
    void stop(boolean cancelled);

    /**
     * Gets the time when the sampler started (unix timestamp in millis)
     *
     * @return the start time
     */
    long startTime();

    /**
     * Gets the time when the sampler should automatically stop (unix timestamp in millis)
     *
     * @return the end time, or -1 if undefined
     */
    long autoEndTime();

    /**
     * If this sampler is running in the background. (wasn't started by a specific user)
     *
     * @return true if the sampler is running in the background
     */
    boolean runningInBackground();

    /**
     * Run tasks after the Sampler is completed
     */
    void whenComplete(BiConsumer<Sampler, Throwable> biConsumer);

}
