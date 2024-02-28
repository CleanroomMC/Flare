package com.cleanroommc.flare.client.tracker;

import com.cleanroommc.flare.common.tracker.Trackable;
import com.cleanroommc.flare.util.LangKeys;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.server.timings.ForgeTimings;

import java.util.Collection;
import java.util.Comparator;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

public class GuiTickList extends GuiScreen {

    private static String name(Object object) {
        return object.getClass().getSimpleName();
    }

    private static BlockPos pos(Object object) {
        if (object instanceof TileEntity) {
            return ((TileEntity) object).getPos();
        }
        if (object instanceof Entity) {
            return ((Entity) object).getPosition();
        }
        return BlockPos.ORIGIN;
    }

    private final Trackable<?> trackable;

    public GuiTickList(Trackable<?> trackable) {
        this.trackable = trackable;
    }

    @Override
    public void initGui() {
        this.trackable.timeTracker().enable(Integer.MAX_VALUE);
    }

    @Override
    public void onGuiClosed() {
        this.trackable.timeTracker().reset();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        // TODO: make this friendlier
        int start = 0;
        Collection<ForgeTimings<?>> sorted = this.trackable.timeTracker().getTimingData().stream()
                .sorted(Comparator.comparingDouble((ToDoubleFunction<ForgeTimings<?>>) ForgeTimings::getAverageTimings).reversed())
                .collect(Collectors.toList());
        for (ForgeTimings<?> timings : sorted) {
            Object object = timings.getObject().get();
            if (object == null) {
                continue;
            }
            BlockPos pos = pos(object);
            GlStateManager.pushMatrix();
            this.drawCenteredString(this.fontRenderer,
                    LangKeys.TRACKER_INFORMATION.translate(name(object), pos.getX(), pos.getY(), pos.getZ(), timings.getAverageTimings()),
                    this.width / 2, (10 + (start++ * 24)), 16777215);
            GlStateManager.popMatrix();
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

}
