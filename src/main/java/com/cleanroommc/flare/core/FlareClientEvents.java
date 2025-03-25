package com.cleanroommc.flare.core;

import com.cleanroommc.flare.client.tracker.GuiSelection;
import com.cleanroommc.flare.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class FlareClientEvents {

    static void register() {
        MinecraftForge.EVENT_BUS.register(FlareClientEvents.class);
    }

    FlareClientEvents() { }

    @SubscribeEvent
    public static void onClientChatReceived(ClientChatReceivedEvent event) {
        if (event.getType() == ChatType.SYSTEM) {
            ITextComponent message = event.getMessage();
            if (message instanceof TextComponentTranslation) {
                if (((TextComponentTranslation) message).getKey().startsWith("flare.")) {
                    event.setMessage(ChatUtil.prefix(message));
                }
            }
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onKeyInput(KeyInputEvent event) {
        if (FlareKeybindings.startSampling()) {
            Minecraft.getMinecraft().player.sendChatMessage("/flare sampler start");
        } else if (FlareKeybindings.startClientSampling()) {
            Minecraft.getMinecraft().player.sendChatMessage("/flarec sampler start");
        } else if (FlareKeybindings.stopSampling() || FlareKeybindings.stopClientSampling()) {
            Minecraft.getMinecraft().player.sendChatMessage("/flare sampler stop");
        }
        if (FlareKeybindings.openTickTrackingMenu()) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiSelection());
        }
    }

}
