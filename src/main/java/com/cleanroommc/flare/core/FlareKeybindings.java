package com.cleanroommc.flare.core;

import com.cleanroommc.flare.api.FlareAPI;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

// TODO small window to control sampling?
@SideOnly(Side.CLIENT)
public final class FlareKeybindings {

    // TODO Lang Key
    // Default off, left for the user to configure
    @SideOnly(Side.CLIENT)
    private static final KeyBinding START_SAMPLING_KEY = new KeyBinding("flare.key.start_sampling", 0, "flare.key.category");
    @SideOnly(Side.CLIENT)
    private static final KeyBinding STOP_SAMPLING_KEY = new KeyBinding("flare.key.stop_sampling", 0, "flare.key.category");
    @SideOnly(Side.CLIENT)
    private static final KeyBinding START_CLIENT_SAMPLING_KEY = new KeyBinding("flare.key.start_client_sampling", 0, "flare.key.category");
    @SideOnly(Side.CLIENT)
    private static final KeyBinding STOP_CLIENT_SAMPLING_KEY = new KeyBinding("flare.key.stop_client_sampling", 0, "flare.key.category");

    static FlareAPI flare;

    @SideOnly(Side.CLIENT)
    static void register() {
        flare = FlareAPI.getInstance();
        ClientRegistry.registerKeyBinding(START_SAMPLING_KEY);
        ClientRegistry.registerKeyBinding(STOP_SAMPLING_KEY);
        ClientRegistry.registerKeyBinding(START_CLIENT_SAMPLING_KEY);
        ClientRegistry.registerKeyBinding(STOP_CLIENT_SAMPLING_KEY);
    }

    @SideOnly(Side.CLIENT)
    static boolean startSampling() {
        return START_SAMPLING_KEY.isPressed();
    }

    @SideOnly(Side.CLIENT)
    static boolean startClientSampling() {
        return START_CLIENT_SAMPLING_KEY.isPressed();
    }

    @SideOnly(Side.CLIENT)
    static boolean stopSampling() {
        return STOP_SAMPLING_KEY.isPressed();
    }

    @SideOnly(Side.CLIENT)
    static boolean stopClientSampling() {
        return STOP_CLIENT_SAMPLING_KEY.isPressed();
    }

    @SideOnly(Side.CLIENT)
    private FlareKeybindings() { }

}
