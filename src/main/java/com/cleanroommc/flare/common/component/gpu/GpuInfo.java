package com.cleanroommc.flare.common.component.gpu;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import org.lwjgl.opengl.GL11;

public final class GpuInfo {

    private static boolean init;
    private static String model;

    public static String queryGpuModel() {
        if (!init) {
            if (FMLLaunchHandler.side().isClient()) {
                model = GlStateManager.glGetString(GL11.GL_RENDERER);
            }
            init = true;
        }
        return model;
    }

    private GpuInfo() { }
}
