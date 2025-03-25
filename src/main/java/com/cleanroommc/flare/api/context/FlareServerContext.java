package com.cleanroommc.flare.api.context;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public interface FlareServerContext {

    MinecraftServer server();

    long serverStartTime();

    <T> Future<T> callAsync(Callable<T> runnable);

    void syncWithServer(Runnable runnable);

    default void runAsync(Runnable runnable) {
        callAsync(Executors.callable(runnable));
    }

    default List<World> worlds() {
        if (server() == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(server().worlds);
    }

    default List<Chunk> chunks() {
        return worlds().stream()
                .map(this::chunks)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    default List<Chunk> chunks(World world) {
        return new ArrayList<>(((ChunkProviderServer) world.getChunkProvider()).getLoadedChunks());
    }

    default List<Region> regions(World world) {
        return Region.mapChunks(chunks(world));
    }

    default List<EntityPlayerMP> players() {
        MinecraftServer server = server();
        if (server == null) {
            return Collections.emptyList();
        }
        PlayerList playerList = server.getPlayerList();
        if (playerList == null) {
            return Collections.emptyList();
        }
        return playerList.getPlayers();
    }

    default List<Entity> entities() {
        return worlds().stream()
                .map(this::entities)
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
    }

    default List<Entity> entities(World world) {
        return world.loadedEntityList;
    }

    default List<Entity> entities(Chunk chunk) {
        List<Entity> entities = new ArrayList<>();
        for (ClassInheritanceMultiMap<Entity> map : chunk.getEntityLists()) {
            entities.addAll(map);
        }
        return entities;
    }

    default List<TileEntity> tileEntities() {
        return worlds().stream()
                .map(this::tileEntities)
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
    }

    default List<TileEntity> tileEntities(World world) {
        return world.loadedTileEntityList;
    }

    default List<TileEntity> tileEntities(Chunk chunk) {
        return new ArrayList<>(chunk.getTileEntityMap().values());
    }

}
