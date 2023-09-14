package com.cleanroommc.flare.api.sampler.node;

import com.cleanroommc.flare.api.sampler.node.type.StackTraceNode;

import java.util.Objects;

public final class MergeMode {

    /**
     * Test if two stack trace nodes should be considered the same and merged.
     *
     * @param separateParentCalls should parent calls be separated
     * @param first               the first node
     * @param second              the second node
     * @return                    if the nodes should be merged
     */
    public static boolean shouldMerge(MethodDescriptorResolver resolver, boolean separateParentCalls,
                                      StackTraceNode first, StackTraceNode second) {
        // Are the class names the same?
        if (!first.className().equals(second.className())) {
            return false;
        }
        // Are the method names the same?
        if (!first.methodName().equals(second.methodName())) {
            return false;
        }
        // Are the parent lines the same? (Did the same line of code call this method?)
        if (separateParentCalls && first.parentLineNumber() != second.parentLineNumber()) {
            return false;
        }
        String firstDesc = resolver.resolve(first);
        String secondDesc = resolver.resolve(second);
        if (firstDesc == null && secondDesc == null) {
            return true;
        }
        return Objects.equals(firstDesc, secondDesc);
    }

    private MergeMode() { }

}
