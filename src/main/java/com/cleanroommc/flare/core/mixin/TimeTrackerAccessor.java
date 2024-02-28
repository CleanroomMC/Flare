package com.cleanroommc.flare.core.mixin;

import net.minecraftforge.server.timings.TimeTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TimeTracker.class, remap = false)
public interface TimeTrackerAccessor {

    @Accessor(value = "enabled")
    boolean isEnabled();

}
