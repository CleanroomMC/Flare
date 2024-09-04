package com.cleanroommc.flare.client.command;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.common.command.FlareCommand;
import net.minecraft.command.ICommandSender;
import net.minecraftforge.fml.relauncher.Side;

public class FlareClientCommand extends FlareCommand {

    public FlareClientCommand(FlareAPI flare) {
        super(flare, Side.CLIENT);
    }

    @Override
    public String getName() {
        return "flarec";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/flarec [command]";
    }
}
