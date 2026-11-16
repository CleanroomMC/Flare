package com.cleanroommc.flare.common.command.sub.memory;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.common.command.sub.memory.sub.HeapDumpCommand;
import com.cleanroommc.flare.common.command.sub.memory.sub.HeapSummaryCommand;
import com.cleanroommc.flare.common.command.FlareCommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.command.CommandTreeBase;

public class HeapTreeCommand extends CommandTreeBase {

    public HeapTreeCommand(FlareAPI flare) {
        addSubcommand(new HeapDumpCommand(flare));
        addSubcommand(new HeapSummaryCommand(flare));
    }

    @Override
    public String getName() {
        return "heap";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/flare heap [command]";
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return FlareCommand.isClientSender(sender) || super.checkPermission(server, sender);
    }

}
