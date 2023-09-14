package com.cleanroommc.flare.api.tick;

import com.cleanroommc.flare.api.util.DoubleAverageInfo;

public interface TickStatistics {

    boolean isDurationSupported();

    double tps5Sec();

    double tps10Sec();

    double tps1Min();

    double tps5Min();

    double tps15Min();

    DoubleAverageInfo duration10Sec();

    DoubleAverageInfo duration1Min();

    DoubleAverageInfo duration5Min();

}
