package com.cleanroommc.flare.util;

import com.cleanroommc.flare.api.FlareAPI;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class FlareThreadFactory implements ThreadFactory {

    private static final Thread.UncaughtExceptionHandler UNCAUGHT_EXCEPTION_HANDLER = (thread, exception) -> {
        if (thread instanceof FlareThread) {
            FlareThread flareThread = (FlareThread) thread;
            flareThread.flare.logger().fatal("Uncaught exception thrown by {}", flareThread.getName(), exception);
        } else {
            System.err.println("Uncaught exception thrown by thread " + thread.getName());
            exception.printStackTrace();
        }
    };
    private static final AtomicInteger poolCount = new AtomicInteger(1);

    private final FlareAPI flare;
    private final AtomicInteger threadCount;
    private final String namePrefix;

    public FlareThreadFactory(FlareAPI flare) {
        this(flare, "flareworker-pool-" + poolCount.getAndIncrement() + "-thread");
    }

    public FlareThreadFactory(FlareAPI flare, String namePrefix) {
        this.flare = flare;
        this.threadCount = new AtomicInteger(1);
        this.namePrefix = namePrefix;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        FlareThread thread = new FlareThread(this.flare, this.namePrefix + "-" + this.threadCount.getAndIncrement(), runnable);
        thread.setUncaughtExceptionHandler(UNCAUGHT_EXCEPTION_HANDLER);
        thread.setDaemon(true);
        return thread;
    }

    private static class FlareThread extends Thread {

        private final FlareAPI flare;

        private FlareThread(FlareAPI flare, String name, Runnable runnable) {
            super(runnable, name);
            this.flare = flare;
        }

    }

}
