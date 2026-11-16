package com.cleanroommc.flare.common.command.sub.component.tick;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.api.tick.TickRoutine;
import com.cleanroommc.flare.api.tick.TickType;
import com.cleanroommc.flare.common.command.sub.CommandOptions;
import com.cleanroommc.flare.common.command.sub.FlareSubCommand;
import com.cleanroommc.flare.common.component.tick.ReportPredicate;
import com.cleanroommc.flare.common.component.tick.TickMonitor;
import com.cleanroommc.flare.util.LangKeys;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class TickMonitoringCommand extends FlareSubCommand {

    private final Side side;
    /** The tick hook instance currently running, if any */
    private TickMonitor activeTickMonitor = null;

    public TickMonitoringCommand(Side side, FlareAPI flare) {
        super(flare);
        this.side = side;
    }

    @Override
    public String getName() {
        return "tickmonitor";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("tickmonitoring", "tm");
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/flare tickmonitor [--threshold (integer) / --threshold-tick (integer)], [--without-gc]";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        return CommandOptions.buildTabCompletion(args, CommandOptions.THRESHOLD, CommandOptions.THRESHOLD_TICK, CommandOptions.WITHOUT_GC);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (this.activeTickMonitor != null) {
            this.activeTickMonitor.stop();
            this.activeTickMonitor = null;
            sendMessage(sender, LangKeys.TICK_MONITORING_DISABLED);
            return;
        }

        TickRoutine tickRoutine = this.flare.tickRoutine(side);

        ReportPredicate reportPredicate = null;
        String thresholdArg = CommandOptions.THRESHOLD.getValue(args);
        if (thresholdArg != null) {
            try {
                reportPredicate = new ReportPredicate.PercentageChangeGt(Integer.parseInt(thresholdArg));
            } catch (NumberFormatException e) {
                sendMessage(sender, LangKeys.MALFORMED_INPUTS, "threshold");
            }
        } else {
            String thresholdTickArg = CommandOptions.THRESHOLD_TICK.getValue(args);
            if (thresholdTickArg != null) {
                try {
                    reportPredicate = new ReportPredicate.DurationGt(Integer.parseInt(thresholdTickArg));
                } catch (NumberFormatException e) {
                    sendMessage(sender, LangKeys.MALFORMED_INPUTS, "threshold-tick");
                }
            }
        }
        if (reportPredicate == null) {
            reportPredicate = new ReportPredicate.PercentageChangeGt(100);
        }

        this.activeTickMonitor = new TickMonitor(this.flare, this.side, TickType.ALL, tickRoutine, reportPredicate, CommandOptions.WITHOUT_GC.hasIn(args));
        this.activeTickMonitor.start(sender);
    }

}
