package com.cleanroommc.flare.common.activity;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.api.activity.Activity;
import com.cleanroommc.flare.api.activity.ActivityLog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

public class FlareActivityLog implements ActivityLog {

    private final FlareAPI flare;
    private final Path directory;

    private Path logFile;

    public FlareActivityLog(FlareAPI flare, Path configDir) {
        this.flare = flare;
        this.directory = configDir.resolve("activity");
    }

    @Override
    public Path location() {
        if (this.logFile == null) {
            this.logFile = this.directory.resolve(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_hh-mm-ss")) + ".log");
            try {
                Files.createDirectories(this.logFile.getParent());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return logFile;
    }

    @Override
    public void write(Activity activity) {
        this.flare.runAsync(() -> {
            try {
                Files.write(this.location(),
                        Collections.singletonList(String.format("[%s]: %s", activity.time().format(DateTimeFormatter.ofPattern("hh:mm:ss")), activity.description())),
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                this.flare.logger().fatal("Unable to write to activity log", e);
            }
        });
    }

}
