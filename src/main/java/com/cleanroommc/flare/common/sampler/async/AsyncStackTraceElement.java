package com.cleanroommc.flare.common.sampler.async;

public class AsyncStackTraceElement {

    /** The class name used for native method calls */
    public static final String NATIVE_CALL = "native";

    /** The name of the class */
    private final String className;
    /** The name of the method */
    private final String methodName;
    /** The method description */
    private final String methodDescription;

    public AsyncStackTraceElement(String className, String methodName, String methodDescription) {
        this.className = className;
        this.methodName = methodName;
        this.methodDescription = methodDescription;
    }

    public String className() {
        return this.className;
    }

    public String methodName() {
        return this.methodName;
    }

    public String methodDescription() {
        return this.methodDescription;
    }

}
