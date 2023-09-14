package com.cleanroommc.flare.common.command.sub;

import com.cleanroommc.flare.api.FlareAPI;
import com.cleanroommc.flare.util.ChatUtil;
import com.cleanroommc.flare.util.LangKeys;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

public abstract class FlareSubCommand extends CommandBase {

    private static final String argPrefix = "--";

    public final FlareAPI flare;

    public FlareSubCommand(FlareAPI flare) {
        this.flare = flare;
    }

    protected boolean hasNoArgs(String[] arguments) {
        return arguments.length == 0;
    }

    protected boolean hasArg(String[] arguments, String arg) {
        arg = argPrefix + arg;
        for (String argument : arguments) {
            if (argument.equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return false;
    }

    protected String getArgValue(String[] arguments, String arg) {
        arg = argPrefix + arg;
        boolean foundArg = false;
        for (String argument : arguments) {
            if (foundArg) {
                return argument;
            }
            if (argument.equalsIgnoreCase(arg)) {
                foundArg = true;
            }
        }
        // TODO: send command when flag is found but no arg values are found
        return null;
    }

    protected Set<String> getArgValues(String[] arguments, String arg) {
        arg = argPrefix + arg;
        boolean foundArg = false;
        for (String argument : arguments) {
            if (foundArg) {
                return new ObjectOpenHashSet<>(Arrays.asList(argument.split(",")));
            }
            if (argument.equalsIgnoreCase(arg)) {
                foundArg = true;
            }
        }
        // TODO: send command when flag is found but no arg values are found
        return Collections.emptySet();
    }

    protected int getIntArgValue(String[] arguments, String arg) {
        arg = argPrefix + arg;
        boolean foundArg = false;
        for (String argument : arguments) {
            if (foundArg) {
                try {
                    return Integer.parseInt(argument);
                } catch (NumberFormatException ignored) {
                    // TODO: send command when flag is found but arg is malformed
                }
            }
            if (argument.equalsIgnoreCase(arg)) {
                foundArg = true;
            }
        }
        // TODO: send command when flag is found but no arg values are found
        return -1;
    }

    protected double getDoubleArgValue(String[] arguments, String arg) {
        arg = argPrefix + arg;
        boolean foundArg = false;
        for (String argument : arguments) {
            if (foundArg) {
                try {
                    return Double.parseDouble(argument);
                } catch (NumberFormatException ignored) {
                    // TODO: send command when flag is found but arg is malformed
                }
            }
            if (argument.equalsIgnoreCase(arg)) {
                foundArg = true;
            }
        }
        // TODO: send command when flag is found but no arg values are found
        return -1;
    }

    protected void sendMessage(ICommandSender sender, ITextComponent textComponent) {
        this.flare.syncWithServer(() -> ChatUtil.sendMessage(this.flare, sender, textComponent));
    }

    protected void sendMessage(ICommandSender sender, LangKeys langKey, Object... formatArgs) {
        this.flare.syncWithServer(() -> ChatUtil.sendMessage(this.flare, sender, langKey, formatArgs));
    }

    protected void sendMessage(ICommandSender sender, LangKeys langKey, Consumer<TextComponentTranslation> consumer, Object... formatArgs) {
        this.flare.syncWithServer(() -> ChatUtil.sendMessage(this.flare, sender, langKey, consumer, formatArgs));
    }

    @FunctionalInterface
    public interface CommandSender {

        void accept(LangKeys langKey, Consumer<TextComponentTranslation> consumer, Object... formatArgs);

        default void accept(LangKeys langKey, Object... formatArgs) {
            accept(langKey, c -> { }, formatArgs);
        }

    }

}
