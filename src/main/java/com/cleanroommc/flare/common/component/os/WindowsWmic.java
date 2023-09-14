package com.cleanroommc.flare.common.component.os;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility for reading from wmic (Windows Management Instrumentation Commandline) on Windows systems.
 */
public enum WindowsWmic {

    /**
     * Gets the CPU name
     */
    CPU_GET_NAME("wmic", "cpu", "get", "name", "/FORMAT:list"),

    /**
     * Gets the operating system name (caption) and version.
     */
    OS_GET_CAPTION_AND_VERSION("wmic", "os", "get", "caption,version", "/FORMAT:list");

    private static final boolean SUPPORTED = System.getProperty("os.name").startsWith("Windows");

    private final String[] cmdArgs;

    WindowsWmic(String... cmdArgs) {
        this.cmdArgs = cmdArgs;
    }

    public @Nonnull List<String> read() {
        if (SUPPORTED) {
            ProcessBuilder process = new ProcessBuilder(this.cmdArgs).redirectErrorStream(true);
            try (BufferedReader buf = new BufferedReader(new InputStreamReader(process.start().getInputStream()))) {
                List<String> lines = new ArrayList<>();
                String line;
                while ((line = buf.readLine()) != null) {
                    lines.add(line);
                }
                return lines;
            } catch (Exception ignored) { }
        }
        return Collections.emptyList();
    }

}

