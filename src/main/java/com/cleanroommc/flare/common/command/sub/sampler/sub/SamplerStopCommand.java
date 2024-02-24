package com.cleanroommc.flare.common.command.sub.sampler.sub;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.api.sampler.Sampler;
import com.cleanroommc.flare.api.sampler.SamplerContainer;
import com.cleanroommc.flare.common.command.sub.FlareSubCommand;
import com.cleanroommc.flare.common.sampler.ExportProps;
import com.cleanroommc.flare.util.LangKeys;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

public class SamplerStopCommand extends FlareSubCommand {

    private final boolean cancel;

    public SamplerStopCommand(FlareAPI flare, boolean cancel) {
        super(flare);
        this.cancel = cancel;
    }

    @Override
    public String getName() {
        return this.cancel ? "cancel" : "stop";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        if (this.cancel) {
            return "/flare sampler cancel";
        }
        return "/flare sampler stop [--arguments]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        SamplerContainer<Sampler> samplerContainer = this.flare.samplerContainer();
        if (samplerContainer.isSamplerActive()) {
            if (this.cancel) {
                sendMessage(sender, LangKeys.SAMPLER_CANCELLING);
                samplerContainer.stopSampler(true);
                return;
            }
            final String comment = getArgValue(args, "comment");
            final boolean separateParentCalls = hasArg(args, "separate-parent-calls");
            final boolean saveToFile = hasArg(args, "save-to-file");
            ExportProps exportProps = samplerContainer.getExportProps();
            if (comment != null) {
                exportProps.comment(comment);
            }
            if (separateParentCalls && !exportProps.separateParentCalls()) {
                exportProps.separateParentCalls(true);
            }
            if (saveToFile) {
                exportProps.saveToFile(true);
            }
            try {
                sendMessage(sender, LangKeys.SAMPLER_STOPPING);
                Sampler sampler = samplerContainer.stopSampler(false);
                SamplerUtil.upload(this.flare, samplerContainer.getExportProps(), (k, c, a) -> sendMessage(sender, k, c, a), sampler, true);
            } catch (Throwable t) {
                sendMessage(sender, LangKeys.SAMPLER_FAILED_UNEXPECTEDLY);
                this.flare.logger().fatal(t);
            }
        } else {
            sendMessage(sender, LangKeys.SAMPLER_HAS_NOT_STARTED);
        }
    }

}
