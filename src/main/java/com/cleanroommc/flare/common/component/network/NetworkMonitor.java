package com.cleanroommc.flare.common.component.network;

import com.cleanroommc.flare.util.MonitoringExecutor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Exposes and monitors the system/process network usage.
 */
public final class NetworkMonitor {

    // Latest readings
    private static final AtomicReference<Map<String, NetworkInterfaceInfo>> SYSTEM = new AtomicReference<>();
    
    // a pattern to match the interface names to exclude from monitoring
    // ignore: virtual eth adapters + container bridge networks
    private static final Pattern INTERFACES_TO_IGNORE = Pattern.compile("^(veth\\w+)|(br-\\w+)$");

    // Rolling averages for system/process data over 15 mins
    private static final Map<String, NetworkInterfaceAverages> SYSTEM_AVERAGES = new ConcurrentHashMap<>();
    
    // poll every minute, keep rolling averages for 15 mins
    private static final int POLL_INTERVAL = 60;
    private static final int WINDOW_SIZE_SECONDS = (int) TimeUnit.MINUTES.toSeconds(15);
    private static final int WINDOW_SIZE = WINDOW_SIZE_SECONDS / POLL_INTERVAL; // 15

    static {
        // schedule rolling average calculations.
        MonitoringExecutor.INSTANCE.scheduleAtFixedRate(new RollingAverageCollectionTask(), 1, POLL_INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * Ensures that the static initializer has been called.
     */
    @SuppressWarnings("EmptyMethod")
    public static void ensureMonitoring() {
        // intentionally empty
    }

    public static Map<String, NetworkInterfaceInfo> systemTotals() {
        Map<String, NetworkInterfaceInfo> values = SYSTEM.get();
        return values == null ? Collections.emptyMap() : values;
    }

    public static Map<String, NetworkInterfaceAverages> systemAverages() {
        return Collections.unmodifiableMap(SYSTEM_AVERAGES);
    }

    /**
     * Task to poll network activity and add to the rolling averages in the enclosing class.
     */
    private static final class RollingAverageCollectionTask implements Runnable {
        private static final BigDecimal POLL_INTERVAL_DECIMAL = BigDecimal.valueOf(POLL_INTERVAL);

        @Override
        public void run() {
            Map<String, NetworkInterfaceInfo> values = pollAndDiff(NetworkInterfaceInfo::pollSystem, SYSTEM);
            if (values != null) {
                submit(SYSTEM_AVERAGES, values);
            }
        }

        /**
         * Submits the incoming values into the rolling averages map.
         *
         * @param values the values
         */
        private static void submit(Map<String, NetworkInterfaceAverages> rollingAveragesMap, Map<String, NetworkInterfaceInfo> values) {
            // ensure all incoming keys are present in the rolling averages map
            for (String key : values.keySet()) {
                if (!INTERFACES_TO_IGNORE.matcher(key).matches()) {
                    rollingAveragesMap.computeIfAbsent(key, k -> new NetworkInterfaceAverages(WINDOW_SIZE));
                }
            }

            // submit a value (0 if unknown) to each rolling average instance in the map
            for (Map.Entry<String, NetworkInterfaceAverages> entry : rollingAveragesMap.entrySet()) {
                String interfaceName = entry.getKey();
                NetworkInterfaceAverages rollingAvgs = entry.getValue();

                NetworkInterfaceInfo info = values.getOrDefault(interfaceName, NetworkInterfaceInfo.ZERO);
                rollingAvgs.accept(info, RollingAverageCollectionTask::calculateRate);
            }
        }

        private static BigDecimal calculateRate(long value) {
            return BigDecimal.valueOf(value).divide(POLL_INTERVAL_DECIMAL, RoundingMode.HALF_UP);
        }
        
        private static Map<String, NetworkInterfaceInfo> pollAndDiff(Supplier<Map<String, NetworkInterfaceInfo>> poller, AtomicReference<Map<String, NetworkInterfaceInfo>> valueReference) {
            // poll the latest value from the supplier
            Map<String, NetworkInterfaceInfo> latest = poller.get();

            // update the value ref.
            // if the previous value was null, and the new value is empty, keep it null
            Map<String, NetworkInterfaceInfo> previous = valueReference.getAndUpdate(prev -> {
                if (prev == null && latest.isEmpty()) {
                    return null;
                } else {
                    return latest;
                }
            });

            if (previous == null) {
                return null;
            }

            return NetworkInterfaceInfo.difference(latest, previous);
        }
    }

    private NetworkMonitor() { }

}
