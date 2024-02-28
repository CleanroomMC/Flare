package com.cleanroommc.flare.common.tracker;

import com.cleanroommc.flare.core.mixin.TimeTrackerAccessor;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.server.timings.TimeTracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Trackable<T> {

    private static final List<Trackable<?>> ALL = new ArrayList<>();

    public static final Trackable<Entity> ENTITY = new Trackable<>("entity", TimeTracker.ENTITY_UPDATE);
    public static final Trackable<TileEntity> TILE_ENTITY = new Trackable<>("tile_entity", TimeTracker.TILE_ENTITY_UPDATE);

    public static List<Trackable<?>> all() {
        return Collections.unmodifiableList(ALL);
    }

    private final String langKey;
    private final TimeTracker<T> timeTracker;

    public Trackable(String type, TimeTracker<T> timeTracker) {
        this.langKey = "flare.misc.tracker." + type;
        this.timeTracker = timeTracker;
        ALL.add(this);
    }

    @SideOnly(Side.CLIENT)
    public String translate() {
        return I18n.format(this.langKey);
    }

    public boolean isEnabled() {
        return ((TimeTrackerAccessor) this.timeTracker).isEnabled();
    }

    public TimeTracker<T> timeTracker() {
        return timeTracker;
    }

}
