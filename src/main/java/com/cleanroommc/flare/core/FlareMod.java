package com.cleanroommc.flare.core;

import com.cleanroommc.flare.Tags;
import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.client.command.FlareClientCommand;
import com.cleanroommc.flare.common.command.FlareCommand;
import com.cleanroommc.flare.common.component.cpu.CpuMonitor;
import com.cleanroommc.flare.common.component.network.NetworkMonitor;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.client.FMLFileResourcePack;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.Side;

import java.io.File;

public class FlareMod extends DummyModContainer {

    // TODO: Description
    public FlareMod() {
        super(new ModMetadata());
        ModMetadata metadata = this.getMetadata();
        metadata.modId = Tags.MOD_ID;
        metadata.name = Tags.MOD_NAME;
        metadata.version = Tags.VERSION;
        metadata.authorList.add("Rongmario");
        metadata.authorList.add("lucko");
        metadata.credits = "CleanroomMC";
        metadata.logoFile = "assets/flare/icon.png";
    }

    @Override
    public boolean registerBus(EventBus bus, LoadController controller) {
        bus.register(this);
        return true;
    }

    @Override
    public File getSource() {
        return FlarePlugin.source;
    }

    @Override
    public Class<?> getCustomResourcePackClass() {
        return FMLFileResourcePack.class;
    }

    @Subscribe
    public void preInit(FMLPreInitializationEvent event) {
        if (event.getSide() == Side.CLIENT) {
            FlareKeybindings.register();
            FlareClientEvents.register();
        }
    }

    @Subscribe
    public void onServerStarting(FMLServerStartingEvent event) {
        if (FMLLaunchHandler.side().isClient()) {
            ClientCommandHandler.instance.registerCommand(new FlareClientCommand(FlareAPI.getInstance()));
        }
        event.registerServerCommand(new FlareCommand(FlareAPI.getInstance(), Side.SERVER));
    }

    @Subscribe
    public void onServerStarted(FMLServerStartedEvent event) {
        ((Flare) FlareAPI.getInstance()).logServerStartTime();
        CpuMonitor.ensureMonitoring();
        NetworkMonitor.ensureMonitoring();
    }

}
