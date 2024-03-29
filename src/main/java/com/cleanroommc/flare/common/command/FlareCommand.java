package com.cleanroommc.flare.common.command;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.common.command.sub.component.HealthCommand;
import com.cleanroommc.flare.common.command.sub.component.ping.PingCommand;
import com.cleanroommc.flare.common.command.sub.component.tick.TPSCommand;
import com.cleanroommc.flare.common.command.sub.memory.HeapTreeCommand;
import com.cleanroommc.flare.common.command.sub.sampler.SamplerTreeCommand;
import net.minecraft.command.ICommandSender;
import net.minecraftforge.server.command.CommandTreeBase;

public class FlareCommand extends CommandTreeBase {

    public FlareCommand(FlareAPI flare) {
        // Tree
        addSubcommand(new HeapTreeCommand(flare));
        addSubcommand(new SamplerTreeCommand(flare));

        // Leaves
        addSubcommand(new HealthCommand(flare));
        addSubcommand(new PingCommand(flare));
        addSubcommand(new TPSCommand(flare));
    }

    @Override
    public String getName() {
        return "flare";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/flare [command]";
    }

}
