package com.cleanroommc.flare.common.sampler;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.api.metadata.MetadataProvider;
import com.cleanroommc.flare.api.sampler.Sampler;
import com.cleanroommc.flare.api.sampler.node.MethodDescriptorResolver;
import com.cleanroommc.flare.common.sampler.aggregator.DataAggregator;
import com.cleanroommc.flare.api.sampler.node.type.ThreadNode;
import com.cleanroommc.flare.api.sampler.source.ClassSourceLookup;
import com.cleanroommc.flare.api.sampler.thread.ThreadDumper;
import com.cleanroommc.flare.common.component.memory.heap.gc.GarbageCollectorStatistics;
import com.cleanroommc.flare.api.sampler.source.SourceMetadata;
import com.cleanroommc.flare.common.sampler.window.ProtoTimeEncoder;
import com.cleanroommc.flare.common.sampler.window.WindowStatisticsCollector;
import com.cleanroommc.flare.common.websocket.ViewerSocket;
import com.cleanroommc.flare.proto.FlareProtos.PlatformStatistics;
import com.cleanroommc.flare.proto.FlareProtos.SystemStatistics;
import com.cleanroommc.flare.proto.FlareSamplerProtos.SamplerData;
import com.cleanroommc.flare.proto.FlareSamplerProtos.SamplerMetadata;
import com.cleanroommc.flare.util.ProtoUtil;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Base implementation class for {@link Sampler}s.
 */
public abstract class AbstractSampler implements Sampler {

    /** The FlareAPI */
    protected final FlareAPI flare;
    /** The interval to wait between sampling, in microseconds */
    protected final int interval;
    /** The instance used to generate thread information for use in sampling */
    protected final ThreadDumper threadDumper;
    /** The unix timestamp (in millis) when this sampler should automatically complete. */
    protected final long autoEndTime; // -1 for nothing
    /** Collects statistics for each window in the sample */
    protected final WindowStatisticsCollector windowStatisticsCollector;

    /** The time when sampling first began */
    protected long startTime = -1;
    /** If the sampler is running in the background */
    protected boolean background;
    /** The garbage collector statistics when profiling started */
    protected Map<String, GarbageCollectorStatistics> initialGcStats;
    /** A set of viewer sockets linked to the sampler */
    protected List<ViewerSocket> viewerSockets = new CopyOnWriteArrayList<>();
    /** A future to encapsulate the completion of this sampler instance */
    protected CompletableFuture<Sampler> future = new CompletableFuture<>();

    protected AbstractSampler(FlareAPI flare, int interval, ThreadDumper threadDumper, long endTime, boolean runningInBackground) {
        this.flare = flare;
        this.interval = interval;
        this.threadDumper = threadDumper;
        this.autoEndTime = endTime;
        this.background = runningInBackground;
        this.windowStatisticsCollector = new WindowStatisticsCollector(flare);
    }

    protected abstract void startWork();

    protected abstract void stopWork(boolean cancelled);

    // Methods used to export the sampler data to the web viewer.
    public abstract SamplerData toProto(FlareAPI flare, ExportProps exportProps, boolean stop);

    @Override
    public long startTime() {
        if (this.startTime == -1) {
            throw new IllegalStateException("Sampler not yet started.");
        }
        return this.startTime;
    }

    @Override
    public long autoEndTime() {
        return this.autoEndTime;
    }

    @Override
    public boolean runningInBackground() {
        return this.background;
    }

    @Override
    public void start() {
        this.startTime = System.currentTimeMillis();
        startWork();
    }

    @Override
    public void stop(boolean cancelled) {
        this.windowStatisticsCollector.stop();
        for (ViewerSocket viewerSocket : this.viewerSockets) {
            viewerSocket.processSamplerStopped(this);
        }
        stopWork(cancelled);
    }

    public void attachSocket(ViewerSocket socket) {
        this.viewerSockets.add(socket);
    }

    public Collection<ViewerSocket> getAttachedSockets() {
        return this.viewerSockets;
    }

    protected void processWindowRotate() {
        this.viewerSockets.removeIf(socket -> {
            if (!socket.isOpen()) {
                return true;
            }
            socket.processWindowRotate(this);
            return false;
        });
    }

    protected void sendStatisticsToSocket() {
        try {
            this.viewerSockets.removeIf(socket -> !socket.isOpen());
            if (this.viewerSockets.isEmpty()) {
                return;
            }
            PlatformStatistics platformStats = ProtoUtil.getPlatformStatsProto(this.flare, false, getInitialGcStats());
            SystemStatistics systemStats = ProtoUtil.getSystemStatsProto();
            for (ViewerSocket viewerSocket : this.viewerSockets) {
                viewerSocket.sendUpdatedStatistics(platformStats, systemStats);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void recordInitialGcStats() {
        this.initialGcStats = GarbageCollectorStatistics.pollStats();
    }

    protected Map<String, GarbageCollectorStatistics> getInitialGcStats() {
        return this.initialGcStats;
    }

    protected void writeMetadataToProto(SamplerData.Builder proto, ExportProps props, DataAggregator dataAggregator) {
        SamplerMetadata.Builder builder = SamplerMetadata.newBuilder()
                .setSamplerMode(ProtoUtil.getSamplerModeProto(mode()))
                .setPlatformMetadata(ProtoUtil.getPlatformMetadataProto())
                .setCreator(ProtoUtil.getCommandSenderProto(props.creator()))
                .setStartTime(this.startTime)
                .setInterval(this.interval)
                .setEndTime(System.currentTimeMillis())
                .setThreadDumper(ProtoUtil.getThreadDumperProto(this.threadDumper))
                .setDataAggregator(dataAggregator.toProto());
        if (props.comment() != null) {
            builder.setComment(props.comment());
        }
        int totalTicks = this.windowStatisticsCollector.getTotalTicks();
        if (totalTicks != -1) {
            builder.setNumberOfTicks(totalTicks);
        }

        builder.setPlatformStatistics(ProtoUtil.getPlatformStatsProto(this.flare, true, getInitialGcStats()));
        builder.setSystemStatistics(ProtoUtil.getSystemStatsProto());

        MetadataProvider extraMetadataProvider = flare.metadataProvider();
        if (extraMetadataProvider != null) {
            builder.putAllExtraPlatformMetadata(extraMetadataProvider.export());
        }

        for (SourceMetadata sourceMetadata : flare.sourceMetadata()) {
            builder.putSources(sourceMetadata.name().toLowerCase(Locale.ROOT), ProtoUtil.getSourceMetadataProto(sourceMetadata));
        }

        proto.setMetadata(builder);
    }

    protected void writeDataToProto(SamplerData.Builder proto, DataAggregator dataAggregator, ExportProps exportProps) {
        try {
            List<ThreadNode> data = dataAggregator.exportData();
            data.sort(Comparator.comparing(ThreadNode::label));

            ClassSourceLookup.Visitor classSourceVisitor = ClassSourceLookup.createVisitor(exportProps.classSourceLookup().get());

            ProtoTimeEncoder timeEncoder = new ProtoTimeEncoder(mode(), data);
            int[] timeWindows = timeEncoder.getKeys();
            FlareAPI.getInstance().logger().warn("Time Windows: {}", Arrays.toString(timeWindows));
            for (int timeWindow : timeWindows) {
                proto.addTimeWindows(timeWindow);
            }

            this.windowStatisticsCollector.ensureHasStatisticsForAllWindows(timeWindows);
            proto.putAllTimeWindowStatistics(this.windowStatisticsCollector.export());

            MethodDescriptorResolver resolver = exportProps.resolver().get();

            for (ThreadNode entry : data) {
                proto.addThreads(ProtoUtil.getThreadNodeProto(entry, timeEncoder, resolver, exportProps.separateParentCalls()));
                classSourceVisitor.visit(entry);
            }

            if (classSourceVisitor.hasClassSourceMappings()) {
                proto.putAllClassSources(classSourceVisitor.getClassSourceMapping());
            }

            if (classSourceVisitor.hasMethodSourceMappings()) {
                proto.putAllMethodSources(classSourceVisitor.getMethodSourceMapping());
            }

            if (classSourceVisitor.hasLineSourceMappings()) {
                proto.putAllLineSources(classSourceVisitor.getLineSourceMapping());
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

}
