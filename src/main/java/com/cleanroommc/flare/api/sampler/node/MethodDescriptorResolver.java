package com.cleanroommc.flare.api.sampler.node;

import com.cleanroommc.flare.api.sampler.node.type.StackTraceNode;

import javax.annotation.Nullable;
import java.io.InputStream;

public abstract class MethodDescriptorResolver {

    protected static InputStream openInputStreamForClass(Class<?> resolvedClass, String className) {
        if (resolvedClass.getClassLoader() == null) {
            return ClassLoader.getSystemResourceAsStream(className.replace('.', '/') + ".class");
        } else {
            return resolvedClass.getClassLoader().getResourceAsStream(className + ".class");
        }
    }

    @Nullable
    public abstract String resolve(StackTraceNode node);

}
