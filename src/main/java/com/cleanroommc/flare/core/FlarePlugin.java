package com.cleanroommc.flare.core;

import com.cleanroommc.flare.Tags;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Map;

@IFMLLoadingPlugin.Name(Tags.MOD_NAME + " Coremod")
public class FlarePlugin implements IFMLLoadingPlugin {

    static File source;

    public FlarePlugin() {
        Launch.blackboard.put("MainFlareInstance", new Flare(Launch.minecraftHome.toPath().resolve("config/" + Tags.MOD_ID)));
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

}
