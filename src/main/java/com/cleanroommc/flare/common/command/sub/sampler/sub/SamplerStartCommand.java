package com.cleanroommc.flare.common.command.sub.sampler.sub;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.api.FlareClientAPI;
import com.cleanroommc.flare.api.sampler.Sampler;
import com.cleanroommc.flare.api.sampler.SamplerBuilder;
import com.cleanroommc.flare.api.sampler.SamplerContainer;
import com.cleanroommc.flare.api.sampler.SamplerMode;
import com.cleanroommc.flare.api.sampler.thread.ThreadDumper;
import com.cleanroommc.flare.api.sampler.thread.ThreadGrouper;
import com.cleanroommc.flare.common.command.sub.CommandOptions;
import com.cleanroommc.flare.common.command.sub.FlareSubCommand;
import com.cleanroommc.flare.common.sampler.ExportProps;
import com.cleanroommc.flare.util.LangKeys;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;
import java.util.List;
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
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        return CommandOptions.buildTabCompletion(args, CommandOptions.TIMEOUT, CommandOptions.INTERVAL,
                CommandOptions.ALLOC, CommandOptions.ALLOC_LIVE_ONLY, CommandOptions.THREAD,
                CommandOptions.REGEX, CommandOptions.THREAD_GROUPER, CommandOptions.ONLY_TICKS_OVER,
                CommandOptions.IGNORE_SLEEPING, CommandOptions.IGNORE_NATIVE, CommandOptions.FORCE_JAVA_SAMPLER,
                CommandOptions.SEPARATE_PARENT_CALLS, CommandOptions.SAVE_TO_FILE, CommandOptions.COMMENT);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        SamplerContainer<Sampler> samplerContainer = this.flare.samplerContainer();
        Sampler sampler = samplerContainer.activeSampler();
        if (sampler != null) {
            sendMessage(sender, LangKeys.SAMPLER_ALREADY_STARTED);
        } else {
            // ExportProps
            final ExportProps exportProps = samplerContainer.getExportProps();
            setExportProps(exportProps, sender, args);
            // Timeout
            int timeoutSeconds = CommandOptions.TIMEOUT.getValue(args);
            if (timeoutSeconds != -1) {
                if (timeoutSeconds <= 10) {
                    sendMessage(sender, LangKeys.SAMPLER_START_TIMEOUT_TOO_SHORT);
                    return;
                } else if (timeoutSeconds < 30) {
                    sendMessage(sender, LangKeys.SAMPLER_START_TIMEOUT_SHORT);
                }
            }
            // SamplerMode
            SamplerMode mode = CommandOptions.ALLOC.hasIn(args) ? SamplerMode.ALLOCATION : SamplerMode.EXECUTION;
            // Allocate Live Only
            boolean allocLiveOnly = CommandOptions.ALLOC_LIVE_ONLY.hasIn(args);
            // Interval
            int interval = CommandOptions.INTERVAL.getValue(args);
            if (interval <= mode.interval()) {
                interval = mode.interval();
            }
            // ThreadDumper
            Set<String> threads = CommandOptions.THREAD.getValue(args);
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
                Set<String> regex = CommandOptions.REGEX.getValue(args);
                if (!regex.isEmpty()) {
                    threadDumper = new ThreadDumper.Regex(regex);
                } else {
                    // Specific matches
                    threadDumper = new ThreadDumper.Specific(threads);
                }
            }
            // ThreadGrouper
            ThreadGrouper threadGrouper = ThreadGrouper.BY_POOL;
            String threadGrouperValue = CommandOptions.THREAD_GROUPER.getValue(args);
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
            int ticksOver = CommandOptions.ONLY_TICKS_OVER.getValue(args);
            boolean ignoreSleeping = CommandOptions.IGNORE_SLEEPING.hasIn(args);
            boolean ignoreNative = CommandOptions.IGNORE_NATIVE.hasIn(args);
            boolean forceJavaSampler = CommandOptions.FORCE_JAVA_SAMPLER.hasIn(args);
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
            sampler.whenComplete((s, t) -> {
                if (t != null) {
                    sendMessage(sender, LangKeys.SAMPLER_FAILED_UNEXPECTEDLY, t.toString());
                    this.flare.logger().fatal(t);
                } else {
                    sendMessage(sender, LangKeys.SAMPLER_STOPPING);
                    s = samplerContainer.stopSampler(false);
                    SamplerUtil.upload(this.flare, exportProps,
                            (k, c, a) -> sendMessage(sender, k, c, a), s, false, true);
                }
            });
            sendMessage(sender, LangKeys.SAMPLER_START);
            sampler.start();
        }
    }

    private void setExportProps(ExportProps exportProps, ICommandSender sender, String[] args) {
        exportProps.creator(sender)
                .separateParentCalls(CommandOptions.SEPARATE_PARENT_CALLS.hasIn(args))
                .saveToFile(CommandOptions.SAVE_TO_FILE.hasIn(args));
        ExportProps.setDefault(this.flare, exportProps);
        String comment = CommandOptions.COMMENT.getValue(args);
        if (comment != null) {
            exportProps.comment(comment);
        }
    }

}
