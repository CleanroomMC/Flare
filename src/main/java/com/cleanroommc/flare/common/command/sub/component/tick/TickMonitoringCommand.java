package com.cleanroommc.flare.common.command.sub.component.tick;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.api.tick.TickRoutine;
import com.cleanroommc.flare.api.tick.TickType;
import com.cleanroommc.flare.common.command.sub.FlareSubCommand;
import com.cleanroommc.flare.common.component.tick.ReportPredicate;
import com.cleanroommc.flare.common.component.tick.TickMonitor;
import com.cleanroommc.flare.util.LangKeys;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.relauncher.Side;

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
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (this.activeTickMonitor != null) {
            this.activeTickMonitor.stop();
            this.activeTickMonitor = null;
            sendMessage(sender, LangKeys.TICK_MONITORING_DISABLED);
            return;
        }

        TickRoutine tickRoutine = this.flare.tickRoutine(side);

        ReportPredicate reportPredicate = null;
        String thresholdArg = getArgValue(args, "threshold");
        if (thresholdArg != null) {
            try {
                reportPredicate = new ReportPredicate.PercentageChangeGt(Integer.parseInt(thresholdArg));
            } catch (NumberFormatException e) {
                sendMessage(sender, LangKeys.MALFORMED_INPUTS, "threshold");
            }
        } else {
            String thresholdTickArg = getArgValue(args, "threshold-tick");
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

        this.activeTickMonitor = new TickMonitor(this.flare, this.side, TickType.ALL, tickRoutine, reportPredicate, hasArg(args, "without-gc"));
        this.activeTickMonitor.start(sender);
    }

}
