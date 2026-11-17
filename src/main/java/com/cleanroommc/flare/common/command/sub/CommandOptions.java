package com.cleanroommc.flare.common.command.sub;

import com.cleanroommc.flare.util.LangKeys;
import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;

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

    public static final StringOption TYPE = new StringOption("type", "all", "render", "world", "player");
    public static final StringOption THREAD_GROUPER = new StringOption("thread-grouper", "combine-all", "not-combined");
    public static final StringOption COMMENT = new StringOption("comment", "<comment>").skipExpected();

    public static final StringListOption THREAD = new StringListOption("thread");
    public static final StringListOption REGEX = new StringListOption("regex");
    public static final StringListOption ID = new StringListOption("id");

    public static final IntegerOption TIMEOUT = new IntegerOption("timeout");
    public static final IntegerOption INTERVAL = new IntegerOption("interval");
    public static final IntegerOption ONLY_TICKS_OVER = new IntegerOption("only-ticks-over");
    public static final IntegerOption THRESHOLD = new IntegerOption("threshold");
    public static final IntegerOption THRESHOLD_TICK = new IntegerOption("threshold-tick");

    public static abstract class Option {

        public static final String argPrefix = "--";

        private static final String[] empty = new String[0];

        final String arg;
        final String[] expected;

        private Option(String arg, String... expected) {
            this.arg = argPrefix + arg;
            this.expected = expected.length > 0 ? expected : empty;
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

        private boolean skipExpected = false;

        private StringOption(String name, String... expected) {
            super(name, expected);
        }

        public StringOption skipExpected() {
            skipExpected = true;
            return this;
        }

        public String getValue(String[] arguments) throws CommandException {
            return getValue(arguments, this.expected);
        }

        public String getValue(String[] arguments, String... expected) throws CommandException {
            Preconditions.checkArgument(expected != null, "%s: expected value should not be null", this.arg);
            for (int i = 0; i < arguments.length; i++) {
                if (arguments[i].equalsIgnoreCase(this.arg)) {
                    if (i == arguments.length - 1 || arguments[i + 1].startsWith(argPrefix) || arguments[i + 1].isEmpty()) {
                        if (expected.length <= 1) {
                            throw new InvalidOptionException(LangKeys.NO_INPUTS.langKey, this.arg);
                        } else {
                            throw new InvalidOptionException(LangKeys.MALFORMED_FLAGS.langKey, this.arg, String.join(", ", expected));
                        }
                    }

                    if (skipExpected) {
                        return arguments[i + 1];
                    }

                    for (String v: expected) {
                        if (arguments[i + 1].equalsIgnoreCase(v)) {
                            return arguments[i + 1];
                        }
                    }

                    throw new InvalidOptionException(LangKeys.EXPECTED_INPUTS.langKey, this.arg, String.join(", ", expected));
                }
            }

            return null;
        }

    }

    public static final class StringListOption extends Option {

        private StringListOption(String name, String expected) {
            super(name, expected);
        }

        private StringListOption(String name) {
            this(name, "<s1,s2,...>");
        }

        public Set<String> getValue(String[] arguments) throws CommandException {
            for (int i = 0; i < arguments.length; i++) {
                if (arguments[i].equalsIgnoreCase(arg)) {
                    if (i == arguments.length - 1 || arguments[i + 1].startsWith(argPrefix) || arguments[i + 1].isEmpty()) {
                        throw new InvalidOptionException(LangKeys.NO_INPUTS.langKey, this.arg);
                    }
                    return new ObjectOpenHashSet<>(Arrays.asList(arguments[i + 1].split(",")));
                }
            }
            return Collections.emptySet();
        }

    }

    public static final class IntegerOption extends Option {

        private IntegerOption(String name) {
            super(name, "<number>");
        }

        public int getValue(String[] arguments) throws CommandException {
            for (int i = 0; i < arguments.length; i++) {
                if (arguments[i].equalsIgnoreCase(arg)) {
                    if (i == arguments.length - 1 || arguments[i + 1].startsWith(argPrefix) || arguments[i + 1].isEmpty()) {
                        throw new InvalidOptionException(LangKeys.NO_INPUTS.langKey, this.arg);
                    }
                    try {
                        return Integer.parseInt(arguments[i + 1]);
                    } catch (NumberFormatException ignored) {
                        throw new InvalidOptionException(LangKeys.MALFORMED_INPUTS.langKey, this.arg);
                    }
                }
            }
            return -1;
        }

    }

    public static List<String> buildTabCompletion(String[] arguments, Option... commandOptions) {
        if (arguments.length == 0) {
            return commandOptions.length == 0 ? Collections.emptyList() :
                    Arrays.stream(commandOptions).map(option -> option.arg).collect(Collectors.toList());
        }

        for (Option option: commandOptions) {
            if (option.isLast(arguments) && !(option instanceof StandaloneOption)) {
                return Arrays.asList(option.expected);
            }
        }

        return Arrays.stream(commandOptions)
            .filter(option -> !option.hasIn(arguments)
                    && CommandBase.doesStringStartWith(arguments[arguments.length - 1], option.arg))
            .map(option -> option.arg)
            .collect(Collectors.toList());
    }

    private static final class InvalidOptionException extends CommandException {

        public InvalidOptionException(String message, Object... objects) {
            super(message, objects);
        }

    }

}
