package com.cleanroommc.flare.core.mixin;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.api.sampler.Sampler;
import com.cleanroommc.flare.common.config.FlareConfiguration;
import com.cleanroommc.flare.common.sampler.SamplingStage;
import com.cleanroommc.flare.util.DefaultSampler;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.event.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LoadController.class, remap = false)
public class LoadControllerMixin {

    @Unique
    private static boolean flare$gameHasLoaded = false;

    @Inject(method = "propogateStateMessage", at = @At("HEAD"))
    private void injectBeforeDistributingState(FMLEvent stateEvent, CallbackInfo ci) {
        if (stateEvent instanceof FMLStateEvent) {
            if (stateEvent instanceof FMLConstructionEvent) {
                if (FlareConfiguration.isStageOn(SamplingStage.CORE_MOD)) {
                    FlareAPI.getInstance().samplerContainer().stopSampler(false, SamplingStage.CORE_MOD);
                }
                if (FlareConfiguration.isStageOn(SamplingStage.CONSTRUCTION)) {
                    Sampler sampler = DefaultSampler.build();
                    FlareAPI.getInstance().samplerContainer().setSampler(sampler, SamplingStage.CONSTRUCTION);
                    sampler.start();
                }
            } else if (stateEvent instanceof FMLPreInitializationEvent) {
                if (FlareConfiguration.isStageOn(SamplingStage.CONSTRUCTION)) {
                    FlareAPI.getInstance().samplerContainer().stopSampler(false, SamplingStage.CONSTRUCTION);
                }
                if (FlareConfiguration.isStageOn(SamplingStage.PRE_INIT)) {
                    Sampler sampler = DefaultSampler.build();
                    FlareAPI.getInstance().samplerContainer().setSampler(sampler, SamplingStage.PRE_INIT);
                    sampler.start();
                }
            } else if (stateEvent instanceof FMLInitializationEvent) {
                if (FlareConfiguration.isStageOn(SamplingStage.PRE_INIT)) {
                    FlareAPI.getInstance().samplerContainer().stopSampler(false, SamplingStage.PRE_INIT);
                }
                if (FlareConfiguration.isStageOn(SamplingStage.INIT)) {
                    Sampler sampler = DefaultSampler.build();
                    FlareAPI.getInstance().samplerContainer().setSampler(sampler, SamplingStage.INIT);
                    sampler.start();
                }
            } else if (stateEvent instanceof FMLPostInitializationEvent) {
                if (FlareConfiguration.isStageOn(SamplingStage.INIT)) {
                    FlareAPI.getInstance().samplerContainer().stopSampler(false, SamplingStage.INIT);
                }
                if (FlareConfiguration.isStageOn(SamplingStage.POST_INIT)) {
                    Sampler sampler = DefaultSampler.build();
                    FlareAPI.getInstance().samplerContainer().setSampler(sampler, SamplingStage.POST_INIT);
                    sampler.start();
                }
            } else if (stateEvent instanceof FMLLoadCompleteEvent) {
                if (FlareConfiguration.isStageOn(SamplingStage.POST_INIT)) {
                    FlareAPI.getInstance().samplerContainer().stopSampler(false, SamplingStage.POST_INIT);
                }
                if (FlareConfiguration.isStageOn(SamplingStage.AVAILABLE)) {
                    Sampler sampler = DefaultSampler.build();
                    FlareAPI.getInstance().samplerContainer().setSampler(sampler, SamplingStage.AVAILABLE);
                    sampler.start();
                }
            } else if (stateEvent instanceof FMLServerAboutToStartEvent) {
                if (FlareConfiguration.isStageOn(SamplingStage.BEFORE_START_WORLD)) {
                    Sampler sampler = DefaultSampler.build();
                    FlareAPI.getInstance().samplerContainer().setSampler(sampler, SamplingStage.BEFORE_START_WORLD);
                    sampler.start();
                } else if (FlareConfiguration.isStageOn(SamplingStage.WORLD_LOAD)) {
                    Sampler sampler = DefaultSampler.build();
                    FlareAPI.getInstance().samplerContainer().setSampler(sampler, SamplingStage.WORLD_LOAD);
                    sampler.start();
                }
            } else if (stateEvent instanceof FMLServerStartingEvent) {
                if (FlareConfiguration.isStageOn(SamplingStage.BEFORE_START_WORLD)) {
                    FlareAPI.getInstance().samplerContainer().stopSampler(false, SamplingStage.BEFORE_START_WORLD);
                }
                if (FlareConfiguration.isStageOn(SamplingStage.STARTING_WORLD)) {
                    Sampler sampler = DefaultSampler.build();
                    FlareAPI.getInstance().samplerContainer().setSampler(sampler, SamplingStage.STARTING_WORLD);
                    sampler.start();
                }
            } else if (stateEvent instanceof FMLServerStartedEvent) {
                if (FlareConfiguration.isStageOn(SamplingStage.STARTING_WORLD)) {
                    FlareAPI.getInstance().samplerContainer().stopSampler(false, SamplingStage.STARTING_WORLD);
                }
                if (FlareConfiguration.isStageOn(SamplingStage.STARTED_WORLD)) {
                    Sampler sampler = DefaultSampler.build();
                    FlareAPI.getInstance().samplerContainer().setSampler(sampler, SamplingStage.STARTED_WORLD);
                    sampler.start();
                }
            }
        } else if (stateEvent instanceof FMLModIdMappingEvent && !flare$gameHasLoaded && ((FMLModIdMappingEvent) stateEvent).isFrozen) {
            if (FlareConfiguration.isStageOn(SamplingStage.FINALIZING)) {
                Sampler sampler = DefaultSampler.build();
                FlareAPI.getInstance().samplerContainer().setSampler(sampler, SamplingStage.FINALIZING);
                sampler.start();
            }
        }
    }

    @Inject(method = "propogateStateMessage", at = @At("RETURN"))
    private void injectAfterDistributingState(FMLEvent stateEvent, CallbackInfo ci) {
        if (stateEvent instanceof FMLStateEvent) {
            if (stateEvent instanceof FMLLoadCompleteEvent) {
                if (FlareConfiguration.isStageOn(SamplingStage.AVAILABLE)) {
                    FlareAPI.getInstance().samplerContainer().stopSampler(false, SamplingStage.AVAILABLE);
                }
            } else if (stateEvent instanceof FMLServerStartedEvent) {
                if (FlareConfiguration.isStageOn(SamplingStage.WORLD_LOAD)) {
                    FlareAPI.getInstance().samplerContainer().stopSampler(false, SamplingStage.WORLD_LOAD);
                } else if (FlareConfiguration.isStageOn(SamplingStage.STARTED_WORLD)) {
                    FlareAPI.getInstance().samplerContainer().stopSampler(false, SamplingStage.STARTED_WORLD);
                }
            }
        } else if (stateEvent instanceof FMLModIdMappingEvent && !flare$gameHasLoaded && ((FMLModIdMappingEvent) stateEvent).isFrozen) {
            if (FlareConfiguration.isStageOn(SamplingStage.GAME_LOAD)) {
                FlareAPI.getInstance().samplerContainer().stopSampler(false, SamplingStage.GAME_LOAD);
            } else if (FlareConfiguration.isStageOn(SamplingStage.FINALIZING)) {
                FlareAPI.getInstance().samplerContainer().stopSampler(false, SamplingStage.FINALIZING);
                flare$gameHasLoaded = true; // Don't profile when this fires on serverStopped etc
            }
        }
    }

}
