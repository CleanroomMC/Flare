package com.cleanroommc.flare.core;

import com.cleanroommc.flare.Tags;
import com.cleanroommc.flare.api.sampler.Sampler;
import com.cleanroommc.flare.common.config.FlareConfiguration;
import com.cleanroommc.flare.common.sampler.SamplingStage;
import com.cleanroommc.flare.util.DefaultSampler;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@IFMLLoadingPlugin.Name(Tags.MOD_NAME + " Coremod")
public class FlarePlugin implements IFMLLoadingPlugin, IEarlyMixinLoader {

    static File source;

    public FlarePlugin() {
        Flare flare = new Flare(Paths.get(".").resolve("config").resolve(Tags.MOD_ID).normalize());
        Launch.blackboard.put("MainFlareInstance", flare);

        if (FlareConfiguration.isStageOn(SamplingStage.GAME_LOAD)) {
            Sampler sampler = DefaultSampler.build();
            flare.samplerContainer().setSampler(sampler, SamplingStage.GAME_LOAD);
            sampler.start();
        } else if (FlareConfiguration.isStageOn(SamplingStage.CORE_MOD)) {
            Sampler sampler = DefaultSampler.build();
            flare.samplerContainer().setSampler(sampler, SamplingStage.CORE_MOD);
            sampler.start();
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return "com.cleanroommc.flare.core.FlareMod";
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        source = (File) data.get("coremodLocation");
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.flare.json");
    }
}
