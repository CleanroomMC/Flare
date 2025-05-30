package com.cleanroommc.flare.common.command.sub.component;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.api.tick.TickStatistics;
import com.cleanroommc.flare.api.tick.TickType;
import com.cleanroommc.flare.common.command.sub.FlareSubCommand;
import com.cleanroommc.flare.common.component.cpu.CpuMonitor;
import com.cleanroommc.flare.common.component.disk.DiskUsage;
import com.cleanroommc.flare.common.component.network.Direction;
import com.cleanroommc.flare.common.component.network.NetworkInterfaceAverages;
import com.cleanroommc.flare.common.component.network.NetworkMonitor;
import com.cleanroommc.flare.util.FormatUtil;
import com.cleanroommc.flare.util.LangKeys;
import com.cleanroommc.flare.util.StatisticFormatter;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;
import java.lang.management.*;
import java.util.*;

public class HealthCommand extends FlareSubCommand {

    public HealthCommand(FlareAPI flare) {
        super(flare);
    }

    @Override
    public String getName() {
        return "health";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("healthreport", "ht", "hr");
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/flare health [--memory] OR [--network]";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        return Arrays.asList("--memory", "--network");
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        sendTickStatistics(sender);
        sendCpuStatistics(sender);
        sendMemoryStatistics(sender, args);
        sendNetworkStatistics(sender, args);
        sendDiskStatistics(sender);
    }

    private void sendTickStatistics(ICommandSender sender) {
        TickStatistics stats = this.flare.tickStatistics(Side.SERVER, TickType.ALL);
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
    }

    private void sendCpuStatistics(ICommandSender sender) {
        sendMessage(sender, LangKeys.CPU_USAGE_SYSTEM_LOAD,
                StatisticFormatter.formatCpuUsage(CpuMonitor.systemLoad10SecAvg()),
                StatisticFormatter.formatCpuUsage(CpuMonitor.systemLoad1MinAvg()),
                StatisticFormatter.formatCpuUsage(CpuMonitor.systemLoad15MinAvg()));
        sendMessage(sender, LangKeys.CPU_USAGE_PROCESS_LOAD,
                StatisticFormatter.formatCpuUsage(CpuMonitor.processLoad10SecAvg()),
                StatisticFormatter.formatCpuUsage(CpuMonitor.processLoad1MinAvg()),
                StatisticFormatter.formatCpuUsage(CpuMonitor.processLoad15MinAvg()));
    }

    private void sendMemoryStatistics(ICommandSender sender, String[] args) {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        sendMessage(sender, LangKeys.BASIC_MEMORY_USAGE_REPORT,
                FormatUtil.formatBytes(heapUsage.getUsed()),
                FormatUtil.formatBytes(heapUsage.getMax()),
                FormatUtil.percent(heapUsage.getUsed(), heapUsage.getMax()),
                StatisticFormatter.generateMemoryUsageDiagram(heapUsage, 60));

        if (hasArg(args, "memory")) {
            MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
            sendMessage(sender, LangKeys.DETAILED_MEMORY_USAGE_REPORT, FormatUtil.formatBytes(nonHeapUsage.getUsed()));
            for (MemoryPoolMXBean memoryPool : ManagementFactory.getMemoryPoolMXBeans()) {
                if (memoryPool.getType() == MemoryType.NON_HEAP) {
                    MemoryUsage usage = memoryPool.getUsage();
                    MemoryUsage collectionUsage = memoryPool.getCollectionUsage();
                    if (usage.getMax() == -1) {
                        usage = new MemoryUsage(usage.getInit(), usage.getUsed(), usage.getCommitted(), usage.getCommitted());
                    }
                    sendMessage(sender, LangKeys.MEMORY_POOL_REPORT,
                            memoryPool.getName(),
                            FormatUtil.formatBytes(usage.getUsed()),
                            FormatUtil.formatBytes(usage.getMax()),
                            FormatUtil.percent(usage.getUsed(), usage.getMax()),
                            StatisticFormatter.generateMemoryPoolDiagram(usage, collectionUsage, 60));
                    if (collectionUsage != null) {
                        sendMessage(sender, LangKeys.MEMORY_POOL_COLLECTION_USAGE_REPORT, FormatUtil.formatBytes(collectionUsage.getUsed()));
                    }
                }
            }
        }
    }

    private void sendNetworkStatistics(ICommandSender sender, String[] args) {
        boolean detailed = hasArg(args, "network");
        List<Runnable> body = new ArrayList<>();
        for (Map.Entry<String, NetworkInterfaceAverages> entry : NetworkMonitor.systemAverages().entrySet()) {
            String interfaceName = entry.getKey();
            NetworkInterfaceAverages averages = entry.getValue();
            for (Direction direction : Direction.values()) {
                long bytesPerSec = (long) averages.bytesPerSecond(direction).mean();
                long packetsPerSec = (long) averages.packetsPerSecond(direction).mean();
                if (detailed || bytesPerSec > 0 || packetsPerSec > 0) {
                    body.add(() -> sendMessage(sender, LangKeys.NETWORKING_REPORT_BODY,
                            FormatUtil.formatBytes(bytesPerSec, TextFormatting.GREEN, "/s"),
                            String.format(Locale.ENGLISH, "%,d", packetsPerSec),
                            interfaceName,
                            direction.abbrev()));
                }
            }
        }
        if (!body.isEmpty()) {
            sendMessage(sender, LangKeys.NETWORKING_REPORT_HEADER);
            body.forEach(Runnable::run);
        }
    }

    private void sendDiskStatistics(ICommandSender sender) {
        long total = DiskUsage.getTotal();
        long used = DiskUsage.getUsed();
        sendMessage(sender, LangKeys.DISK_USAGE_REPORT,
                FormatUtil.formatBytes(used),
                FormatUtil.formatBytes(total),
                FormatUtil.percent(used, total),
                StatisticFormatter.generateDiskUsageDiagram(used, total, 60));
    }

}
