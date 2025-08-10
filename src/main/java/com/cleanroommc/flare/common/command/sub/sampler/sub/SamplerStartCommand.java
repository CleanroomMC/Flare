package com.cleanroommc.flare.common.command.sub.sampler.sub;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.api.FlareClientAPI;
import com.cleanroommc.flare.api.sampler.Sampler;
import com.cleanroommc.flare.api.sampler.SamplerBuilder;
import com.cleanroommc.flare.api.sampler.SamplerContainer;
import com.cleanroommc.flare.api.sampler.SamplerMode;
import com.cleanroommc.flare.api.sampler.thread.ThreadDumper;
import com.cleanroommc.flare.api.sampler.thread.ThreadGrouper;
import com.cleanroommc.flare.common.command.sub.FlareSubCommand;
import com.cleanroommc.flare.common.sampler.ExportProps;
import com.cleanroommc.flare.util.LangKeys;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public class SamplerStartCommand extends FlareSubCommand {

    private final Side side;

    public SamplerStartCommand(Side side, FlareAPI flare) {
        super(flare);
        this.side = side;
    }

    @Override
    public String getName() {
        return "start";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/flare sampler start [--arguments]";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        SamplerContainer<Sampler> samplerContainer = this.flare.samplerContainer();
        Sampler sampler = samplerContainer.activeSampler();
        if (sampler != null) {
            sendMessage(sender, LangKeys.SAMPLER_ALREADY_STARTED);
        } else {
            // ExportProps
            setExportProps(samplerContainer.getExportProps(), sender, args);
            // Timeout
            int timeoutSeconds = getIntArgValue(args, "timeout");
            if (timeoutSeconds != -1) {
                if (timeoutSeconds <= 10) {
                    sendMessage(sender, LangKeys.SAMPLER_START_TIMEOUT_TOO_SHORT);
                    return;
                } else if (timeoutSeconds < 30) {
                    sendMessage(sender, LangKeys.SAMPLER_START_TIMEOUT_SHORT);
                }
            }
            // SamplerMode
            SamplerMode mode = hasArg(args, "alloc") ? SamplerMode.ALLOCATION : SamplerMode.EXECUTION;
            // Allocate Live Only
            boolean allocLiveOnly = hasArg(args, "alloc-live-only");
            // Interval
            int interval = getIntArgValue(args, "interval");
            if (interval <= mode.interval()) {
                interval = mode.interval();
            }
            // ThreadDumper
            Set<String> threads = getArgValues(args, "thread");
            ThreadDumper threadDumper;
            if (threads.isEmpty()) {
                if (this.side.isClient()) {
                    threadDumper = ((FlareClientAPI) this.flare).clientThreadDumper();
                } else {
                    threadDumper = this.flare.serverThreadDumper();
                }
            } else if (threads.contains("*")) {
                threadDumper = ThreadDumper.ALL;
            } else {
                Set<String> regex = getArgValues(args, "regex");
                if (!regex.isEmpty()) {
                    threadDumper = new ThreadDumper.Regex(regex);
                } else {
                    // Specific matches
                    threadDumper = new ThreadDumper.Specific(threads);
                }
            }
            // ThreadGrouper
            ThreadGrouper threadGrouper = ThreadGrouper.BY_POOL;
            String threadGrouperValue = getArgValue(args, "thread-grouper");
            if (threadGrouperValue != null) {
                switch (threadGrouperValue) {
                    case "combine-all":
                        threadGrouper = ThreadGrouper.AS_ONE;
                        break;
                    case "not-combined":
                        threadGrouper = ThreadGrouper.BY_NAME;
                        break;
                }
            }
            int ticksOver = getIntArgValue(args, "only-ticks-over");
            boolean ignoreSleeping = hasArg(args, "ignore-sleeping");
            boolean ignoreNative = hasArg(args, "ignore-native");
            boolean forceJavaSampler = hasArg(args, "force-java-sampler");
            SamplerBuilder samplerBuilder = this.flare.samplerBuilder()
                    .mode(mode)
                    .interval(interval)
                    .allocLiveOnly(allocLiveOnly)
                    .threadDumper(threadDumper)
                    .threadGrouper(threadGrouper)
                    .ignoreSleeping(ignoreSleeping)
                    .ignoreNative(ignoreNative)
                    .forceJavaSampler(forceJavaSampler);
            if (ticksOver != -1) {
                samplerBuilder.ticksOver(ticksOver, this.flare.tickRoutine(this.side));
            }
            if (timeoutSeconds != -1) {
                samplerBuilder.completeAfter(timeoutSeconds, TimeUnit.SECONDS);
            }
            sampler = samplerBuilder.build();
            samplerContainer.setSampler(sampler);
            sendMessage(sender, LangKeys.SAMPLER_START);
            sampler.start();
        }
    }

    private void setExportProps(ExportProps exportProps, ICommandSender sender, String[] args) {
        exportProps.creator(sender)
                .separateParentCalls(hasArg(args, "separate-parent-calls"))
                .saveToFile(hasArg(args, "save-to-file"));
        ExportProps.setDefault(this.flare, exportProps);
        String comment = getArgValue(args, "comment");
        if (comment != null) {
            exportProps.comment(comment);
        }
    }

}
