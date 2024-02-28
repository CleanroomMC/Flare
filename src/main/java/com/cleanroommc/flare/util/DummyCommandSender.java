package com.cleanroommc.flare.util;

import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class DummyCommandSender implements ICommandSender {

    @Override
    public String getName() {
        return "FlareAPI";
    }

    @Override
    public boolean canUseCommand(int permLevel, String commandName) {
        return false;
    }

    @Override
    public World getEntityWorld() {
        return null;
    }

    @Nullable
    @Override
    public MinecraftServer getServer() {
        return null;
    }

}
