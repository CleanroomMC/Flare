package com.cleanroommc.flare.common.component.memory.heap.dump;

import org.objectweb.asm.Type;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Represents a "heap dump summary" from the VM.
 *
 * <p>Contains a number of entries, corresponding to types of objects in the virtual machine and their recorded impact on memory usage.</p>
 */
public final class HeapDumpSummary {

    /** The object name of the com.sun.management.DiagnosticCommandMBean */
    private static final String DIAGNOSTIC_BEAN = "com.sun.management:type=DiagnosticCommand";
    /** A regex pattern representing the expected format of the raw heap output */
    private static final Pattern OUTPUT_FORMAT = Pattern.compile("^\\s*(\\d+):\\s*(\\d+)\\s*(\\d+)\\s*([^\\s]+).*$");

    /**
     * Obtains the raw heap data output from the DiagnosticCommandMBean.
     *
     * @return the raw output
     * @throws Exception lots could go wrong!
     */
    private static String getRawHeapData() throws Exception {
        MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName diagnosticBeanName = ObjectName.getInstance(DIAGNOSTIC_BEAN);
        DiagnosticCommandMXBean proxy = JMX.newMXBeanProxy(beanServer, diagnosticBeanName, DiagnosticCommandMXBean.class);
        return proxy.gcClassHistogram(new String[0]);
    }

    /**
     * Converts type descriptors to their class name.
     *
     * @param type the type
     * @return the class name
     */
    private static String typeToClassName(String type) {
        try {
            Type typed = Type.getType(type);
            if (typed.getSort() == 11) {
                typed = Type.getType("L" + type + ";");
            }
            return typed.getClassName();
        } catch (IllegalArgumentException e) {
            return type;
        }
    }

    /**
     * Creates a new heap dump based on the current VM.
     *
     * @return the created heap dump
     * @throws RuntimeException if an error occurred whilst requesting a heap dump from the VM
     */
    public static HeapDumpSummary createNew() {
        String rawOutput;
        try {
            rawOutput = getRawHeapData();
        } catch (Exception e) {
            throw new RuntimeException("Unable to get heap dump!", e);
        }
        return new HeapDumpSummary(Arrays.stream(rawOutput.split("\n"))
                .map(line -> {
                    Matcher matcher = OUTPUT_FORMAT.matcher(line);
                    if (!matcher.matches()) {
                        return null;
                    }
                    try {
                        return new Entry(
                                Integer.parseInt(matcher.group(1)),
                                Integer.parseInt(matcher.group(2)),
                                Long.parseLong(matcher.group(3)),
                                typeToClassName(matcher.group(4))
                        );
                    } catch (Exception e) {
                        new IllegalArgumentException("Exception parsing line: " + line, e).printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    /** The entries in this heap dump */
    private final List<Entry> entries;

    private HeapDumpSummary(List<Entry> entries) {
        this.entries = entries;
    }

    public List<Entry> entries() {
        return entries;
    }

    public static final class Entry {

        private final int order;
        private final int instances;
        private final long bytes;
        private final String type;

        Entry(int order, int instances, long bytes, String type) {
            this.order = order;
            this.instances = instances;
            this.bytes = bytes;
            this.type = type;
        }

        public int order() {
            return this.order;
        }

        public int instances() {
            return this.instances;
        }

        public long bytes() {
            return this.bytes;
        }

        public String type() {
            return this.type;
        }

    }

    public interface DiagnosticCommandMXBean {

        String gcClassHistogram(String[] args);

    }

}
