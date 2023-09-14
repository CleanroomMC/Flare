package com.cleanroommc.flare.common.component.memory.heap.dump;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.nio.file.Path;

public class HeapDump {

    /** The object name of the com.sun.management.HotSpotDiagnosticMXBean */
    private static final String DIAGNOSTIC_BEAN = "com.sun.management:type=HotSpotDiagnostic";

    /**
     * Creates a heap dump at the given output path.
     *
     * @param outputPath the path to write the snapshot to
     * @param live if true dump only live objects i.e. objects that are reachable from others
     * @throws Exception catch all
     */
    public static void dumpHeap(Path outputPath, boolean live) throws Exception {
        String outputPathString = outputPath.toAbsolutePath().normalize().toString();
        if (isOpenJ9()) {
            dumpOpenJ9(outputPathString);
        } else {
            dumpHotspot(outputPathString, live);
        }
    }

    private static void dumpOpenJ9(String outputPathString) throws Exception {
        Class<?> dumpClass = Class.forName("com.ibm.jvm.Dump");
        Method heapDumpMethod = dumpClass.getMethod("heapDumpToFile", String.class);
        heapDumpMethod.invoke(null, outputPathString);
    }

    private static void dumpHotspot(String outputPathString, boolean live) throws Exception {
        MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName diagnosticBeanName = ObjectName.getInstance(DIAGNOSTIC_BEAN);
        HotSpotDiagnosticMXBean proxy = JMX.newMXBeanProxy(beanServer, diagnosticBeanName, HotSpotDiagnosticMXBean.class);
        proxy.dumpHeap(outputPathString, live);
    }

    public static boolean isOpenJ9() {
        try {
            Class.forName("com.ibm.jvm.Dump");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public interface HotSpotDiagnosticMXBean {

        void dumpHeap(String outputFile, boolean live) throws IOException;

    }

}
