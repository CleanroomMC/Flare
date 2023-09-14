package com.cleanroommc.flare.common.component.os;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * Utility for reading from /proc/ on Linux systems.
 */
public enum LinuxProc {

    /**
     * Information about the system CPU.
     */
    CPUINFO("/proc/cpuinfo"),

    /**
     * Information about the system memory.
     */
    MEMINFO("/proc/meminfo"),

    /**
     * Information about the system network usage.
     */
    NET_DEV("/proc/net/dev"),

    /**
     * Information about the operating system distro.
     */
    OSINFO("/etc/os-release");

    private final Path path;

    LinuxProc(String path) {
        this.path = resolvePath(path);
    }

    private static @Nullable Path resolvePath(String path) {
        try {
            Path p = Paths.get(path);
            if (Files.isReadable(p)) {
                return p;
            }
        } catch (Exception ignored) { }
        return null;
    }

    public @Nonnull List<String> read() {
        if (this.path != null) {
            try {
                return Files.readAllLines(this.path, StandardCharsets.UTF_8);
            } catch (Exception ignored) { }
        }

        return Collections.emptyList();
    }

}
