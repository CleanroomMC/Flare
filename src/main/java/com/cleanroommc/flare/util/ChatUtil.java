package com.cleanroommc.flare.util;

import com.cleanroommc.flare.api.FlareAPI;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.function.Consumer;

public final class ChatUtil {

    public static final String RAW_PREFIX = "[⚡] ";
    public static final ITextComponent RESET = new TextComponentString("");
    public static final ITextComponent PREFIX = new TextComponentString("[");
    public static final DecimalFormat FLOAT_FORMAT = new DecimalFormat("#.##");

    private static final Map<String, LangKeys> langKeys = new Object2ObjectOpenHashMap<>();

    static {
        RESET.getStyle().setColor(TextFormatting.RESET);
        PREFIX.getStyle().setColor(TextFormatting.DARK_GRAY);
        ITextComponent symbol = new TextComponentString("⚡");
        symbol.getStyle().setColor(TextFormatting.YELLOW);
        PREFIX.appendSibling(symbol);
        ITextComponent closingBracket = new TextComponentString("] ");
        closingBracket.getStyle().setColor(TextFormatting.DARK_GRAY);
        PREFIX.appendSibling(closingBracket);
        for (LangKeys langKey : LangKeys.values()) {
            langKeys.put(langKey.langKey, langKey);
        }
    }

    public static ITextComponent prefix(ITextComponent message) {
        return PREFIX.createCopy().appendSibling(RESET.createCopy()).appendSibling(message.createCopy());
    }

    public static void sendMessage(FlareAPI flare, ICommandSender sender, ITextComponent textComponent) {
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
