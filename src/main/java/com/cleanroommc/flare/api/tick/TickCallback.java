package com.cleanroommc.flare.api.tick;

public interface TickCallback {

    default void onTickStart(int currentTick, double duration) {

    }

    default void onTickEnd(int currentTick, double duration) {

    }

}

