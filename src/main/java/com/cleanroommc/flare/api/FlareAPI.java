package com.cleanroommc.flare.api;

import com.cleanroommc.flare.api.activity.ActivityLog;
import com.cleanroommc.flare.api.content.BytebinClient;
import com.cleanroommc.flare.api.context.FlareServerContext;
import com.cleanroommc.flare.api.metadata.MetadataProvider;
import com.cleanroommc.flare.api.ping.PingStatistics;
import com.cleanroommc.flare.api.sampler.Sampler;
import com.cleanroommc.flare.api.sampler.SamplerBuilder;
import com.cleanroommc.flare.api.sampler.SamplerContainer;
import com.cleanroommc.flare.api.sampler.node.MethodDescriptorResolver;
import com.cleanroommc.flare.api.sampler.source.ClassSourceLookup;
import com.cleanroommc.flare.api.sampler.source.SourceMetadata;
import com.cleanroommc.flare.api.sampler.thread.ThreadDumper;
import com.cleanroommc.flare.api.tick.TickRoutine;
import com.cleanroommc.flare.api.tick.TickStatistics;
import com.cleanroommc.flare.common.websocket.TrustedKeyStore;
import net.minecraft.launchwrapper.Launch;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public interface FlareAPI extends FlareServerContext {

    static FlareAPI getInstance(String key) {
        Object flare = Launch.blackboard.get(key + "FlareInstance");
        if (flare == null) {
            return null;
        }
        return (FlareAPI) flare;
    }

    static FlareAPI getInstance() {
        FlareAPI flare = getInstance("Main");
        if (flare == null) {
            throw new IllegalStateException("Flare not instantiated yet!");
        }
        return flare;
    }

    static <T> T getIfPresent(String key, Function<FlareAPI, T> function) {
        FlareAPI flare = getInstance(key);
        if (flare != null) {
            return function.apply(flare);
        }
        return null;
    }

    static <T> T getIfPresent(Function<FlareAPI, T> function) {
        return getIfPresent("", function);
    }

    static void runIfPresent(String key, Consumer<FlareAPI> consumer) {
        FlareAPI flare = getInstance(key);
        if (flare != null) {
            consumer.accept(flare);
        }
    }

    static void runIfPresent(Consumer<FlareAPI> consumer) {
        runIfPresent("", consumer);
    }

    <T> T get(Class<T> clazz);

    Logger logger();

    Path saveDirectory();

    ActivityLog activityLog();

    String viewerUrl();

    BytebinClient bytebinClient();

    TrustedKeyStore trustedKeyStore();

    ThreadDumper serverThreadDumper();

    SamplerBuilder samplerBuilder();

    <T extends Sampler> SamplerContainer<T> samplerContainer();

    TickStatistics tickStats();

    PingStatistics pingStats();

    TickRoutine tickRoutine();

    MethodDescriptorResolver methodDescriptorResolver();

    ClassSourceLookup classSourceLookup();

    // TODO: registry
    MetadataProvider metadataProvider();

    List<SourceMetadata> sourceMetadata();

}
