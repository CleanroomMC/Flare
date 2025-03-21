package com.cleanroommc.flare.api.tick;

import net.minecraftforge.fml.relauncher.Side;

public interface TickCallback {

    default void onTickStart(Side side, TickType type, int currentTick, double duration) {

    }

    default void onTickEnd(Side side, TickType type, int currentTick, double duration) {

    }

}

