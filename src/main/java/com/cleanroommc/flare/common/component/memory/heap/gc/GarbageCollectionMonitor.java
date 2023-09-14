package com.cleanroommc.flare.common.component.memory.heap.gc;

import com.sun.management.GarbageCollectionNotificationInfo;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;

/**
 * Monitoring process for garbage collections.
 */
public class GarbageCollectionMonitor implements NotificationListener, AutoCloseable {

    /** The registered listeners */
    private final List<Listener> listeners = new ArrayList<>();
    /** A list of the NotificationEmitters that feed information to this monitor. */
    private final List<NotificationEmitter> emitters = new ArrayList<>();

    public GarbageCollectionMonitor() {
        // Add ourselves as a notification listener for all GarbageCollectorMXBean that
        // support notifications.
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (bean instanceof NotificationEmitter) {
                NotificationEmitter notificationEmitter = (NotificationEmitter) bean;
                notificationEmitter.addNotificationListener(this, null, null);

                // Keep track of the notification emitters we subscribe to so
                // the listeners can be removed on #close
                this.emitters.add(notificationEmitter);
            }
        }
    }

    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void handleNotification(Notification notification, Object handback) {
        // We're only interested in GC notifications
        if (!notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
            return;
        }

        GarbageCollectionNotificationInfo data = GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData());
        for (Listener listener : this.listeners) {
            listener.onGc(data);
        }
    }

    @Override
    public void close() {
        for (NotificationEmitter e : this.emitters) {
            try {
                e.removeNotificationListener(this);
            } catch (ListenerNotFoundException ex) {
                ex.printStackTrace();
            }
        }
        this.emitters.clear();
        this.listeners.clear();
    }

    /**
     * A simple listener object for garbage collections.
     */
    public interface Listener {

        void onGc(GarbageCollectionNotificationInfo data);

    }

    /**
     * Gets a human-friendly description for the type of the given GC notification.
     *
     * @param info the notification object
     * @return the name of the GC type
     */
    public static String getGcType(GarbageCollectionNotificationInfo info) {
        if ("end of minor GC".equals(info.getGcAction())) {
            return "Young Gen";
        } else if ("end of major GC".equals(info.getGcAction())) {
            return "Old Gen";
        } else {
            return info.getGcAction();
        }
    }

}
