package com.cleanroommc.flare.common.sampler.async;

import com.cleanroommc.flare.common.sampler.async.jfr.JfrReader;

import java.nio.charset.StandardCharsets;

/**
 * Represents a profile "segment".
 *
 * <p>async-profiler groups unique stack traces together per-thread in its output.</p>
 */
public class ProfileSegment {

    /** The native thread id (does not correspond to Thread#getId) */
    private final int nativeThreadId;
    /** The name of the thread */
    private final String threadName;
    /** The stack trace for this segment */
    private final AsyncStackTraceElement[] stackTrace;
    /** The time spent executing this segment in microseconds */
    private final long value;

    public ProfileSegment(int nativeThreadId, String threadName, AsyncStackTraceElement[] stackTrace, long value) {
        this.nativeThreadId = nativeThreadId;
        this.threadName = threadName;
        this.stackTrace = stackTrace;
        this.value = value;
    }

    public int getNativeThreadId() {
        return this.nativeThreadId;
    }

    public String getThreadName() {
        return this.threadName;
    }

    public AsyncStackTraceElement[] getStackTrace() {
        return this.stackTrace;
    }

    public long getValue() {
        return this.value;
    }

    public static ProfileSegment parseSegment(JfrReader reader, JfrReader.Event sample, String threadName, long value) {
        JfrReader.StackTrace stackTrace = reader.stackTraces.get(sample.stackTraceId);
        int len = stackTrace != null ? stackTrace.methods.length : 0;

        AsyncStackTraceElement[] stack = new AsyncStackTraceElement[len];
        for (int i = 0; i < len; i++) {
            stack[i] = parseStackFrame(reader, stackTrace.methods[i]);
        }

        return new ProfileSegment(sample.tid, threadName, stack, value);
    }

    private static AsyncStackTraceElement parseStackFrame(JfrReader reader, long methodId) {
        AsyncStackTraceElement result = reader.stackFrames.get(methodId);
        if (result != null) {
            return result;
        }

        JfrReader.MethodRef methodRef = reader.methods.get(methodId);
        JfrReader.ClassRef classRef = reader.classes.get(methodRef.cls);

        byte[] className = reader.symbols.get(classRef.name);
        byte[] methodName = reader.symbols.get(methodRef.name);

        if (className == null || className.length == 0) {
            // Native call
            result = new AsyncStackTraceElement(
                    AsyncStackTraceElement.NATIVE_CALL,
                    new String(methodName, StandardCharsets.UTF_8),
                    null
            );
        } else {
            // Java method
            byte[] methodDesc = reader.symbols.get(methodRef.sig);
            result = new AsyncStackTraceElement(
                    new String(className, StandardCharsets.UTF_8).replace('/', '.'),
                    new String(methodName, StandardCharsets.UTF_8),
                    new String(methodDesc, StandardCharsets.UTF_8)
            );
        }

        reader.stackFrames.put(methodId, result);
        return result;
    }

}
