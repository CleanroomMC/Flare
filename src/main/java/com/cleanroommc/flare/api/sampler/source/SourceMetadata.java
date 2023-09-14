package com.cleanroommc.flare.api.sampler.source;

import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A "source" is a plugin or mod on the platform that may be identified
 * as a source of a method call in a profile.
 */
public class SourceMetadata {

    public static <T> List<SourceMetadata> gather(Collection<T> sources, Function<? super T, String> nameFunction,
                                                  Function<? super T, String> versionFunction,
                                                  Function<? super T, List<String>> authorsFunction) {
        return gatherButExclude(sources, t -> true, nameFunction, versionFunction, authorsFunction);
    }

    public static <T> List<SourceMetadata> gatherButExclude(Collection<T> sources, Predicate<T> excludeFunction,
                                                            Function<? super T, String> nameFunction,
                                                            Function<? super T, String> versionFunction,
                                                            Function<? super T, List<String>> authorsFunction) {
        ImmutableList.Builder<SourceMetadata> builder = ImmutableList.builder();
        for (T source : sources) {
            if (!excludeFunction.test(source)) {
                String name = nameFunction.apply(source);
                String version = versionFunction.apply(source);
                List<String> authors = authorsFunction.apply(source);
                SourceMetadata metadata = new SourceMetadata(name, version, authors);
                builder.add(metadata);
            }
        }
        return builder.build();
    }

    private final String name;
    private final String version;
    private final List<String> authors;

    public SourceMetadata(String name, String version, List<String> authors) {
        this.name = name;
        this.version = version;
        this.authors = authors;
    }

    public String name() {
        return this.name;
    }

    public String version() {
        return this.version;
    }

    public List<String> authors() {
        return this.authors;
    }

}
