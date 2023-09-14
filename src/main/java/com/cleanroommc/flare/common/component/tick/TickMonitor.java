package com.cleanroommc.flare.common.component.tick;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.api.tick.TickCallback;
import com.cleanroommc.flare.api.tick.TickRoutine;
import com.cleanroommc.flare.common.component.memory.heap.gc.GarbageCollectionMonitor;
import com.cleanroommc.flare.util.ChatUtil;
import com.cleanroommc.flare.util.LangKeys;
import com.sun.management.GarbageCollectionNotificationInfo;

import net.minecraft.command.ICommandSender;

import java.text.DecimalFormat;
import java.util.DoubleSummaryStatistics;

/**
 * Monitoring process for the server/client tick rate.
 */
public abstract class TickMonitor implements TickCallback, GarbageCollectionMonitor.Listener {

    private static final DecimalFormat DF = new DecimalFormat("#.##");

    /** Flare API */
    private final FlareAPI flare;
    private final TickRoutine tickRoutine;
    /** The index of the tick when the monitor first started */
    private final int zeroTick;
    /** The active garbage collection monitor, if enabled */
    private final GarbageCollectionMonitor garbageCollectionMonitor;
    /** The predicate used to decide if a tick should be reported. */
    private final ReportPredicate reportPredicate;

    /**
     * Enum representing the various phases in a tick monitors lifetime.
     */
    private enum Phase {

        /** Tick monitor is in the setup phase where it determines the average tick rate. */
        SETUP,
        /** Tick monitor is in the monitoring phase where it listens for ticks that exceed the threshold. */
        MONITORING

    }

    /** The command sender **/
    private ICommandSender commandSender = null;
    /** The phase the monitor is in */
    private Phase phase = null;
    /** Gets the system timestamp of the last recorded tick */
    private volatile double lastTickTime = 0;
    /** Used to calculate the average tick time during the SETUP phase. */
    private final DoubleSummaryStatistics averageTickTimeCalc = new DoubleSummaryStatistics();
    /** The average tick time, defined at the end of the SETUP phase. */
    private double averageTickTime;

    public TickMonitor(FlareAPI flare, TickRoutine tickRoutine, ReportPredicate reportPredicate, boolean monitorGc) {
        this.flare = flare;
        this.tickRoutine = tickRoutine;
        this.zeroTick = tickRoutine.currentTick();
        this.reportPredicate = reportPredicate;
        if (monitorGc) {
            this.garbageCollectionMonitor =  new GarbageCollectionMonitor();
            this.garbageCollectionMonitor.addListener(this);
        } else {
            this.garbageCollectionMonitor = null;
        }
    }

    public int getCurrentTick() {
        return tickRoutine.currentTick() - this.zeroTick;
    }

    public void start(ICommandSender commandSender) {
        this.commandSender = commandSender;
        tickRoutine.addCallback(this);
    }

    public void stop() {
        tickRoutine.removeCallback(this);
        if (this.garbageCollectionMonitor != null) {
            this.garbageCollectionMonitor.close();
        }
        this.commandSender = null;
    }

    @Override
    public void onTickStart(int currentTick, double duration) {
        double now = ((double) System.nanoTime()) / 1000000d;

        // Initialize
        if (this.phase == null) {
            this.phase = Phase.SETUP;
            this.lastTickTime = now;
            ChatUtil.sendMessage(this.flare, this.commandSender, LangKeys.TICK_MONITORING_START);
            return;
        }

        // Find the difference
        double last = this.lastTickTime;
        double tickDuration = now - last;
        this.lastTickTime = now;

        if (last == 0) {
            return;
        }

        // Form averages
        if (this.phase == Phase.SETUP) {
            this.averageTickTimeCalc.accept(tickDuration);

            // Move onto the next state
            if (this.averageTickTimeCalc.getCount() >= 120) {
                // TODO: was async
                ChatUtil.sendMessage(this.flare, this.commandSender, LangKeys.TICK_MONITORING_END,
                        DF.format(this.averageTickTimeCalc.getMax()),
                        DF.format(this.averageTickTimeCalc.getMin()),
                        DF.format(this.averageTickTimeCalc.getAverage()));
                ChatUtil.sendMessage(this.flare, this.commandSender, this.reportPredicate.getMonitoringStartMessage());
                this.averageTickTime = this.averageTickTimeCalc.getAverage();
                this.phase = Phase.MONITORING;
            }
        }

        if (this.phase == Phase.MONITORING) {
            double increase = tickDuration - this.averageTickTime;
            double percentageChange = (increase * 100d) / this.averageTickTime;
            if (this.reportPredicate.shouldReport(tickDuration, increase, percentageChange)) {
                // TODO: Was async
                ChatUtil.sendMessage(this.flare, this.commandSender, LangKeys.TICK_MONITORING_REPORT, getCurrentTick(), DF.format(tickDuration), DF.format(percentageChange));
            }
        }
    }

    @Override
    public void onGc(GarbageCollectionNotificationInfo data) {
        if (this.phase == Phase.SETUP) {
            // Set lastTickTime to zero so this tick won't be counted in the average
            this.lastTickTime = 0;
            return;
        }
        ChatUtil.sendMessage(this.flare, this.commandSender, LangKeys.TICK_MONITORING_GC_REPORT,
                getCurrentTick(),
                DF.format(data.getGcInfo().getDuration()),
                GarbageCollectionMonitor.getGcType(data));
    }

}
