package com.cleanroommc.flare.common.component.cpu;

import com.cleanroommc.flare.common.component.os.LinuxProc;
import com.cleanroommc.flare.common.component.os.WindowsReg;

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

        for (String line : WindowsReg.CPU_GET_NAME.read()) {
            String trimmed = line.trim();
            if (trimmed.startsWith("ProcessorNameString")) {
                int index = trimmed.indexOf("REG_SZ");
                if (index != -1) {
                    return trimmed.substring(index + 6).trim();
                }
            }
        }

        return "";
    }

    private CpuInfo() { }

}
