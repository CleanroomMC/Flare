package com.cleanroommc.flare.common.component.tick;

import com.cleanroommc.flare.api.tick.TickCallback;
import com.cleanroommc.flare.api.tick.TickRoutine;
import com.cleanroommc.flare.api.tick.TickType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.List;

public class FlareTickRoutine implements TickRoutine {

    private final List<TickCallback> callbacks = new ArrayList<>();

    private long start = 0;
    private int tick = 0;

    @SubscribeEvent
    public void onTick(TickEvent e) {
        double duration = (System.nanoTime() - this.start) / 1000000D;
        Side side = e.side;
        TickType type;
        switch (e.type) {
            case PLAYER:
                type = TickType.PLAYER;
                break;
            case WORLD:
                type = TickType.WORLD;
                break;
            case RENDER:
                type = TickType.RENDER;
                break;
            default:
                type = TickType.ALL;
        }

        if (e.phase == TickEvent.Phase.START) {
            this.start = System.nanoTime();
            for (int i = 0; i < this.callbacks.size(); i++) {
                this.callbacks.get(i).onTickStart(side, type, this.tick, duration);
            }
        } else {
            for (int i = 0; i < this.callbacks.size(); i++) {
                this.callbacks.get(i).onTickEnd(side, type, this.tick, duration);
            }
            this.tick++;
        }
    }

    @Override
    public void start() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void stop() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @Override
    public int currentTick() {
        return tick;
    }

    @Override
    public void addCallback(TickCallback runnable) {
        synchronized (this.callbacks) {
            this.callbacks.add(runnable);
        }
    }

    @Override
    public void removeCallback(TickCallback runnable) {
        synchronized (this.callbacks) {
            this.callbacks.remove(runnable);
        }
    }

}
