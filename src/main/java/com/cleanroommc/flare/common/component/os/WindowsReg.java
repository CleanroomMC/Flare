package com.cleanroommc.flare.common.component.os;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility for reading from Windows Registry on Windows systems.
 * A modern and faster replacement for deprecated WMIC.
 */
public enum WindowsReg {

    /**
     * Gets the CPU name from the registry
     */
    CPU_GET_NAME("reg", "query", "HKLM\\HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0", "/v", "ProcessorNameString"),

    /**
     * Gets the operating system name (ProductName) and version (CurrentBuild).
     * Modern JVMs handle OS name/version natively via System.getProperty,
     * but this is retained if native registry readout is strictly needed.
     */
    OS_GET_CAPTION("reg", "query", "HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion", "/v", "ProductName");

    private static final boolean SUPPORTED = System.getProperty("os.name").startsWith("Windows");

    private final String[] cmdArgs;

    WindowsReg(String... cmdArgs) {
        this.cmdArgs = cmdArgs;
    }

    public @Nonnull List<String> read() {
        if (SUPPORTED) {
            ProcessBuilder process = new ProcessBuilder(this.cmdArgs).redirectErrorStream(true);
            try (BufferedReader buf = new BufferedReader(new InputStreamReader(process.start().getInputStream()))) {
                List<String> lines = new ArrayList<>();
                String line;
                while ((line = buf.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        lines.add(line);
                    }
                }
                return lines;
            } catch (Exception ignored) { }
        }
        return Collections.emptyList();
    }
}