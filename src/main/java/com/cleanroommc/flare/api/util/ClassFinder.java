package com.cleanroommc.flare.api.util;

import javax.annotation.Nullable;

public final class ClassFinder {

    @Nullable
    public static Class<?> findClass(String className) {
        try {
            return Class.forName(className);
        } catch (Throwable ignored) { }
        return null;
    }

    private ClassFinder() { }

}
