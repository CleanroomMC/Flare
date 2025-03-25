package com.cleanroommc.flare.util;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.api.context.Region;
import com.cleanroommc.flare.api.ping.PingStatistics;
import com.cleanroommc.flare.api.sampler.SamplerMode;
import com.cleanroommc.flare.api.sampler.node.MethodDescriptorResolver;
import com.cleanroommc.flare.api.sampler.node.type.StackTraceNode;
import com.cleanroommc.flare.api.sampler.node.type.ThreadNode;
import com.cleanroommc.flare.api.sampler.thread.ThreadDumper;
import com.cleanroommc.flare.api.sampler.thread.ThreadGrouper;
import com.cleanroommc.flare.api.tick.TickStatistics;
import com.cleanroommc.flare.api.tick.TickType;
import com.cleanroommc.flare.api.util.DoubleAverageInfo;
import com.cleanroommc.flare.common.component.cpu.CpuInfo;
import com.cleanroommc.flare.common.component.cpu.CpuMonitor;
import com.cleanroommc.flare.common.component.disk.DiskUsage;
import com.cleanroommc.flare.common.component.gpu.GpuInfo;
import com.cleanroommc.flare.common.component.memory.MemoryInfo;
import com.cleanroommc.flare.common.component.memory.heap.dump.HeapDumpSummary;
import com.cleanroommc.flare.common.component.memory.heap.dump.HeapDumpSummary.Entry;
import com.cleanroommc.flare.common.component.memory.heap.gc.GarbageCollectorStatistics;
import com.cleanroommc.flare.common.component.network.NetworkInterfaceAverages;
import com.cleanroommc.flare.common.component.network.NetworkMonitor;
import com.cleanroommc.flare.common.component.os.OperatingSystemInfo;
import com.cleanroommc.flare.api.sampler.source.SourceMetadata;
import com.cleanroommc.flare.common.sampler.window.ProtoTimeEncoder;
import com.cleanroommc.flare.proto.FlareHeapProtos.HeapData;
import com.cleanroommc.flare.proto.FlareHeapProtos.HeapEntry;
import com.cleanroommc.flare.proto.FlareHeapProtos.HeapMetadata;
import com.cleanroommc.flare.proto.FlareProtos.CommandSenderMetadata;
import com.cleanroommc.flare.proto.FlareProtos.RollingAverageValues;
import com.cleanroommc.flare.proto.FlareProtos.SystemStatistics;
import com.cleanroommc.flare.proto.FlareProtos.WorldStatistics;
import com.cleanroommc.flare.proto.FlareProtos.PlatformMetadata;
import com.cleanroommc.flare.proto.FlareProtos.PlatformStatistics;
import com.cleanroommc.flare.proto.FlareSamplerProtos;
import com.cleanroommc.flare.proto.FlareSamplerProtos.SamplerMetadata;
import com.cleanroommc.flare.proto.FlareSamplerProtos.SamplerMetadata.DataAggregator;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ProtoUtil {

    public static PlatformMetadata getPlatformMetadataProto() {
        return PlatformMetadata.newBuilder()
                .setName(LoaderUtil.getName())
                .setType(FMLLaunchHandler.side().isClient() ? PlatformMetadata.Type.CLIENT : PlatformMetadata.Type.SERVER)
                .setMinecraftVersion("1.12.2")
                .setSparkVersion(2)
                .setVersion(LoaderUtil.getVersion())
                .build();
    }

    public static CommandSenderMetadata getCommandSenderProto(ICommandSender sender) {
        if (sender == null) {
            sender = new DummyCommandSender();
        }
        CommandSenderMetadata .Builder builder = CommandSenderMetadata.newBuilder().setName(sender.getName());
        if (sender instanceof EntityPlayer) {
            builder.setType(CommandSenderMetadata.Type.PLAYER).setUniqueId(((EntityPlayer) sender).getUniqueID().toString());
        } else {
            builder.setType(CommandSenderMetadata.Type.OTHER);
        }
        return builder.build();
    }

    public static PlatformStatistics getPlatformStatsProto(FlareAPI flare, Side side, boolean includeWorld,
                                                           @Nullable Map<String, GarbageCollectorStatistics> startingGcCollectorStats) {
        PlatformStatistics.Builder builder = PlatformStatistics.newBuilder();
        builder.setMemory(getHeapProto());
        long uptime = System.currentTimeMillis() - flare.serverStartTime();
        builder.setUptime(uptime);
        if (startingGcCollectorStats != null) {
            GarbageCollectorStatistics.pollStatsSubtractInitial(startingGcCollectorStats)
                    .forEach((beanName, gcCollectorStats) ->
                            builder.putGc(beanName, getPlatformGcProto(gcCollectorStats, uptime)));
        }
        TickStatistics tickStats = flare.tickStatistics(side, TickType.ALL);
        if (tickStats != null) {
            builder.setTps(getTpsProto(tickStats));
            if (tickStats.isDurationSupported()) {
                builder.setMspt(getMsptProto(tickStats));
            }
        }
        PingStatistics pingStats = flare.pingStats();
        if (pingStats != null && pingStats.average().getSamples() != 0) {
            builder.setPing(getPingProto(pingStats));
        }
        builder.setPlayerCount(flare.players().size());
        if (includeWorld) {
            WorldStatistics worldStats = getWorldStatsProto(flare);
            if (worldStats != null) {
                builder.setWorld(worldStats);
            }
        }
        return builder.build();
    }

    public static WorldStatistics getWorldStatsProto(FlareAPI flare) {
        if (flare.server() == null) {
            return null;
        }
        WorldStatistics.Builder statsBuilder = WorldStatistics.newBuilder();
        int totalServerEntities = 0;
        Map<String, Integer> entities = new Object2ObjectOpenHashMap<>();
        for (World world : flare.worlds()) {
            WorldStatistics.World.Builder worldBuilder = WorldStatistics.World.newBuilder();
            worldBuilder.setName(world.provider.getDimensionType().name());
            List<Region> regions = flare.regions(world);
            int totalWorldEntities = 0;
            for (Region region : regions) {
                worldBuilder.addRegions(getRegionProto(flare, region, entities));
                totalWorldEntities += region.totalEntities();
            }
            worldBuilder.setTotalEntities(totalWorldEntities);
            totalServerEntities += totalWorldEntities;
            statsBuilder.addWorlds(worldBuilder);
        }
        statsBuilder.setTotalEntities(totalServerEntities);
        statsBuilder.putAllEntityCounts(entities);
        return statsBuilder.build();
    }

    public static PlatformStatistics.Tps getTpsProto(TickStatistics tickStats) {
        return PlatformStatistics.Tps.newBuilder()
                .setLast1M(tickStats.tps1Min())
                .setLast5M(tickStats.tps5Min())
                .setLast15M(tickStats.tps15Min())
                .build();
    }

    public static PlatformStatistics.Mspt getMsptProto(TickStatistics tickStats) {
        return PlatformStatistics.Mspt.newBuilder()
                .setLast1M(rollingAvgProto(tickStats.duration1Min()))
                .setLast5M(rollingAvgProto(tickStats.duration5Min()))
                .build();
    }

    public static PlatformStatistics.Ping getPingProto(PingStatistics pingStats) {
        return PlatformStatistics.Ping.newBuilder().setLast15M(rollingAvgProto(pingStats.average())).build();
    }

    public static SystemStatistics getSystemStatsProto() {
        SystemStatistics.Builder builder = SystemStatistics.newBuilder()
                .setCpu(getCpuProto())
                .setMemory(getMemoryProto())
                .setDisk(getDiskProto())
                .setOs(getOsProto())
                .setJava(getJavaProto());
        final long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        builder.setUptime(uptime);
        GarbageCollectorStatistics.pollStats().forEach((beanName, gcCollectorStats) ->
                builder.putGc(beanName, getSystemGcProto(gcCollectorStats, uptime)));
        NetworkMonitor.systemAverages().forEach((name, networkInterfaceAverages) ->
                builder.putNet(name, getNetInterfaceProto(networkInterfaceAverages)));
        return builder.build();
    }

    // TODO: query if a GPU section is possible
    public static SystemStatistics.Cpu getCpuProto() {
        return SystemStatistics.Cpu.newBuilder()
                .setThreads(Runtime.getRuntime().availableProcessors())
                .setProcessUsage(getProcessLoadProto())
                .setSystemUsage(getSystemLoadProto())
                .setModelName(CpuInfo.queryCpuModel())
                .build();
    }

    public static SystemStatistics.Cpu.Usage getProcessLoadProto() {
        return SystemStatistics.Cpu.Usage.newBuilder()
                .setLast1M(CpuMonitor.processLoad1MinAvg())
                .setLast15M(CpuMonitor.processLoad15MinAvg())
                .build();
    }

    public static SystemStatistics.Cpu.Usage getSystemLoadProto() {
        return SystemStatistics.Cpu.Usage.newBuilder()
                .setLast1M(CpuMonitor.systemLoad1MinAvg())
                .setLast15M(CpuMonitor.systemLoad15MinAvg())
                .build();
    }

    public static SystemStatistics.Memory getMemoryProto() {
        return SystemStatistics.Memory.newBuilder()
                .setPhysical(getMemoryPoolProto(true))
                .setSwap(getMemoryPoolProto(false))
                .build();
    }

    public static PlatformStatistics.Memory getHeapProto() {
        return PlatformStatistics.Memory.newBuilder().setHeap(getHeapMemoryPoolProto()).build();
    }

    public static SystemStatistics.Memory.MemoryPool getMemoryPoolProto(boolean physical) {
        return SystemStatistics.Memory.MemoryPool.newBuilder()
                .setUsed(physical ? MemoryInfo.getUsedPhysicalMemory() : MemoryInfo.getUsedSwap())
                .setTotal(physical ? MemoryInfo.getTotalPhysicalMemory() : MemoryInfo.getTotalSwap())
                .build();
    }

    public static PlatformStatistics.Memory.MemoryPool getHeapMemoryPoolProto() {
        MemoryUsage memoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        return PlatformStatistics.Memory.MemoryPool.newBuilder()
                .setUsed(memoryUsage.getUsed())
                .setTotal(memoryUsage.getCommitted())
                .build();
    }

    public static SystemStatistics.Disk getDiskProto() {
        return SystemStatistics.Disk.newBuilder().setTotal(DiskUsage.getTotal()).setUsed(DiskUsage.getUsed()).build();
    }

    public static SystemStatistics.Os getOsProto() {
        OperatingSystemInfo osInfo = OperatingSystemInfo.poll();
        return SystemStatistics.Os.newBuilder()
                .setArch(osInfo.arch())
                .setName(osInfo.name())
                .setVersion(osInfo.version())
                .build();
    }

    public static SystemStatistics.Java getJavaProto() {
        return SystemStatistics.Java.newBuilder()
                .setVendor(System.getProperty("java.vendor", "unknown"))
                .setVersion(System.getProperty("java.version", "unknown"))
                .setVendorVersion(System.getProperty("java.vendor.version", "unknown"))
                .setVmArgs(String.join(" ", ManagementFactory.getRuntimeMXBean().getInputArguments()))
                .build();
    }

    public static PlatformStatistics.Gc getPlatformGcProto(GarbageCollectorStatistics gcCollectorStats, long uptime) {
        return PlatformStatistics.Gc.newBuilder()
                .setTotal(gcCollectorStats.getCollectionCount())
                .setAvgTime(gcCollectorStats.getAverageCollectionTime())
                .setAvgFrequency(gcCollectorStats.getAverageCollectionFrequency(uptime))
                .build();
    }

    public static SystemStatistics.Gc getSystemGcProto(GarbageCollectorStatistics gcCollectorStats, long uptime) {
        return SystemStatistics.Gc.newBuilder()
                .setTotal(gcCollectorStats.getCollectionCount())
                .setAvgTime(gcCollectorStats.getAverageCollectionTime())
                .setAvgFrequency(gcCollectorStats.getAverageCollectionFrequency(uptime))
                .build();
    }

    public static SystemStatistics.NetInterface getNetInterfaceProto(NetworkInterfaceAverages networkInterfaceAverages) {
        return SystemStatistics.NetInterface.newBuilder()
                .setRxBytesPerSecond(rollingAvgProto(networkInterfaceAverages.rxBytesPerSecond()))
                .setRxPacketsPerSecond(rollingAvgProto(networkInterfaceAverages.rxPacketsPerSecond()))
                .setTxBytesPerSecond(rollingAvgProto(networkInterfaceAverages.txBytesPerSecond()))
                .setTxPacketsPerSecond(rollingAvgProto(networkInterfaceAverages.txPacketsPerSecond()))
                .build();
    }

    public static RollingAverageValues rollingAvgProto(DoubleAverageInfo info) {
        return RollingAverageValues.newBuilder()
                .setMean(info.mean())
                .setMax(info.max())
                .setMin(info.min())
                .setMedian(info.median())
                .setPercentile95(info.percentile95th())
                .build();
    }

    public static WorldStatistics.Chunk getChunkProto(FlareAPI flare, Chunk chunk,
                                                      @Nullable Map<String, Integer> entityMapping) {
        WorldStatistics.Chunk.Builder builder = WorldStatistics.Chunk.newBuilder();
        builder.setX(chunk.x);
        builder.setZ(chunk.z);
        List<Entity> entities = flare.entities(chunk);
        builder.setTotalEntities(entities.size());
        Map<String, Integer> nameMapping = new Object2ObjectOpenHashMap<>();
        for (Entity entity : entities) {
            // TODO resolve item metas
            ResourceLocation key = entity instanceof EntityItem ? ((EntityItem) entity).getItem().getItem().getRegistryName() : EntityList.getKey(entity);
            String name = key == null ? entity.getName() : key.toString();
            nameMapping.compute(name, (k, v) -> v == null ? 1 : v + 1);
        }
        builder.putAllEntityCounts(nameMapping);
        if (entityMapping != null) {
            nameMapping.forEach((k, v) -> entityMapping.merge(k, v, Integer::sum));
        }
        return builder.build();
    }

    public static WorldStatistics.Region getRegionProto(FlareAPI flare, Region region,
                                                        @Nullable Map<String, Integer> entityMapping) {
        WorldStatistics.Region.Builder builder = WorldStatistics.Region.newBuilder();
        builder.setTotalEntities(region.totalEntities());
        for (Chunk chunk : region.chunks()) {
            builder.addChunks(getChunkProto(flare, chunk, entityMapping));
        }
        return builder.build();
    }

    public static SamplerMetadata.SourceMetadata getSourceMetadataProto(SourceMetadata sourceMetadata) {
        List<String> authors = sourceMetadata.authors();
        String authorsString = authors.isEmpty() ? "" : authors.size() > 1 ?
                " | authors: " + String.join(", ", authors) : " | author: " + String.join(", ", authors);
        return SamplerMetadata.SourceMetadata.newBuilder()
                .setName(sourceMetadata.name())
                .setVersion(sourceMetadata.version() + authorsString)
                .build();
    }

    public static HeapData getHeapDataProto(FlareAPI flare, ICommandSender creator, HeapDumpSummary heapDumpSummary) {
        HeapData.Builder builder = HeapData.newBuilder();
        builder.setMetadata(getHeapMetadataProto(flare, creator));
        heapDumpSummary.entries().forEach(entry -> builder.addEntries(getHeapEntryProto(entry)));
        return builder.build();
    }

    public static HeapMetadata getHeapMetadataProto(FlareAPI flare, ICommandSender creator) {
        return HeapMetadata.newBuilder()
                .setPlatformMetadata(ProtoUtil.getPlatformMetadataProto())
                .setCreator(ProtoUtil.getCommandSenderProto(creator))
                .setPlatformStatistics(getPlatformStatsProto(flare, Side.SERVER, true, null))
                .setSystemStatistics(getSystemStatsProto())
                .build();
    }

    public static HeapEntry getHeapEntryProto(Entry entry) {
        return HeapEntry.newBuilder()
                .setOrder(entry.order())
                .setInstances(entry.instances())
                .setSize(entry.bytes())
                .setType(entry.type())
                .build();
    }

    // TODO
    public static SamplerMetadata.SamplerMode getSamplerModeProto(SamplerMode samplerMode) {
        return samplerMode == SamplerMode.EXECUTION ? SamplerMetadata.SamplerMode.EXECUTION :
                SamplerMetadata.SamplerMode.ALLOCATION;
    }

    // TODO
    public static SamplerMetadata.ThreadDumper getThreadDumperProto(ThreadDumper threadDumper) {
        SamplerMetadata.ThreadDumper.Builder builder = SamplerMetadata.ThreadDumper.newBuilder();
        if (threadDumper == ThreadDumper.ALL) {
            return builder.setType(SamplerMetadata.ThreadDumper.Type.ALL).build();
        }
        if (threadDumper instanceof ThreadDumper.Specific) {
            return builder.setType(SamplerMetadata.ThreadDumper.Type.SPECIFIC)
                    .addAllIds(((ThreadDumper.Specific) threadDumper).threadIds())
                    .build();
        }
        if (threadDumper instanceof ThreadDumper.Regex) {
            return builder.setType(SamplerMetadata.ThreadDumper.Type.REGEX)
                    .addAllPatterns(((ThreadDumper.Regex) threadDumper).namePatterns().stream()
                            .map(Pattern::pattern)
                            .collect(Collectors.toList()))
                    .build();
        }
        return null;
    }

    // TODO
    public static SamplerMetadata.DataAggregator.ThreadGrouper getThreadGrouperProto(ThreadGrouper grouper) {
        if (grouper == ThreadGrouper.BY_NAME) {
            return DataAggregator.ThreadGrouper.BY_NAME;
        }
        if (grouper == ThreadGrouper.BY_POOL) {
            return DataAggregator.ThreadGrouper.BY_POOL;
        }
        return DataAggregator.ThreadGrouper.AS_ONE;
    }

    public static FlareSamplerProtos.ThreadNode getThreadNodeProto(ThreadNode threadNode, ProtoTimeEncoder timeEncoder,
                                                 MethodDescriptorResolver resolver, boolean separateParentCalls) {
        FlareSamplerProtos.ThreadNode.Builder proto = FlareSamplerProtos.ThreadNode.newBuilder().setName(threadNode.label());
        double[] times = threadNode.encodeTimesForProto(timeEncoder);
        // FlareAPI.getInstance().logger().warn("[{}] Time Windows: {}", threadNode.label(), Arrays.toString(times));
        for (double time : times) {
            proto.addTimes(time);
        }

        // When converting to a proto, we change the data structure from a recursive tree to an array.
        // Effectively, instead of:
        //
        //   {
        //     data: 'one',
        //     children: [
        //       {
        //         data: 'two',
        //         children: [{ data: 'four' }]
        //       },
        //       { data: 'three' }
        //     ]
        //   }
        //
        // We transmit:
        //
        //   [
        //     { data: 'one', children: [1, 2] },
        //     { data: 'two', children: [3] }
        //     { data: 'three', children: [] }
        //     { data: 'four', children: [] }
        //   ]
        //

        // The flattened array of nodes
        List<FlareSamplerProtos.StackTraceNode> nodesArray = new ArrayList<>();

        // Perform a depth-first post order traversal of the tree
        Deque<Node> stack = new ArrayDeque<>();

        // Push the thread node's children to the stack
        List<Integer> childrenRefs = new LinkedList<>();
        for (StackTraceNode child : threadNode.exportChildren(resolver, separateParentCalls)) {
            stack.push(new Node(child, childrenRefs));
        }

        Node node;
        while (!stack.isEmpty()) {
            node = stack.peek();

            // On the first visit, just push this node's children and leave it on the stack
            if (node.firstVisit) {
                for (StackTraceNode child : node.stackTraceNode.exportChildren(resolver, separateParentCalls)) {
                    stack.push(new Node(child, node.childrenRefs));
                }
                node.firstVisit = false;
                continue;
            }

            // Convert StackTraceNode to a proto
            // - At this stage, we have already visited this node's children
            // - The refs for each child are stored in node.childrenRefs
            FlareSamplerProtos.StackTraceNode childProto = getStackTraceNodeProto(node.stackTraceNode, resolver,
                    separateParentCalls, timeEncoder, node.childrenRefs);

            // Add the child proto to the nodes array, and record the ref in the parent
            int childIndex = nodesArray.size();
            nodesArray.add(childProto);
            node.parentChildrenRefs.add(childIndex);

            // Pop from the stack
            stack.pop();
        }

        proto.addAllChildrenRefs(childrenRefs);
        proto.addAllChildren(nodesArray);

        return proto.build();
    }

    public static FlareSamplerProtos.StackTraceNode getStackTraceNodeProto(StackTraceNode stackTraceNode,
                                                                           MethodDescriptorResolver resolver,
                                                                           boolean separateParentCalls,
                                                                           ProtoTimeEncoder timeEncoder,
                                                                           Iterable<Integer> childrenRefs) {
        FlareSamplerProtos.StackTraceNode.Builder proto = FlareSamplerProtos.StackTraceNode.newBuilder()
                .setClassName(stackTraceNode.className())
                .setMethodName(stackTraceNode.methodName());
        double[] times = stackTraceNode.encodeTimesForProto(timeEncoder);
        // FlareAPI.getInstance().logger().warn("[{}.{}()] Time Windows: {}", stackTraceNode.className(), stackTraceNode.methodName(), Arrays.toString(times));
        for (double time : times) {
            proto.addTimes(time);
        }
        if (stackTraceNode.lineNumber() >= 0) {
            proto.setLineNumber(stackTraceNode.lineNumber());
        }
        if (separateParentCalls && stackTraceNode.parentLineNumber() >= 0) {
            proto.setParentLineNumber(stackTraceNode.parentLineNumber());
        }
        if (stackTraceNode.methodDescription() != null) {
            proto.setMethodDesc(stackTraceNode.methodDescription());
        } else {
            String methodDesc = resolver.resolve(stackTraceNode);
            if (methodDesc != null) {
                proto.setMethodDesc(methodDesc);
            }
        }
        proto.addAllChildrenRefs(childrenRefs);
        return proto.build();
    }

    private static final class Node {

        private final StackTraceNode stackTraceNode;
        private boolean firstVisit = true;
        private final List<Integer> childrenRefs = new LinkedList<>();
        private final List<Integer> parentChildrenRefs;

        private Node(StackTraceNode node, List<Integer> parentChildrenRefs) {
            this.stackTraceNode = node;
            this.parentChildrenRefs = parentChildrenRefs;
        }

    }

    private ProtoUtil() { }

}
