package com.cleanroommc.flare.api.ping;

import com.cleanroommc.flare.api.util.RollingDoubleAverageInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

public interface PingStatistics {

    void start(MinecraftServer server);

    void stop();

    PingSummary currentSummary();

    RollingDoubleAverageInfo average();

    PlayerPing playerPing(String playerName);

    PlayerPing playerPing(UUID playerUuid);

    default PlayerPing playerPing(EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            return new PlayerPing(player.getName(), ((EntityPlayerMP) player).ping);
        }
        return playerPing(player.getUniqueID());
    }

}
