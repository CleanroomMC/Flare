package com.cleanroommc.flare.common.sampler.source;

import com.cleanroommc.flare.api.sampler.source.ClassSourceLookup;
import net.minecraftforge.fml.common.ModAPIManager;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.discovery.ModCandidate;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

// TODO: can be greatly expanded on to provide clarity for the user
// TODO: mixin support
public class FlareClassSourceLookup implements ClassSourceLookup {

    private final ASMDataTable asmDataTable;

    public FlareClassSourceLookup() {
        try {
            Field modApiManager$dataTable = ModAPIManager.class.getDeclaredField("dataTable");
            modApiManager$dataTable.setAccessible(true);
            this.asmDataTable = (ASMDataTable) modApiManager$dataTable.get(ModAPIManager.INSTANCE);
        } catch (Throwable t) {
            throw new RuntimeException(t); // TODO: don't throw uncaught
        }
    }

    @Nullable
    @Override
    public String identify(String className) {
        String packageName = className.substring(0, className.lastIndexOf('.'));
        // if (packageName.startsWith("sun.") || packageName.startsWith("java.")) {
            // return "Java";
        // }
        if (packageName.startsWith("net.minecraft")) {
            if (packageName.charAt(13) == '.') {
                // if ("launchwrapper".equals(packageName.substring(14, 27))) {
                    // return "LaunchWrapper";
                // }
                return "Minecraft";
            }
            return "Forge";
        }
        Set<ModCandidate> candidates = asmDataTable.getCandidatesFor(packageName);
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.stream()
                .map(ModCandidate::getContainedMods)
                .flatMap(Collection::stream)
                .map(ModContainer::getName)
                .collect(Collectors.joining(", "));
    }

}
