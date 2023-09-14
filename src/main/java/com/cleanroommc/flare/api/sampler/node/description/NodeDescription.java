package com.cleanroommc.flare.api.sampler.node.description;

import com.cleanroommc.flare.api.sampler.node.type.SamplingStackNode;

import java.util.Objects;

public class NodeDescription {

    private final String className;
    private final String methodName;

    // async-profiler
    private final String methodDescription;

    // Java
    private final int lineNumber;
    private final int parentLineNumber;

    private final int hash;

    // Constructor used by the Java sampler
    public NodeDescription(String className, String methodName, int lineNumber, int parentLineNumber) {
        this.className = className;
        this.methodName = methodName;
        this.methodDescription = null;
        this.lineNumber = lineNumber;
        this.parentLineNumber = parentLineNumber;
        this.hash = Objects.hash(this.className, this.methodName, this.lineNumber, this.parentLineNumber);
    }

    // NodeDescription used by the async-profiler sampler
    public NodeDescription(String className, String methodName, String methodDescription) {
        this.className = className;
        this.methodName = methodName;
        this.methodDescription = methodDescription;
        this.lineNumber = SamplingStackNode.NULL_LINE_NUMBER;
        this.parentLineNumber = SamplingStackNode.NULL_LINE_NUMBER;
        this.hash = Objects.hash(this.className, this.methodName, this.methodDescription);
    }

    public String className() {
        return className;
    }

    public String methodName() {
        return methodName;
    }

    public String methodDescription() {
        return methodDescription;
    }

    public int lineNumber() {
        return lineNumber;
    }

    public int parentLineNumber() {
        return parentLineNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NodeDescription description = (NodeDescription) o;
        return this.hash == description.hash && this.lineNumber == description.lineNumber &&
                this.parentLineNumber == description.parentLineNumber &&
                this.className.equals(description.className) &&
                this.methodName.equals(description.methodName) &&
                Objects.equals(this.methodDescription, description.methodDescription);
    }

    @Override
    public int hashCode() {
        return this.hash;
    }

}
