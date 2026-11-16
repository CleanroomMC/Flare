package com.cleanroommc.flare.common.command.sub.sampler.sub;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.common.command.sub.CommandOptions;
import com.cleanroommc.flare.common.command.sub.FlareSubCommand;
import com.cleanroommc.flare.common.sampler.AbstractSampler;
import com.cleanroommc.flare.common.websocket.ViewerSocket;
import com.cleanroommc.flare.util.LangKeys;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

public class SamplerTrustViewerCommand extends FlareSubCommand {

    public SamplerTrustViewerCommand(FlareAPI flare) {
        super(flare);
    }

    @Override
    public String getName() {
        return "trustviewer";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/flare sampler trustviewer";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        return CommandOptions.buildTabCompletion(args, CommandOptions.ID);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        Set<String> ids = getArgValues(args, "id");
        if (ids.isEmpty()) {
            sendMessage(sender, LangKeys.SAMPLER_VIEWER_ID_NOT_PROVIDED);
            return;
        }
        for (String id : ids) {
            boolean success = this.flare.trustedKeyStore().trustPendingKey(id);
            if (success) {
                AbstractSampler sampler = (AbstractSampler) this.flare.samplerContainer().activeSampler();
                if (sampler != null) {
                    for (ViewerSocket socket : sampler.getAttachedSockets()) {
                        socket.sendClientTrustedMessage(id);
                    }
                }
                sendMessage(sender, LangKeys.SAMPLER_VIEWER_TRUST, id);
            } else {
                sendMessage(sender, LangKeys.SAMPLER_VIEWER_TRUST_ID_NOT_FOUND, id);
            }
        }
    }

}
