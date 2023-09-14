package com.cleanroommc.flare.common.command.sub.sampler;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.common.command.sub.sampler.sub.*;
import net.minecraft.command.ICommandSender;
import net.minecraftforge.server.command.CommandTreeBase;

// TODO: client overload
public class SamplerTreeCommand extends CommandTreeBase {

    public SamplerTreeCommand(FlareAPI flare) {
        addSubcommand(new SamplerStartCommand(flare));
        addSubcommand(new SamplerInfoCommand(flare));
        // TODO: addSubcommand(new SamplerUploadCommand(flare));
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
