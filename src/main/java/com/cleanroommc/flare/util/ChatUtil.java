package com.cleanroommc.flare.util;

import com.cleanroommc.flare.api.FlareAPI;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import java.text.DecimalFormat;
import java.util.function.Consumer;

public final class ChatUtil {

    public static final String RAW_PREFIX = TextFormatting.GRAY + "[" + TextFormatting.GOLD + "✳" + TextFormatting.GRAY + "]" + TextFormatting.RESET + " ";
    public static final DecimalFormat FLOAT_FORMAT = new DecimalFormat("#.##");

    public static ITextComponent prefix(ITextComponent message) {
        return new TextComponentString(RAW_PREFIX).appendSibling(message);
    }

    public static void sendMessage(FlareAPI flare, ICommandSender sender, ITextComponent textComponent) {
        textComponent = ChatUtil.prefix(textComponent);
        if (sender == null) {
            flare.logger().warn(textComponent.getUnformattedText());
        } else {
            sender.sendMessage(textComponent);
        }
    }

    public static void sendMessage(FlareAPI flare, ICommandSender sender, LangKeys langKey, Object... formatArgs) {
        sendMessage(flare, sender, new TextComponentTranslation(langKey.langKey, formatArgs));
    }

    public static void sendMessage(FlareAPI flare, ICommandSender sender, LangKeys langKey, Consumer<TextComponentTranslation> unaryOperator, Object... formatArgs) {
        TextComponentTranslation text = new TextComponentTranslation(langKey.langKey, formatArgs);
        unaryOperator.accept(text);
        sendMessage(flare, sender, text);
    }
    
    private ChatUtil() { }
    
}
