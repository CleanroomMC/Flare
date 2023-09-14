package com.cleanroommc.flare.common.sampler.window;

import com.cleanroommc.flare.api.sampler.SamplerMode;
import com.cleanroommc.flare.api.sampler.node.type.ThreadNode;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.LongToDoubleFunction;
import java.util.stream.IntStream;

/**
 * Encodes a map of int->double into a double array.
 */
public class ProtoTimeEncoder {

    /** A transformer function to transform the 'time' value from a long to a double */
    private final LongToDoubleFunction valueTransformer;
    /** A sorted array of all possible keys to encode */
    private final int[] keys;
    /** A map of key value -> index in the keys array */
    private final Map<Integer, Integer> keysToIndex;

    public ProtoTimeEncoder(SamplerMode mode, List<ThreadNode> sourceData) {
        this(mode == SamplerMode.EXECUTION ? v -> (v / 1000D) : v -> v, sourceData);
    }

    public ProtoTimeEncoder(LongToDoubleFunction valueTransformer, List<ThreadNode> sourceData) {
        this.valueTransformer = valueTransformer;
        // Get an array of all keys that show up in the source data
        this.keys = sourceData.stream()
                .map(n -> n.timeWindows().stream().mapToInt(i -> i))
                .reduce(IntStream.empty(), IntStream::concat)
                .distinct()
                .sorted()
                .toArray();
        // Construct a reverse index lookup
        this.keysToIndex = new HashMap<>(this.keys.length);
        for (int i = 0; i < this.keys.length; i++) {
            this.keysToIndex.put(this.keys[i], i);
        }
    }

    /**
     * Gets an array of the keys that could be encoded by this encoder.
     *
     * @return an array of keys
     */
    public int[] getKeys() {
        return this.keys;
    }

    /**
     * Encode a map of times/durations into a double array.
     *
     * @param times a dictionary of times (unix-time millis -> duration in microseconds)
     * @return the times encoded as a double array
     */
    public double[] encode(Map<Integer, LongAdder> times) {
        // Construct an array of values - length needs to exactly match the number of keys, even if some values are zero.
        double[] array = new double[this.keys.length];
        times.forEach((key, value) -> {
            // Get the index for the given key
            Integer idx = this.keysToIndex.get(key);
            if (idx == null) {
                throw new RuntimeException("No index for key " + key + " in " + this.keysToIndex.keySet());
            }
            // Store in the array
            array[idx] = this.valueTransformer.applyAsDouble(value.longValue());
        });
        return array;
    }
}
