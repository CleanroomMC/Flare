package com.cleanroommc.flare.common.sampler.aggregator;

import com.cleanroommc.flare.api.sampler.node.type.ThreadNode;
import com.cleanroommc.flare.proto.FlareSamplerProtos.SamplerMetadata;

import java.util.List;
import java.util.function.IntPredicate;

public interface DataAggregator {

    /**
     * Forms the output data
     *
     * @return the output data
     */
    List<ThreadNode> exportData();

    /**
     * Prunes windows of data from this aggregator if the given {@code timeWindowPredicate} returns true.
     *
     * @param timeWindowPredicate the predicate
     */
    void pruneData(IntPredicate timeWindowPredicate);

    /**
     * Gets metadata about the data aggregator instance.
     */
    SamplerMetadata.DataAggregator toProto();

}
