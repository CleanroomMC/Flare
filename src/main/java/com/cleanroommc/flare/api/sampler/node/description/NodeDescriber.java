package com.cleanroommc.flare.api.sampler.node.description;

import javax.annotation.Nullable;

/**
 * Function to construct a {@link NodeDescription} from a stack trace element
 * of type {@code T}.
 *
 * @param <T> the stack trace element type, e.g. {@link java.lang.StackTraceElement}
 */
@FunctionalInterface
public interface NodeDescriber<T> {

    /**
     * Create a description for the given element.
     *
     * @param element the element
     * @param parent the parent element
     * @return the description
     */
    NodeDescription describe(T element, @Nullable T parent);

}
