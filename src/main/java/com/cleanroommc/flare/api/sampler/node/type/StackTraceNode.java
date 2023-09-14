package com.cleanroommc.flare.api.sampler.node.type;

import com.cleanroommc.flare.api.sampler.node.MergeMode;
import com.cleanroommc.flare.api.sampler.node.MethodDescriptorResolver;
import com.cleanroommc.flare.api.sampler.node.description.NodeDescription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StackTraceNode extends SamplingStackNode {

    private final NodeDescription description;

    public StackTraceNode(NodeDescription description) {
        this.description = description;
    }

    @Override
    protected SamplingStackNode resolveChild(NodeDescription description) {
        return this.children.computeIfAbsent(description, StackTraceNode::new);
    }

    public String className() {
        return this.description.className();
    }

    public String methodName() {
        return this.description.methodName();
    }

    public String methodDescription() {
        return this.description.methodDescription();
    }

    public int lineNumber() {
        return this.description.lineNumber();
    }

    public int parentLineNumber() {
        return this.description.parentLineNumber();
    }

    public List<StackTraceNode> exportChildren(MethodDescriptorResolver resolver, boolean separateParentCalls) {
        if (this.children.isEmpty()) {
            return Collections.emptyList();
        }
        List<StackTraceNode> list = new ArrayList<>(this.children.size());
        outer: for (SamplingStackNode child : this.children.values()) {
            if (!(child instanceof StackTraceNode)) {
                continue;
            }
            StackTraceNode stackTraceChildNode = (StackTraceNode) child;
            // Attempt to find an existing node we can merge into
            for (StackTraceNode other : list) {
                if (MergeMode.shouldMerge(resolver, separateParentCalls, other, stackTraceChildNode)) {
                    other.merge(child);
                    continue outer;
                }
            }
            list.add(stackTraceChildNode);
        }
        return list;
    }

}
