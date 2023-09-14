package com.cleanroommc.flare.common.sampler.async;

import com.cleanroommc.flare.api.sampler.SamplerMode;
import com.cleanroommc.flare.common.sampler.async.jfr.JfrReader;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.Collection;

/**
 * Collects and processes sample events for a given type.
 *
 * @param <E> the event type
 */
public interface SampleCollector<E extends JfrReader.Event> {

    /**
     * Gets the arguments to initialise the profiler.
     *
     * @param access the async profiler access object
     * @return the initialisation arguments
     */
    Collection<String> initArguments(AsyncProfilerAccess access);

    /**
     * Gets the event class processed by this sample collector.
     *
     * @return the event class
     */
    Class<E> eventClass();

    /**
     * Gets the measurements for a given event
     *
     * @param event the event
     * @return the measurement
     */
    long measure(E event);

    /**
     * Gets the mode for the collector.
     *
     * @return the mode
     */
    SamplerMode mode();

    /**
     * Sample collector for execution (cpu time) profiles.
     */
    final class Execution implements SampleCollector<JfrReader.ExecutionSample> {

        private final int interval; // Time in microseconds

        public Execution(int interval) {
            this.interval = interval;
        }

        @Override
        public Collection<String> initArguments(AsyncProfilerAccess access) {
            AsyncProfilerAccess.ProfilingEvent event = access.getProfilingEvent();
            Preconditions.checkNotNull(event);
            return ImmutableList.of("event=" + event, "interval=" + this.interval + "us");
        }

        @Override
        public Class<JfrReader.ExecutionSample> eventClass() {
            return JfrReader.ExecutionSample.class;
        }

        @Override
        public long measure(JfrReader.ExecutionSample event) {
            return event.value() * this.interval;
        }

        @Override
        public SamplerMode mode() {
            return SamplerMode.EXECUTION;
        }

    }

    /**
     * Sample collector for allocation (memory) profiles.
     */
    final class Allocation implements SampleCollector<JfrReader.AllocationSample> {

        private final int intervalBytes;
        private final boolean liveOnly;

        public Allocation(int intervalBytes, boolean liveOnly) {
            this.intervalBytes = intervalBytes;
            this.liveOnly = liveOnly;
        }

        public boolean isLiveOnly() {
            return this.liveOnly;
        }

        @Override
        public Collection<String> initArguments(AsyncProfilerAccess access) {
            AsyncProfilerAccess.ProfilingEvent event = access.getAllocationProfilingEvent();
            Preconditions.checkNotNull(event);

            ImmutableList.Builder<String> builder = ImmutableList.builder();
            builder.add("event=" + event);
            builder.add("alloc=" + this.intervalBytes);
            if (this.liveOnly) {
                builder.add("live");
            }
            return builder.build();
        }

        @Override
        public Class<JfrReader.AllocationSample> eventClass() {
            return JfrReader.AllocationSample.class;
        }

        @Override
        public long measure(JfrReader.AllocationSample event) {
            return event.value();
        }

        @Override
        public SamplerMode mode() {
            return SamplerMode.ALLOCATION;
        }

    }

}
