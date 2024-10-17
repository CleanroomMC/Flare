package com.cleanroommc.flare.common.command.sub.component.ping;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.api.ping.PingStatistics;
import com.cleanroommc.flare.api.ping.PingSummary;
import com.cleanroommc.flare.api.ping.PlayerPing;
import com.cleanroommc.flare.common.command.sub.FlareSubCommand;
import com.cleanroommc.flare.util.LangKeys;
import com.cleanroommc.flare.util.RollingAverage;
import com.cleanroommc.flare.util.StatisticFormatter;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;

import javax.annotation.Nullable;
import java.util.*;

public class PingCommand extends FlareSubCommand {

    public PingCommand(FlareAPI flare) {
        super(flare);
    }

    @Override
    public String getName() {
        return "ping";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/flare ping (player name) OR (player uuid)";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        return Arrays.asList(server.getPlayerList().getOnlinePlayerNames());
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (FMLLaunchHandler.side().isClient() && server instanceof IntegratedServer && !((IntegratedServer) server).getPublic()) {
            sendMessage(sender, LangKeys.PING_STATISTICS_SINGLEPLAYER);
        } else {
            PingStatistics stats = this.flare.pingStats();
            if (args.length > 0) {
                String flag = args[0];
                if (!flag.equalsIgnoreCase("player")) {
                    sendMessage(sender, LangKeys.MALFORMED_FLAGS, "player");
                    return;
                }
                if (args.length == 1) {
                    sendMessage(sender, LangKeys.NO_INPUTS, "player");
                    return;
                }
                for (int i = 1; i < args.length; i++) {
                    String input = args[i];
                    EntityPlayerMP player = getPlayer(server, sender, input); // Allow it to fail
                    PlayerPing ping = stats.playerPing(player);
                    sendMessage(sender, LangKeys.PING_STATISTICS_SPECIFIC_PLAYER, ping.name(), ping.ping());
                }
            } else {
                PingSummary summary = stats.currentSummary();
                RollingAverage average = (RollingAverage) stats.average();
                if (summary.total() == 0 && average.getSamples() == 0) {
                    sendMessage(sender, LangKeys.PING_STATISTICS_NOT_ENOUGH_DATA);
                    return;
                }
                sendMessage(sender, LangKeys.PING_STATISTICS_AVERAGES,
                        StatisticFormatter.formatPingRtts(summary.min(), summary.median(), summary.percentile95th(), summary.max()),
                        StatisticFormatter.formatPingRtts(average.min(), average.median(), average.percentile95th(), average.max()));
            }
        }
    }

}
