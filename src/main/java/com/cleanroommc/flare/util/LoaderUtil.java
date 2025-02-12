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

    public static String getName() {
        return cleanroomBuildField != null ? "Cleanroom" : "Forge";
    }

    public static String getVersion() {
        if (cleanroomBuildField == null) {
            return ForgeVersion.getVersion();
        }
        try {
            return (String) cleanroomBuildField.get(null);
        } catch (Throwable ignore) { }
        return ForgeVersion.getVersion();
    }

    private LoaderUtil() { }

}
