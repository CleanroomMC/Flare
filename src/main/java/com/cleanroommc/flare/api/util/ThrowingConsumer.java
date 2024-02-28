package com.cleanroommc.flare.api.util;

@FunctionalInterface
public interface ThrowingConsumer<T> {

    void accept(T t) throws Throwable;

}
