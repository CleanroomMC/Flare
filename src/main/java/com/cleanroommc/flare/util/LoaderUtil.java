package com.cleanroommc.flare.util;

import net.minecraftforge.common.ForgeVersion;

import java.lang.reflect.Field;

public final class LoaderUtil {

    private static final Field cleanroomBuildField;

    static {
        Field buildField = null;
        try {
            Class clazz = Class.forName("com.cleanroommc.common.CleanroomVersion");
            buildField = clazz.getField("BUILD_VERSION");
        } catch (Throwable ignore) { }
        cleanroomBuildField = buildField;
    }

    // The spark viewer picks mappings by "<platform>/<minecraft version>" and only knows "forge/1.12.2" (MCP),
    // so Cleanroom reports itself as Forge; the version still says Cleanroom.
    public static String getName() {
        return "Forge";
    }

    public static String getVersion() {
        if (cleanroomBuildField == null) {
            return ForgeVersion.getVersion();
        }
        try {
            return "Cleanroom " + cleanroomBuildField.get(null);
        } catch (Throwable ignore) { }
        return ForgeVersion.getVersion();
    }

    private LoaderUtil() { }

}
