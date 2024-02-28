package com.cleanroommc.flare.client.tracker;

import com.cleanroommc.flare.common.tracker.Trackable;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.util.List;

public class GuiSelection extends GuiScreen {

    @Override
    public void initGui() {
        this.buttonList.clear();
        int index = 0;
        for (Trackable<?> trackable : Trackable.all()) {
            this.buttonList.add(new GuiButton(index++, this.width / 2 - 100, this.height / 4 + (index * 24), trackable.translate()));
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        List<Trackable<?>> trackables = Trackable.all();
        int index = button.id;
        if (index >= trackables.size()) {
            return;
        }
        Trackable<?> trackable = trackables.get(index);
        this.mc.displayGuiScreen(new GuiTickList(trackable));
    }

}
