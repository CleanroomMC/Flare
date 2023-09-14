package com.cleanroommc.flare.common.command.sub.sampler.sub;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.api.sampler.Sampler;
import com.cleanroommc.flare.api.sampler.SamplerContainer;
import com.cleanroommc.flare.common.command.sub.FlareSubCommand;
import com.cleanroommc.flare.common.sampler.AbstractSampler;
import com.cleanroommc.flare.common.sampler.ExportProps;
import com.cleanroommc.flare.common.websocket.ViewerSocket;
import com.cleanroommc.flare.util.LangKeys;
import me.lucko.bytesocks.client.BytesocksClient;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.ClickEvent.Action;

public class SamplerViewCommand extends FlareSubCommand {

    public SamplerViewCommand(FlareAPI flare) {
        super(flare);
    }

    @Override
    public String getName() {
        return "view";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/flare sampler view";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        SamplerContainer<Sampler> samplerContainer = this.flare.samplerContainer();
        Sampler sampler = samplerContainer.activeSampler();
        if (sampler == null) {
            sendMessage(sender, LangKeys.SAMPLER_HAS_NOT_STARTED);
            return;
        }
        BytesocksClient bytesocksClient = this.flare.get(BytesocksClient.class);
        if (bytesocksClient == null) {
            sendMessage(sender, LangKeys.SAMPLER_VIEWER_UNSUPPORTED);
            return;
        }
        this.flare.runAsync(() -> {
            try {
                ExportProps exportProps = samplerContainer.getExportProps();
                ViewerSocket viewerSocket = new ViewerSocket(this.flare, bytesocksClient, exportProps);
                // TODO AbstractSampler
                AbstractSampler abstractSampler = (AbstractSampler) sampler;
                abstractSampler.attachSocket(viewerSocket);
                exportProps.channelInfo(viewerSocket.getPayload());

                String url = SamplerUtil.upload(this.flare, exportProps, sampler, true, false);
                sendMessage(sender, LangKeys.SAMPLER_VIEWER_OPEN,
                        msg -> msg.getStyle().setClickEvent(new ClickEvent(Action.OPEN_URL, url)), url);
            } catch (Throwable t) {
                sendMessage(sender, LangKeys.SAMPLER_VIEWER_FAILED_UNEXPECTEDLY, t.toString());
                this.flare.logger().fatal(t);
            }
        });
    }

}
