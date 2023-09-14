package com.cleanroommc.flare.api.ping;

import java.util.Arrays;

public final class PingSummary {

    public static final PingSummary EMPTY = new PingSummary();

    private final int[] values;
    private final int total;
    private final int max;
    private final int min;
    private final double mean;

    private PingSummary() {
        this.values = new int[0];
        this.total = 0;
        this.max = 0;
        this.min = 0;
        this.mean = 0D;
    }

    public PingSummary(int[] values) {
        Arrays.sort(values);
        this.values = values;

        int total = 0;
        for (int value : values) {
            total += value;
        }
        this.total = total;

        this.mean = (double) total / values.length;
        this.max = values[values.length - 1];
        this.min = values[0];
    }

    public int total() {
        return this.total;
    }

    public double mean() {
        return this.mean;
    }

    public int max() {
        return this.max;
    }

    public int min() {
        return this.min;
    }

    public int percentile(double percentile) {
        if (percentile < 0 || percentile > 1) {
            throw new IllegalArgumentException("Invalid percentile " + percentile);
        }

        int rank = (int) Math.ceil(percentile * (this.values.length - 1));
        return this.values[rank];
    }

    public double median() {
        return percentile(0.50d);
    }

    public double percentile95th() {
        return percentile(0.95d);
    }

}
