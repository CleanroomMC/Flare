package com.cleanroommc.flare.util;

import com.cleanroommc.flare.api.FlareAPI;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public enum MonitoringExecutor {

    ;

    /** The executor used to monitor & calculate rolling averages. */
    public static final ScheduledExecutorService INSTANCE = Executors.newSingleThreadScheduledExecutor(runnable ->
            new FlareThreadFactory(FlareAPI.getInstance(), "flare-monitoring-thread").newThread(runnable));

}
