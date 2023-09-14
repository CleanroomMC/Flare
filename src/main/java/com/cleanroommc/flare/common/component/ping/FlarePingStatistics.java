package com.cleanroommc.flare.common.component.ping;

import com.cleanroommc.flare.api.ping.PingStatistics;
import com.cleanroommc.flare.api.ping.PingSummary;
import com.cleanroommc.flare.api.ping.PlayerPing;
import com.cleanroommc.flare.util.MonitoringExecutor;
import com.cleanroommc.flare.util.RollingAverage;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Provides statistics for player ping RTT to the server.
 */
public class FlarePingStatistics implements PingStatistics, Runnable {

    private static final int QUERY_RATE_SECONDS = 10;
    private static final int WINDOW_SIZE_SECONDS = (int) TimeUnit.MINUTES.toSeconds(15); // 900
    private static final int WINDOW_SIZE = WINDOW_SIZE_SECONDS / QUERY_RATE_SECONDS; // 90

    /** Rolling average of the median ping across all players */
    private final RollingAverage rollingAverage = new RollingAverage(WINDOW_SIZE);

    /** The scheduler task that polls pings and calculates the rolling average */
    private ScheduledFuture<?> future;
    /** Minecraft Server **/
    private MinecraftServer server;

    @Override
    public void run() {
        PingSummary summary = currentSummary();
        if (summary.total() == 0) {
            return;
        }
        this.rollingAverage.add(BigDecimal.valueOf(summary.median()));
    }

    @Override
    public void start(MinecraftServer server) {
        if (this.future != null) {
            throw new IllegalStateException();
        }
        this.server = server;
        this.future = MonitoringExecutor.INSTANCE.scheduleAtFixedRate(this, QUERY_RATE_SECONDS, QUERY_RATE_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        if (this.future != null) {
            this.future.cancel(false);
            this.future = null;
        }
        this.server = null;
    }

    @Override
    public PingSummary currentSummary() {
        int[] values = this.server.getPlayerList().getPlayers().stream().mapToInt(p -> p.ping).filter(i -> i > 0).toArray();
        return values.length == 0 ? PingSummary.EMPTY : new PingSummary(values);
    }

    @Override
    public RollingAverage average() {
        return this.rollingAverage;
    }

    @Override
    public PlayerPing playerPing(String playerName) {
        EntityPlayerMP player = this.server.getPlayerList().getPlayerByUsername(playerName);
        if (player != null) {
            return new PlayerPing(playerName, player.ping);
        }
        List<EntityPlayerMP> players = this.server.getPlayerList().getPlayers();
        for (int i = 0; i < players.size(); i++) {
            player = players.get(i);
            if (player.getName().equalsIgnoreCase(playerName)) {
                return new PlayerPing(player.getName(), player.ping);
            }
        }
        return null;
    }

    @Override
    public PlayerPing playerPing(UUID playerUuid) {
        EntityPlayerMP player = this.server.getPlayerList().getPlayerByUUID(playerUuid);
        if (player != null) {
            return new PlayerPing(player.getName(), player.ping);
        }
        return null;
    }

}
