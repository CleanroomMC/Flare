package com.cleanroommc.flare.common.component.cpu;

import com.cleanroommc.flare.common.component.os.LinuxProc;
import com.cleanroommc.flare.common.component.os.WindowsWmic;

import java.util.regex.Pattern;

/**
 * Small utility to query the CPU model on Linux and Windows systems.
 */
public final class CpuInfo {

    private static final Pattern SPACE_COLON_SPACE_PATTERN = Pattern.compile("\\s+:\\s");

    /**
     * Queries the CPU model.
     *
     * @return the cpu model
     */
    public static String queryCpuModel() {
        for (String line : LinuxProc.CPUINFO.read()) {
            String[] splitLine = SPACE_COLON_SPACE_PATTERN.split(line);
            if (splitLine[0].equals("model name") || splitLine[0].equals("Processor")) {
                return splitLine[1];
            }
        }

        for (String line : WindowsWmic.CPU_GET_NAME.read()) {
            if (line.startsWith("Name")) {
                return line.substring(5).trim();
            }
        }

        return "";
    }

    private CpuInfo() { }

}
