package com.cleanroommc.flare.common.component.gpu;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

public final class GpuInfo {

    private static volatile String model;

    @SideOnly(Side.CLIENT)
    public static void captureGpuModel() {
        model = GlStateManager.glGetString(GL11.GL_RENDERER);
    }

    public static String queryGpuModel() {
        return model;
    }

    private GpuInfo() { }
}
