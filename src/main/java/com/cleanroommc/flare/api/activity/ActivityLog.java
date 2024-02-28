package com.cleanroommc.flare.api.activity;

import java.nio.file.Path;

// TODO: read?
public interface ActivityLog {

    Path location();

    void write(Activity activity);

}
