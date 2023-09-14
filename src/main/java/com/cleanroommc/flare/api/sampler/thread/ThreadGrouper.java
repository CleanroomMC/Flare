package com.cleanroommc.flare.api.sampler.thread;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Function for grouping threads together
 */
public interface ThreadGrouper {

    /**
     * Gets the group for the given thread.
     *
     * @param threadId the id of the thread
     * @param threadName the name of the thread
     * @return the group
     */
    String group(long threadId, String threadName);

    /**
     * Gets the label to use for a given group.
     *
     * @param group the group
     * @return the label
     */
    String label(String group);

    /**
     * Implementation of {@link ThreadGrouper} that just groups by thread name.
     */
    ThreadGrouper BY_NAME = new ThreadGrouper() {
        @Override
        public String group(long threadId, String threadName) {
            return threadName;
        }

        @Override
        public String label(String group) {
            return group;
        }
    };

    /**
     * Implementation of {@link ThreadGrouper} that attempts to group by the name of the pool
     * the thread originated from.
     *
     * <p>The regex pattern used to match pools expects a digit at the end of the thread name,
     * separated from the pool name with any of one or more of ' ', '-', or '#'.</p>
     */
    ThreadGrouper BY_POOL = new ThreadGrouper() {
        private final Pattern pattern = Pattern.compile("^(.*?)[-# ]+\\d+$");

        // Thread id -> Group
        private final Map<Long, String> cache = new ConcurrentHashMap<>();
        // Group -> Thread ids
        private final Map<String, Set<Long>> seen = new ConcurrentHashMap<>();

        @Override
        public String group(long threadId, String threadName) {
            String cached = this.cache.get(threadId);
            if (cached != null) {
                return cached;
            }

            Matcher matcher = this.pattern.matcher(threadName);
            if (!matcher.matches()) {
                return threadName;
            }

            String group = matcher.group(1).trim();
            this.cache.put(threadId, group);
            this.seen.computeIfAbsent(group, g -> ConcurrentHashMap.newKeySet()).add(threadId);
            return group;
        }

        @Override
        public String label(String group) {
            int count = this.seen.getOrDefault(group, Collections.emptySet()).size();
            if (count == 0) {
                return group;
            }
            return group + " (x" + count + ")";
        }

    };

    /**
     * Implementation of {@link ThreadGrouper} which groups all threads as one, under
     * the name "All".
     */
    ThreadGrouper AS_ONE = new ThreadGrouper() {
        private final Set<Long> seen = ConcurrentHashMap.newKeySet();

        @Override
        public String group(long threadId, String threadName) {
            this.seen.add(threadId);
            return "root";
        }

        @Override
        public String label(String group) {
            return "All (x" + this.seen.size() + ")";
        }

    };

}
