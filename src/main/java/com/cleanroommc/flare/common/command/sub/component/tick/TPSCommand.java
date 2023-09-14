package com.cleanroommc.flare.common.command.sub.component.tick;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.api.tick.TickStatistics;
import com.cleanroommc.flare.common.command.sub.FlareSubCommand;
import com.cleanroommc.flare.common.component.cpu.CpuMonitor;
import com.cleanroommc.flare.util.LangKeys;
import com.cleanroommc.flare.util.StatisticFormatter;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

import java.util.Collections;
import java.util.List;

public class TPSCommand extends FlareSubCommand {

    public TPSCommand(FlareAPI flare) {
        super(flare);
    }

    @Override
    public String getName() {
        return "tps";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("cpu");
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/flare tps";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        TickStatistics stats = this.flare.tickStats();
        sendMessage(sender, LangKeys.TPS_STATISTICS_RECALL,
                StatisticFormatter.formatTps(stats.tps5Sec()),
                StatisticFormatter.formatTps(stats.tps10Sec()),
                StatisticFormatter.formatTps(stats.tps1Min()),
                StatisticFormatter.formatTps(stats.tps5Min()),
                StatisticFormatter.formatTps(stats.tps15Min()));
        if (stats.isDurationSupported()) {
            sendMessage(sender, LangKeys.TPS_STATISTICS_DURATION_AVERAGES,
                    StatisticFormatter.formatTickDurations(stats.duration10Sec()),
                    StatisticFormatter.formatTickDurations(stats.duration1Min()));
        }
        sendMessage(sender, LangKeys.CPU_USAGE_SYSTEM_LOAD,
                StatisticFormatter.formatCpuUsage(CpuMonitor.systemLoad10SecAvg()),
                StatisticFormatter.formatCpuUsage(CpuMonitor.systemLoad1MinAvg()),
                StatisticFormatter.formatCpuUsage(CpuMonitor.systemLoad15MinAvg()));
        sendMessage(sender, LangKeys.CPU_USAGE_SYSTEM_LOAD,
                StatisticFormatter.formatCpuUsage(CpuMonitor.processLoad10SecAvg()),
                StatisticFormatter.formatCpuUsage(CpuMonitor.processLoad1MinAvg()),
                StatisticFormatter.formatCpuUsage(CpuMonitor.processLoad15MinAvg()));
    }

}
