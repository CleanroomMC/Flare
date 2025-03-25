package com.cleanroommc.flare.common.component.tick;

import com.cleanroommc.flare.util.ChatUtil;
import com.cleanroommc.flare.util.LangKeys;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

/**
 * A predicate to test whether a tick should be reported.
 */
public interface ReportPredicate {

    /**
     * Tests whether a tick should be reported.
     *
     * @param duration         the tick duration
     * @param increaseFromAvg  the difference between the ticks duration and the average
     * @param percentageChange the percentage change between the ticks duration and the average
     * @return true if the tick should be reported, false otherwise
     */
    boolean shouldReport(double duration, double increaseFromAvg, double percentageChange);

    /**
     * Gets a component to describe how the predicate will select ticks to report.
     *
     * @return the component
     */
    ITextComponent getMonitoringStartMessage();

    final class PercentageChangeGt implements ReportPredicate {

        private final double threshold;

        public PercentageChangeGt(double threshold) {
            this.threshold = threshold;
        }

        @Override
        public boolean shouldReport(double duration, double increaseFromAvg, double percentageChange) {
            if (increaseFromAvg <= 0) {
                return false;
            }
            return percentageChange > this.threshold;
        }

        @Override
        public ITextComponent getMonitoringStartMessage() {
            return LangKeys.MONITORING_START_PERCENTAGE_CHANGE.component(ChatUtil.FLOAT_FORMAT.format(this.threshold));
        }
    }

    final class DurationGt implements ReportPredicate {

        private final double threshold;

        public DurationGt(double threshold) {
            this.threshold = threshold;
        }

        @Override
        public boolean shouldReport(double duration, double increaseFromAvg, double percentageChange) {
            if (increaseFromAvg <= 0) {
                return false;
            }
            return duration > this.threshold;
        }

        @Override
        public ITextComponent getMonitoringStartMessage() {
            return LangKeys.MONITORING_START_GREATER_DURATION.component(ChatUtil.FLOAT_FORMAT.format(this.threshold));
        }
    }

}
