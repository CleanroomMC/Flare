package com.cleanroommc.flare.common.command.sub.sampler.sub;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.api.sampler.Sampler;
import com.cleanroommc.flare.common.command.sub.FlareSubCommand;
import com.cleanroommc.flare.util.FormatUtil;
import com.cleanroommc.flare.util.LangKeys;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

public class SamplerInfoCommand extends FlareSubCommand {

    public SamplerInfoCommand(FlareAPI flare) {
        super(flare);
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/flare sampler info";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        Sampler sampler = this.flare.samplerContainer().activeSampler();
        if (sampler == null) {
            sendMessage(sender, LangKeys.SAMPLER_INFO_START);
        } else {
            // TODO: beautify
            long runningTime = (System.currentTimeMillis() - sampler.startTime()) / 1000L;
            // TODO: make distinction between profiler / sampler (?)
            sendMessage(sender, LangKeys.SAMPLER_INFO_STARTED, FormatUtil.formatSeconds(runningTime));
            sendMessage(sender, LangKeys.SAMPLER_INFO_VIEW);
            long timeout = sampler.autoEndTime();
            if (timeout == -1) {
                sendMessage(sender, LangKeys.SAMPLER_INFO_STOP);
            } else {
                sendMessage(sender, LangKeys.SAMPLER_INFO_STOPPING, FormatUtil.formatSeconds((timeout - System.currentTimeMillis()) / 1000L) + ".");
            }
            sendMessage(sender, LangKeys.SAMPLER_INFO_CANCEL);
        }
    }

}
