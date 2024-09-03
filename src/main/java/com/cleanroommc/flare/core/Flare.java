package com.cleanroommc.flare.core;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.api.FlareClientAPI;
import com.cleanroommc.flare.api.activity.ActivityLog;
import com.cleanroommc.flare.api.content.BytebinClient;
import com.cleanroommc.flare.api.metadata.MetadataProvider;
import com.cleanroommc.flare.api.ping.PingStatistics;
import com.cleanroommc.flare.api.sampler.Sampler;
import com.cleanroommc.flare.api.sampler.SamplerBuilder;
import com.cleanroommc.flare.api.sampler.SamplerContainer;
import com.cleanroommc.flare.api.sampler.node.MethodDescriptorResolver;
import com.cleanroommc.flare.api.sampler.source.ClassSourceLookup;
import com.cleanroommc.flare.api.sampler.source.SourceMetadata;
import com.cleanroommc.flare.api.sampler.thread.ThreadDumper;
import com.cleanroommc.flare.api.sampler.thread.ThreadDumper.GameThread;
import com.cleanroommc.flare.api.tick.TickRoutine;
import com.cleanroommc.flare.api.tick.TickStatistics;
import com.cleanroommc.flare.common.activity.FlareActivityLog;
import com.cleanroommc.flare.common.component.ping.FlarePingStatistics;
import com.cleanroommc.flare.common.component.tick.FlareTickRoutine;
import com.cleanroommc.flare.common.component.tick.FlareTickStatistics;
import com.cleanroommc.flare.common.content.FlareBytebinClient;
import com.cleanroommc.flare.common.sampler.FlareSamplerBuilder;
import com.cleanroommc.flare.common.sampler.source.FlareClassSourceLookup;
import com.cleanroommc.flare.common.websocket.TrustedKeyStore;
import com.cleanroommc.flare.common.websocket.client.J8BytesocksClient;
import com.cleanroommc.flare.core.mixin.MinecraftAccessor;
import com.cleanroommc.flare.core.mixin.MinecraftServerAccessor;
import com.cleanroommc.flare.util.FlareMethodDescriptorResolver;
import com.cleanroommc.flare.util.FlareThreadFactory;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.lucko.bytesocks.client.BytesocksClient;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class Flare implements FlareAPI, FlareClientAPI {

    private final Logger logger = LogManager.getLogger("Flare");
    private final Map<Class<?>, Object> objects = new Object2ObjectOpenHashMap<>();
    private final BytebinClient bytebinClient = new FlareBytebinClient();
    private final TrustedKeyStore trustedKeyStore = new TrustedKeyStore();
    private final TickStatistics tickStatistics = new FlareTickStatistics();
    private final PingStatistics pingStatistics = new FlarePingStatistics();
    private final SamplerContainer<?> sampler = new SamplerContainer<>();
    // TODO: Debate if this should be in API
    private final BytesocksClient bytesocksClient;

    private final Path saveDirectory;
    private final ActivityLog activityLog;

    private ExecutorService asyncExecutor;
    private long serverStartTime = -1L;
    private GameThread serverThread;
    @SideOnly(Side.CLIENT)
    private GameThread clientThread;

    Flare(Path saveDirectory) {
        this.saveDirectory = saveDirectory;
        this.activityLog = new FlareActivityLog(this.saveDirectory);
        // this.bytesocksClient = SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_9) ? ... :
        this.bytesocksClient = J8BytesocksClient.create("spark-usersockets.lucko.me", "spark-plugin");
        this.objects.put(BytesocksClient.class, this.bytesocksClient);
    }

    void logServerStartTime() {
        this.serverStartTime = System.currentTimeMillis();
    }

    @Override
    public <T> T get(Class<T> clazz) {
        return (T) objects.get(clazz);
    }

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public Path saveDirectory() {
        return saveDirectory;
    }

    @Override
    public ActivityLog activityLog() {
        return activityLog;
    }

    @Override
    public String viewerUrl() {
        return "https://spark.lucko.me/";
    }

    @Override
    public BytebinClient bytebinClient() {
        return bytebinClient;
    }

    @Override
    public TrustedKeyStore trustedKeyStore() {
        return trustedKeyStore;
    }

    @Override
    public ThreadDumper serverThreadDumper() {
        if (this.serverThread == null) {
            this.serverThread = new GameThread(() -> ((MinecraftServerAccessor) server()).flare$getServerThread());
        }
        return this.serverThread.get();
    }

    @Override
    public SamplerBuilder samplerBuilder() {
        return new FlareSamplerBuilder(this);
    }

    @Override
    public TickStatistics tickStats() {
        return tickStatistics;
    }

    @Override
    public PingStatistics pingStats() {
        return pingStatistics;
    }

    @Override
    public TickRoutine tickRoutine() {
        return new FlareTickRoutine();
    }

    @Override
    public MethodDescriptorResolver methodDescriptorResolver() {
        return new FlareMethodDescriptorResolver();
    }

    @Override
    public ClassSourceLookup classSourceLookup() {
        return new FlareClassSourceLookup();
    }

    @Override
    public MetadataProvider metadataProvider() {
        return Collections::emptyMap;
    }

    @Override
    public List<SourceMetadata> sourceMetadata() {
        return SourceMetadata.gatherButExclude(
                Loader.instance().getActiveModList(),
                mc -> {
                    switch (mc.getModId()) {
                        case "forge":
                        case "minecraft":
                        case "mcp":
                        case "FML":
                            return true;
                        default:
                            return false;
                    }
                },
                ModContainer::getName,
                ModContainer::getDisplayVersion,
                mc -> mc.getMetadata().authorList
        );
    }

    @Override
    public <T extends Sampler> SamplerContainer<T> samplerContainer() {
        return (SamplerContainer<T>) sampler;
    }

    @Override
    public MinecraftServer server() {
        return FMLCommonHandler.instance().getMinecraftServerInstance();
    }

    @Override
    public long serverStartTime() {
        return serverStartTime;
    }

    @Override
    public <T> Future<T> callAsync(Callable<T> runnable) {
        if (this.asyncExecutor == null) {
            this.asyncExecutor = Executors.newSingleThreadExecutor(new FlareThreadFactory(this));
        }
        return this.asyncExecutor.submit(runnable);
    }

    @Override
    public void syncWithServer(Runnable runnable) {
        MinecraftServer server = server();
        if (server == null) {
            throw new IllegalStateException("Server isn't active!");
        }
        server.addScheduledTask(runnable);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ThreadDumper clientThreadDumper() {
        if (this.clientThread == null) {
            this.clientThread = new GameThread(() -> ((MinecraftAccessor) Minecraft.getMinecraft()).flare$getThread());
        }
        return this.clientThread.get();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void syncWithClient(Runnable runnable) {
        Minecraft.getMinecraft().addScheduledTask(runnable);
    }

}
