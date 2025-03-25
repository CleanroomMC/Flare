package com.cleanroommc.flare.common.command.sub.sampler;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.common.command.sub.sampler.sub.*;
import net.minecraft.command.ICommandSender;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.server.command.CommandTreeBase;

// TODO: client overload
public class SamplerTreeCommand extends CommandTreeBase {

    public SamplerTreeCommand(Side side, FlareAPI flare) {
        addSubcommand(new SamplerStartCommand(side, flare));
        addSubcommand(new SamplerInfoCommand(flare));
        addSubcommand(new SamplerViewCommand(flare));
        addSubcommand(new SamplerTrustViewerCommand(flare));
        addSubcommand(new SamplerStopCommand(flare, true));
        addSubcommand(new SamplerStopCommand(flare, false));
    }

    @Override
    public String getName() {
        return "sampler";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/flare sampler [command]";
    }

}
