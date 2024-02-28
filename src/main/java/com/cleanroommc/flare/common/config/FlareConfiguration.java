package com.cleanroommc.flare.common.config;

import com.cleanroommc.configanytime.ConfigAnytime;
import com.cleanroommc.flare.Tags;
import com.cleanroommc.flare.common.sampler.SamplingStage;
import net.minecraftforge.common.config.Config;

@Config(modid = Tags.MOD_ID, name = Tags.CONFIG_PATH)
public class FlareConfiguration {

    public static String[] trustedKeys = new String[0];

    @Config.Comment({
            "Acceptable values are as follows:",
            "GAME_LOAD: Profiles the loading stage of the game from the earliest point until the start menu",
            "WORLD_LOAD: Profiles the loading of the world in its entirety, from creating a world to when the player spawns in",
            "CORE_MOD: Profiles loading of coremods, only the ones after Flare is initialized",
            "CONSTRUCTION: Profiles the FMLConstructionEvent",
            "PRE_INIT: Profiles the FMLPreInitializationEvent",
            "INIT: Profiles the FMLInitializationEvent",
            "POST_INIT: Profiles the FMLPostInitializationEvent",
            "AVAILABLE: Profiles the FMLLoadCompleteEvent",
            "FINALIZING: Profiles the FMLModIdMappingEvent, the last event before reaching the start menu",
            "BEFORE_START_WORLD: Profiles the FMLServerAboutToStartEvent",
            "STARTING_WORLD: Profiles the FMLServerStartingEvent",
            "STARTED_WORLD: Profiles the FMLServerStartedEvent",
    })
    public static SamplingStage[] stages = new SamplingStage[0];

    static {
        ConfigAnytime.register(FlareConfiguration.class);
    }

    public static boolean isStageOn(SamplingStage stage) {
        for (SamplingStage s : stages) {
            if (s == stage) {
                return true;
            }
        }
        return false;
    }

}
