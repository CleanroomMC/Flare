package com.cleanroommc.flare.common.command;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.common.command.sub.component.HealthCommand;
import com.cleanroommc.flare.common.command.sub.component.ping.PingCommand;
import com.cleanroommc.flare.common.command.sub.component.tick.TPSCommand;
import com.cleanroommc.flare.common.command.sub.component.tick.TickMonitoringCommand;
import com.cleanroommc.flare.common.command.sub.memory.HeapTreeCommand;
import com.cleanroommc.flare.common.command.sub.sampler.SamplerTreeCommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.server.command.CommandTreeBase;

public class FlareCommand extends CommandTreeBase {

    public FlareCommand(FlareAPI flare, Side side) {
        // Tree
        addSubcommand(new HeapTreeCommand(flare));
        addSubcommand(new SamplerTreeCommand(side, flare));

        // Leaves
        addSubcommand(new HealthCommand(flare));
        addSubcommand(new PingCommand(flare));
        addSubcommand(new TPSCommand(side, flare));
        addSubcommand(new TickMonitoringCommand(side, flare));
    }

    @Override
    public String getName() {
        return "flare";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/flare [command]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return server == null || !server.isDedicatedServer() || super.checkPermission(server, sender);
    }

}
