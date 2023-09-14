package com.cleanroommc.flare.common.component.disk;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Exposes the system disk usage.
 */
public final class DiskUsage {

    private static final FileStore FILE_STORE;

    static {
        FileStore fileStore = null;
        try {
            fileStore = Files.getFileStore(Paths.get("."));
        } catch (IOException e) {
            // ignore
        }
        FILE_STORE = fileStore;
    }

    public static long getUsed() {
        if (FILE_STORE == null) {
            return 0;
        }

        try {
            long total = FILE_STORE.getTotalSpace();
            return total - FILE_STORE.getUsableSpace();
        } catch (IOException e) {
            return 0;
        }
    }

    public static long getTotal() {
        if (FILE_STORE == null) {
            return 0;
        }

        try {
            return FILE_STORE.getTotalSpace();
        } catch (IOException e) {
            return 0;
        }
    }

    private DiskUsage() { }

}
