package com.cleanroommc.flare.common.command.sub;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.*;
import java.util.stream.Collectors;

public class CommandOptions {
    public static final StandaloneOption MEMORY = new StandaloneOption("memory");
    public static final StandaloneOption NETWORK = new StandaloneOption("network");
    public static final StandaloneOption WITHOUT_GC = new StandaloneOption("without-gc");
    public static final StandaloneOption RUN_GC_FIRST = new StandaloneOption("run-gc-first");
    public static final StandaloneOption INCLUDE_NON_LIVE = new StandaloneOption("include-non-live");
    public static final StandaloneOption COMPRESS = new StandaloneOption("compress");
    public static final StandaloneOption SAVE_LOCALLY = new StandaloneOption("save-locally");
    public static final StandaloneOption ALLOC = new StandaloneOption("alloc");
    public static final StandaloneOption ALLOC_LIVE_ONLY = new StandaloneOption("alloc-live-only");
    public static final StandaloneOption IGNORE_SLEEPING = new StandaloneOption("ignore-sleeping");
    public static final StandaloneOption IGNORE_NATIVE = new StandaloneOption("ignore-native");
    public static final StandaloneOption FORCE_JAVA_SAMPLER = new StandaloneOption("force-java-sampler");
    public static final StandaloneOption SEPARATE_PARENT_CALLS = new StandaloneOption("separate-parent-calls");
    public static final StandaloneOption SAVE_TO_FILE = new StandaloneOption("save-to-file");

    public static final StringOption TYPE = new StringOption("type");
    public static final StringOption THRESHOLD = new StringOption("threshold");
    public static final StringOption THRESHOLD_TICK = new StringOption("threshold-tick");
    public static final StringOption THREAD_GROUPER = new StringOption("thread-grouper");
    public static final StringOption COMMENT = new StringOption("comment");

    public static final StringListOption THREAD = new StringListOption("thread");
    public static final StringListOption REGEX = new StringListOption("regex");
    public static final StringListOption ID = new StringListOption("id");

    public static final IntegerOption TIMEOUT = new IntegerOption("timeout");
    public static final IntegerOption INTERVAL = new IntegerOption("interval");
    public static final IntegerOption ONLY_TICKS_OVER = new IntegerOption("only-ticks-over");

    public static abstract class Option {

        public static final String argPrefix = "--";

        final String arg;
        final String expected;

        private Option(String arg) {
            this(arg, null);
        }

        private Option(String arg, String expected) {
            this.arg = argPrefix + arg;
            this.expected = "[" + expected + "]";
        }

        public boolean isLast(String[] arguments) {
            return !(arguments.length <= 1)
                    && !arguments[arguments.length - 1].startsWith(argPrefix)
                    && arg.equalsIgnoreCase(arguments[arguments.length - 2]);
        }

        public boolean hasIn(String[] arguments) {
            for (String argument : arguments) {
                if (arg.equalsIgnoreCase(argument)) {
                    return true;
                }
            }
            return false;
        }

    }

    public static final class StandaloneOption extends Option {

        private StandaloneOption(String name) {
            super(name);
        }

    }

    public static final class StringOption extends Option {

        private StringOption(String name) {
            super(name, "value");
        }

        public String getValue(String[] arguments) {
            boolean foundArg = false;
            for (String argument : arguments) {
                if (foundArg) {
                    if (argument.startsWith(argPrefix)) {
                        break;
                    }
                    return argument;
                }
                if (argument.equalsIgnoreCase(arg)) {
                    foundArg = true;
                }
            }
            // TODO: send command when flag is found but no arg values are found
            return null;
        }

    }

    public static final class StringListOption extends Option {

        private StringListOption(String name) {
            super(name, "s1,s2,...");
        }

        public Set<String> getValue(String[] arguments) {
            boolean foundArg = false;
            for (String argument : arguments) {
                if (foundArg) {
                    if (argument.startsWith(argPrefix)) {
                        break;
                    }
                    return new ObjectOpenHashSet<>(Arrays.asList(argument.split(",")));
                }
                if (argument.equalsIgnoreCase(arg)) {
                    foundArg = true;
                }
            }
            // TODO: send command when flag is found but no arg values are found
            return Collections.emptySet();
        }

    }

    public static final class IntegerOption extends Option {

        private IntegerOption(String name) {
            super(name, "number");
        }

        public int getValue(String[] arguments) {
            boolean foundArg = false;
            for (String argument : arguments) {
                if (foundArg) {
                    try {
                        return Integer.parseInt(argument);
                    } catch (NumberFormatException ignored) {
                        // TODO: send command when flag is found but arg is malformed
                        break;
                    }
                }
                if (argument.equalsIgnoreCase(arg)) {
                    foundArg = true;
                }
            }
            // TODO: send command when flag is found but no arg values are found
            return -1;
        }

    }

    public static List<String> buildTabCompletion(String[] arguments, Option... commandOptions) {
        if (arguments.length == 0) {
            return commandOptions.length == 0 ? Collections.emptyList() :
                    Arrays.stream(commandOptions).map(option -> option.arg).collect(Collectors.toList());
        }

        List<String> suggestList = null;

        for (Option option: commandOptions) {
            if (!(option instanceof StandaloneOption) && option.isLast(arguments)) {
                return Collections.singletonList(option.expected);
            } else if (!option.hasIn(arguments)) {
                if (suggestList == null) {
                    suggestList = new ArrayList<>();
                }
                suggestList.add(option.arg);
            }
        }

        return suggestList == null ? Collections.emptyList() : suggestList;
    }
}
