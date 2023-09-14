package com.cleanroommc.flare.common.component.cpu;

import com.cleanroommc.flare.util.MonitoringExecutor;
import com.cleanroommc.flare.util.RollingAverage;

import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * Exposes and monitors the system/process CPU usage.
 */
public final class CpuMonitor {

    /** The object name of the com.sun.management.OperatingSystemMXBean */
    private static final String OPERATING_SYSTEM_BEAN = "java.lang:type=OperatingSystem";
    /** The OperatingSystemMXBean instance */
    private static final OperatingSystemMXBean BEAN;

    // Rolling averages for system/process data
    private static final RollingAverage SYSTEM_AVERAGE_10_SEC = new RollingAverage(10);
    private static final RollingAverage SYSTEM_AVERAGE_1_MIN = new RollingAverage(60);
    private static final RollingAverage SYSTEM_AVERAGE_15_MIN = new RollingAverage(60 * 15);
    private static final RollingAverage PROCESS_AVERAGE_10_SEC = new RollingAverage(10);
    private static final RollingAverage PROCESS_AVERAGE_1_MIN = new RollingAverage(60);
    private static final RollingAverage PROCESS_AVERAGE_15_MIN = new RollingAverage(60 * 15);

    static {
        try {
            MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName diagnosticBeanName = ObjectName.getInstance(OPERATING_SYSTEM_BEAN);
            BEAN = JMX.newMXBeanProxy(beanServer, diagnosticBeanName, OperatingSystemMXBean.class);
        } catch (Exception e) {
            throw new UnsupportedOperationException("OperatingSystemMXBean is not supported by the system.", e);
        }

        // Schedule rolling average calculations.
        MonitoringExecutor.INSTANCE.scheduleAtFixedRate(new RollingAverageCollectionTask(), 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Ensures that the static initializer has been called.
     */
    @SuppressWarnings("EmptyMethod")
    public static void ensureMonitoring() { }

    /**
     * Returns the "recent cpu usage" for the whole system. This value is a
     * double in the [0.0,1.0] interval. A value of 0.0 means that all CPUs
     * were idle during the recent period of time observed, while a value
     * of 1.0 means that all CPUs were actively running 100% of the time
     * during the recent period being observed. All values betweens 0.0 and
     * 1.0 are possible depending of the activities going on in the system.
     * If the system recent cpu usage is not available, the method returns a
     * negative value.
     *
     * @return the "recent cpu usage" for the whole system; a negative
     * value if not available.
     */
    public static double systemLoad() {
        return BEAN.getSystemCpuLoad();
    }

    public static double systemLoad10SecAvg() {
        return SYSTEM_AVERAGE_10_SEC.mean();
    }

    public static double systemLoad1MinAvg() {
        return SYSTEM_AVERAGE_1_MIN.mean();
    }

    public static double systemLoad15MinAvg() {
        return SYSTEM_AVERAGE_15_MIN.mean();
    }

    /**
     * Returns the "recent cpu usage" for the Java Virtual Machine process.
     * This value is a double in the [0.0,1.0] interval. A value of 0.0 means
     * that none of the CPUs were running threads from the JVM process during
     * the recent period of time observed, while a value of 1.0 means that all
     * CPUs were actively running threads from the JVM 100% of the time
     * during the recent period being observed. Threads from the JVM include
     * the application threads as well as the JVM internal threads. All values
     * betweens 0.0 and 1.0 are possible depending of the activities going on
     * in the JVM process and the whole system. If the Java Virtual Machine
     * recent CPU usage is not available, the method returns a negative value.
     *
     * @return the "recent cpu usage" for the Java Virtual Machine process;
     * a negative value if not available.
     */
    public static double processLoad() {
        return BEAN.getProcessCpuLoad();
    }

    public static double processLoad10SecAvg() {
        return PROCESS_AVERAGE_10_SEC.mean();
    }

    public static double processLoad1MinAvg() {
        return PROCESS_AVERAGE_1_MIN.mean();
    }

    public static double processLoad15MinAvg() {
        return PROCESS_AVERAGE_15_MIN.mean();
    }

    private CpuMonitor() { }

    /**
     * Task to poll CPU loads and add to the rolling averages in the enclosing class.
     */
    private static final class RollingAverageCollectionTask implements Runnable {
        private final RollingAverage[] systemAverages = new RollingAverage[]{
                SYSTEM_AVERAGE_10_SEC,
                SYSTEM_AVERAGE_1_MIN,
                SYSTEM_AVERAGE_15_MIN
        };
        private final RollingAverage[] processAverages = new RollingAverage[]{
                PROCESS_AVERAGE_10_SEC,
                PROCESS_AVERAGE_1_MIN,
                PROCESS_AVERAGE_15_MIN
        };

        @Override
        public void run() {
            BigDecimal systemCpuLoad = new BigDecimal(systemLoad());
            BigDecimal processCpuLoad = new BigDecimal(processLoad());

            if (systemCpuLoad.signum() != -1) { // if value is not negative
                for (RollingAverage average : this.systemAverages) {
                    average.add(systemCpuLoad);
                }
            }

            if (processCpuLoad.signum() != -1) { // if value is not negative
                for (RollingAverage average : this.processAverages) {
                    average.add(processCpuLoad);
                }
            }
        }
    }


    public interface OperatingSystemMXBean {

        double getSystemCpuLoad();

        double getProcessCpuLoad();

    }

}
