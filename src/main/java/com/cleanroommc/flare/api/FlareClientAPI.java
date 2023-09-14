package com.cleanroommc.flare.api;

import com.cleanroommc.flare.api.sampler.thread.ThreadDumper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface FlareClientAPI {

    @SideOnly(Side.CLIENT)
    ThreadDumper clientThreadDumper();

    @SideOnly(Side.CLIENT)
    void syncWithClient(Runnable runnable);

}
