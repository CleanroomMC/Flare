package com.cleanroommc.flare.api.tick;

/**
 * A Tick Routine.
 */
public interface TickRoutine {

    /**
     * Starts the hook
     */
    void start();

    /**
     * Stops the hook
     */
    void stop();

    /**
     * Gets the current tick number
     *
     * @return the current tick
     */
    int currentTick();

    /**
     * Adds a callback to be called each time the tick increments
     *
     * @param callback the task
     */
    void addCallback(TickCallback callback);

    /**
     * Removes a callback
     *
     * @param callback the callback
     */
    void removeCallback(TickCallback callback);

}
