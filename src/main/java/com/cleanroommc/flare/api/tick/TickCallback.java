package com.cleanroommc.flare.api.tick;

import net.minecraftforge.fml.relauncher.Side;

public interface TickCallback {

    Side getSide();

    default void onTickStart(int currentTick, double duration) {

    }

    default void onTickEnd(int currentTick, double duration) {

    }

}

