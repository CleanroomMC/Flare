package com.cleanroommc.flare.common.command.sub.memory;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.common.command.sub.memory.sub.HeapDumpCommand;
import com.cleanroommc.flare.common.command.sub.memory.sub.HeapSummaryCommand;
import net.minecraft.command.ICommandSender;
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

}
