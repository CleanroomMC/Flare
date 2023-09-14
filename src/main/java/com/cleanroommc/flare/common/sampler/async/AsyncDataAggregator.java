package com.cleanroommc.flare.common.sampler.async;

import com.cleanroommc.flare.common.sampler.aggregator.AbstractDataAggregator;
import com.cleanroommc.flare.api.sampler.node.description.NodeDescriber;
import com.cleanroommc.flare.api.sampler.node.description.NodeDescription;
import com.cleanroommc.flare.api.sampler.node.type.ThreadNode;
import com.cleanroommc.flare.api.sampler.thread.ThreadGrouper;
import com.cleanroommc.flare.proto.FlareSamplerProtos.SamplerMetadata;
import com.cleanroommc.flare.proto.FlareSamplerProtos.SamplerMetadata.DataAggregator;
import com.cleanroommc.flare.util.ProtoUtil;

public class AsyncDataAggregator extends AbstractDataAggregator {

    /** A describer for async-profiler stack trace elements. */
    private static final NodeDescriber<AsyncStackTraceElement> DESCRIBER = (element, parent) ->
            new NodeDescription(element.className(), element.methodName(), element.methodDescription());

    protected AsyncDataAggregator(ThreadGrouper threadGrouper) {
        super(threadGrouper);
    }

    public void insertData(ProfileSegment element, int window) {
        try {
            ThreadNode node = getNode(this.threadGrouper.group(element.getNativeThreadId(), element.getThreadName()));
            node.trace(DESCRIBER, element.getStackTrace(), element.getValue(), window);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public SamplerMetadata.DataAggregator toProto() {
        return SamplerMetadata.DataAggregator.newBuilder()
                .setType(SamplerMetadata.DataAggregator.Type.SIMPLE)
                .setThreadGrouper(ProtoUtil.getThreadGrouperProto(this.threadGrouper))
                .build();
    }

}
