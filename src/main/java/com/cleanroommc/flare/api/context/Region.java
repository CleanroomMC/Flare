package com.cleanroommc.flare.api.context;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.entity.Entity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.world.chunk.Chunk;

import java.util.*;

public final class Region {

    private static final int DISTANCE_THRESHOLD = 2;

    public static List<Region> mapChunks(List<Chunk> chunks) {
        List<Region> regions = new ArrayList<>();
        for (Chunk chunk : chunks) {
            for (ClassInheritanceMultiMap<Entity> map : chunk.getEntityLists()) {
                if (!map.isEmpty()) {
                    boolean found = false;
                    for (Region region : regions) {
                        if (region.isAdjacent(chunk)) {
                            found = true;
                            region.add(chunk);

                            // If the chunk is adjacent to more than one region, merge the regions together
                            for (Iterator<Region> iterator = regions.iterator(); iterator.hasNext();) {
                                Region otherRegion = iterator.next();
                                if (region != otherRegion && otherRegion.isAdjacent(chunk)) {
                                    iterator.remove();
                                    region.merge(otherRegion);
                                }
                            }
                            break;
                        }
                    }
                    if (!found) {
                        regions.add(new Region(chunk));
                    }
                }
            }
        }
        return regions;
    }

    private static long squaredEuclideanDistance(Chunk a, Chunk b) {
        long dx = a.x - b.x;
        long dz = a.z - b.z;
        return (dx * dx) + (dz * dz);
    }

    private final Set<Chunk> chunks;

    private int totalEntities = -1;

    private Region(Chunk chunk) {
        this.chunks = new ObjectOpenHashSet<>();
        this.chunks.add(chunk);
    }

    public Collection<Chunk> chunks() {
        return Collections.unmodifiableCollection(chunks);
    }

    public boolean isAdjacent(Chunk other) {
        for (Chunk chunk : this.chunks) {
            if (squaredEuclideanDistance(chunk, other) <= DISTANCE_THRESHOLD) {
                return true;
            }
        }
        return false;
    }

    public int totalEntities() {
        if (this.totalEntities == -1) {
            this.totalEntities = 0;
            for (Chunk chunk : this.chunks) {
                for (ClassInheritanceMultiMap<Entity> map : chunk.getEntityLists()) {
                    this.totalEntities += map.size();
                }
            }
        }
        return totalEntities;
    }

    private void add(Chunk chunk) {
        this.chunks.add(chunk);
    }

    private void merge(Region region) {
        this.chunks.addAll(region.chunks);
    }

}
