package com.cleanroommc.flare.core;

import com.cleanroommc.flare.Tags;
import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.client.command.FlareClientCommand;
import com.cleanroommc.flare.common.command.FlareCommand;
import com.cleanroommc.flare.common.component.cpu.CpuMonitor;
import com.cleanroommc.flare.common.component.gpu.GpuInfo;
import com.cleanroommc.flare.common.component.network.NetworkMonitor;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.client.FMLFileResourcePack;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.event.*;
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
    public void onConstruct(FMLConstructionEvent event) {
        GpuInfo.queryGpuModel(); // Earliest
    }

    @Subscribe
    public void preInit(FMLPreInitializationEvent event) {
        if (event.getSide() == Side.CLIENT) {
            FlareKeybindings.register();
            FlareClientEvents.register();

            FlareAPI.getInstance().tickRoutine(Side.CLIENT).start();
        }
        FlareAPI.getInstance().tickRoutine(Side.SERVER).start();
    }

    @Subscribe
    public void postInit(FMLPostInitializationEvent event) {
        if (event.getSide().isClient()) {
            ClientCommandHandler.instance.registerCommand(new FlareClientCommand(FlareAPI.getInstance()));
        }
        CpuMonitor.ensureMonitoring();
        NetworkMonitor.ensureMonitoring();
    }

    @Subscribe
    public void onServerStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new FlareCommand(FlareAPI.getInstance(), Side.SERVER));
    }

    @Subscribe
    public void onServerStarted(FMLServerStartedEvent event) {
        ((Flare) FlareAPI.getInstance()).logServerStartTime();
    }

}
