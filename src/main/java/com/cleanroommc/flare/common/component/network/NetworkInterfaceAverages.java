package com.cleanroommc.flare.common.component.network;

import com.cleanroommc.flare.util.RollingAverage;

import java.math.BigDecimal;

public final class NetworkInterfaceAverages {

    private final RollingAverage rxBytesPerSecond;
    private final RollingAverage txBytesPerSecond;
    private final RollingAverage rxPacketsPerSecond;
    private final RollingAverage txPacketsPerSecond;

    NetworkInterfaceAverages(int windowSize) {
        this.rxBytesPerSecond = new RollingAverage(windowSize);
        this.txBytesPerSecond = new RollingAverage(windowSize);
        this.rxPacketsPerSecond = new RollingAverage(windowSize);
        this.txPacketsPerSecond = new RollingAverage(windowSize);
    }

    void accept(NetworkInterfaceInfo info, RateCalculator rateCalculator) {
        this.rxBytesPerSecond.add(rateCalculator.calculate(info.getReceivedBytes()));
        this.txBytesPerSecond.add(rateCalculator.calculate(info.getTransmittedBytes()));
        this.rxPacketsPerSecond.add(rateCalculator.calculate(info.getReceivedPackets()));
        this.txPacketsPerSecond.add(rateCalculator.calculate(info.getTransmittedPackets()));
    }

    interface RateCalculator {
        BigDecimal calculate(long value);
    }

    public RollingAverage bytesPerSecond(Direction direction) {
        switch (direction) {
            case RECEIVE:
                return rxBytesPerSecond();
            case TRANSMIT:
                return txBytesPerSecond();
            default:
                throw new AssertionError();
        }
    }

    public RollingAverage packetsPerSecond(Direction direction) {
        switch (direction) {
            case RECEIVE:
                return rxPacketsPerSecond();
            case TRANSMIT:
                return txPacketsPerSecond();
            default:
                throw new AssertionError();
        }
    }

    public RollingAverage rxBytesPerSecond() {
        return this.rxBytesPerSecond;
    }

    public RollingAverage rxPacketsPerSecond() {
        return this.rxPacketsPerSecond;
    }

    public RollingAverage txBytesPerSecond() {
        return this.txBytesPerSecond;
    }

    public RollingAverage txPacketsPerSecond() {
        return this.txPacketsPerSecond;
    }

}
