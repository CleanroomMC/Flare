package com.cleanroommc.flare.api.sampler.node.type;

import com.cleanroommc.flare.api.sampler.node.MergeMode;
import com.cleanroommc.flare.api.sampler.node.MethodDescriptorResolver;
import com.cleanroommc.flare.api.sampler.node.description.NodeDescriber;
import com.cleanroommc.flare.api.sampler.node.description.NodeDescription;

import java.util.*;
import java.util.function.IntPredicate;

public class ThreadNode extends SamplingStackNode {

    /**
     * The name of this thread / thread group
     */
    private final String name;

    /**
     * The label used to describe this thread in the viewer
     */
    private String label;

    public ThreadNode(String name) {
        this.name = name;
    }

    @Override
    protected SamplingStackNode resolveChild(NodeDescription description) {
        return this.children.computeIfAbsent(description, StackTraceNode::new);
    }

    public String label() {
        return this.label != null ? this.label : this.name;
    }

    public String group() {
        return this.name;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Logs the given stack trace against this node and its children.
     *
     * @param describer the function that describes the elements of the stack
     * @param stack the stack
     * @param time the total time to log
     * @param window the window
     * @param <T> the stack trace element type
     */
    public <T> void trace(NodeDescriber<T> describer, T[] stack, long time, int window) {
        if (stack.length == 0) {
            return;
        }
        timeAccumulator(window).add(time);
        SamplingStackNode node = this;
        T previous = null;
        for (int offset = 0; offset < Math.min(maxStackDepth(), stack.length); offset++) {
            T current = stack[(stack.length - 1) - offset];
            node = node.resolveChild(describer.describe(current, previous));
            node.timeAccumulator(window).add(time);
            previous = current;
        }
    }

    /**
     * Removes time windows that match the given {@code predicate}.
     *
     * @param predicate the predicate to use to test the time windows
     * @return true if this node is now empty
     */
    public boolean removeTimeWindowsRecursively(IntPredicate predicate) {
        Queue<SamplingStackNode> queue = new ArrayDeque<>();
        queue.add(this);
        while (!queue.isEmpty()) {
            SamplingStackNode node = queue.remove();
            Collection<SamplingStackNode> children = node.children();
            boolean needToProcessChildren = false;
            for (Iterator<SamplingStackNode> it = children.iterator(); it.hasNext();) {
                SamplingStackNode child = it.next();
                boolean windowsWereRemoved = child.removeTimeWindows(predicate);
                if (child.timeWindows().isEmpty()) {
                    it.remove();
                    continue;
                }
                if (windowsWereRemoved) {
                    needToProcessChildren = true;
                }
            }
            if (needToProcessChildren) {
                queue.addAll(children);
            }
        }
        removeTimeWindows(predicate);
        return timeWindows().isEmpty();
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
