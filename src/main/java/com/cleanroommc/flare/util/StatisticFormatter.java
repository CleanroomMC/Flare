package com.cleanroommc.flare.util;

import com.cleanroommc.flare.api.util.DoubleAverageInfo;
import com.google.common.base.Strings;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.lang.management.MemoryUsage;
import java.util.Locale;

public enum StatisticFormatter {

    ;

    // private static final String BAR_TRUE_CHARACTER = "┃";
    private static final String BAR_TRUE_CHARACTER = "\u2503";
    private static final String BAR_FALSE_CHARACTER = "\u257B";
    // private static final String BAR_FALSE_CHARACTER = "╻";

    public static ITextComponent formatTps(double tps) {
        TextFormatting colour;
        if (tps > 18.0) {
            colour = TextFormatting.GREEN;
        } else if (tps > 16.0) {
            colour = TextFormatting.YELLOW;
        } else {
            colour = TextFormatting.RED;
        }
        ITextComponent component = new TextComponentString((tps > 20.0 ? "*" : "") + Math.min(Math.round(tps * 100.0) / 100.0, 20.0));
        component.getStyle().setColor(colour);
        return component;
    }

    public static ITextComponent formatTickDurations(DoubleAverageInfo average) {
        ITextComponent text = formatTickDuration(average.min());
        ITextComponent divider = new TextComponentString(" / ");
        divider.getStyle().setColor(TextFormatting.GRAY);
        return text.appendSibling(divider)
                .appendSibling(formatTickDuration(average.median()))
                .appendSibling(divider)
                .appendSibling(formatTickDuration(average.percentile95th()))
                .appendSibling(divider)
                .appendSibling(formatTickDuration(average.max()));
    }

    public static ITextComponent formatTickDuration(double duration) {
        TextFormatting colour;
        if (duration >= 50d) {
            colour = TextFormatting.RED;
        } else if (duration >= 40d) {
            colour = TextFormatting.YELLOW;
        } else {
            colour = TextFormatting.GREEN;
        }
        ITextComponent component = new TextComponentString(String.format(Locale.ENGLISH, "%.1f", duration));
        component.getStyle().setColor(colour);
        return component;
    }

    public static ITextComponent formatCpuUsage(double usage) {
        TextFormatting colour;
        if (usage > 0.9) {
            colour = TextFormatting.RED;
        } else if (usage > 0.65) {
            colour = TextFormatting.YELLOW;
        } else {
            colour = TextFormatting.GREEN;
        }
        ITextComponent component = new TextComponentString(FormatUtil.percent(usage, 1d));
        component.getStyle().setColor(colour);
        return component;
    }

    public static ITextComponent formatPingRtts(double min, double median, double percentile95th, double max) {
        ITextComponent text = formatPingRtt(min);
        ITextComponent divider = new TextComponentString(" / ");
        divider.getStyle().setColor(TextFormatting.GRAY);
        return text.appendSibling(divider)
                .appendSibling(formatPingRtt(median))
                .appendSibling(divider)
                .appendSibling(formatPingRtt(percentile95th))
                .appendSibling(divider)
                .appendSibling(formatPingRtt(max));
    }

    public static ITextComponent formatPingRtt(double ping) {
        TextFormatting colour;
        if (ping >= 200) {
            colour = TextFormatting.RED;
        } else if (ping >= 100) {
            colour = TextFormatting.YELLOW;
        } else {
            colour = TextFormatting.GREEN;
        }
        ITextComponent component = new TextComponentString(String.valueOf((int) Math.ceil(ping)));
        component.getStyle().setColor(colour);
        return component;
    }

    public static ITextComponent generateMemoryUsageDiagram(MemoryUsage usage, int length) {
        double used = usage.getUsed();
        double committed = usage.getCommitted();
        double max = usage.getMax();

        int usedChars = (int) ((used * length) / max);
        int committedChars = (int) ((committed * length) / max);

        ITextComponent text = new TextComponentString(Strings.repeat(BAR_TRUE_CHARACTER, usedChars));
        text.getStyle().setColor(TextFormatting.YELLOW);
        if (committedChars > usedChars) {
            ITextComponent gray = new TextComponentString(" " + Strings.repeat(BAR_FALSE_CHARACTER, (committedChars - usedChars) - 1));
            gray.getStyle().setColor(TextFormatting.GRAY);
            text.appendSibling(gray);
            ITextComponent red = new TextComponentString(" " + BAR_FALSE_CHARACTER);
            red.getStyle().setColor(TextFormatting.RED);
            text.appendSibling(red);
        }
        if (length > committedChars) {
            ITextComponent gray = new TextComponentString(" " + Strings.repeat(BAR_FALSE_CHARACTER, (length - committedChars)));
            gray.getStyle().setColor(TextFormatting.GRAY);
            text.appendSibling(gray);
        }
        ITextComponent openingBracket = new TextComponentString("[ ");
        openingBracket.getStyle().setColor(TextFormatting.DARK_GRAY);
        ITextComponent closingBracket = new TextComponentString(" ]");
        closingBracket.getStyle().setColor(TextFormatting.DARK_GRAY);
        return openingBracket.appendSibling(text).appendSibling(closingBracket);
    }

    public static ITextComponent generateMemoryPoolDiagram(MemoryUsage usage, MemoryUsage collectionUsage, int length) {
        double used = usage.getUsed();
        double collectionUsed = used;
        if (collectionUsage != null) {
            collectionUsed = collectionUsage.getUsed();
        }
        double committed = usage.getCommitted();
        double max = usage.getMax();

        int usedChars = (int) ((used * length) / max);
        int collectionUsedChars = (int) ((collectionUsed * length) / max);
        int committedChars = (int) ((committed * length) / max);

        ITextComponent text = new TextComponentString(Strings.repeat(BAR_TRUE_CHARACTER, collectionUsedChars));
        text.getStyle().setColor(TextFormatting.YELLOW);

        if (usedChars > collectionUsedChars) {
            ITextComponent red = new TextComponentString(" " + BAR_TRUE_CHARACTER);
            red.getStyle().setColor(TextFormatting.RED);
            text.appendSibling(red);
            ITextComponent yellow = new TextComponentString(" " + Strings.repeat(BAR_TRUE_CHARACTER, (usedChars - collectionUsedChars) - 1));
            yellow.getStyle().setColor(TextFormatting.YELLOW);
            text.appendSibling(yellow);
        }
        if (committedChars > usedChars) {
            ITextComponent gray = new TextComponentString(" " + Strings.repeat(BAR_FALSE_CHARACTER, (committedChars - usedChars) - 1));
            gray.getStyle().setColor(TextFormatting.GRAY);
            text.appendSibling(gray);
            ITextComponent yellow = new TextComponentString(" " + BAR_FALSE_CHARACTER);
            yellow.getStyle().setColor(TextFormatting.YELLOW);
            text.appendSibling(yellow);
        }
        if (length > committedChars) {
            ITextComponent gray = new TextComponentString(" " + Strings.repeat(BAR_FALSE_CHARACTER, (length - committedChars)));
            gray.getStyle().setColor(TextFormatting.YELLOW);
            text.appendSibling(gray);
        }
        ITextComponent openingBracket = new TextComponentString("[ ");
        openingBracket.getStyle().setColor(TextFormatting.DARK_GRAY);
        ITextComponent closingBracket = new TextComponentString(" ]");
        closingBracket.getStyle().setColor(TextFormatting.DARK_GRAY);
        return openingBracket.appendSibling(text).appendSibling(closingBracket);
    }

    public static ITextComponent generateDiskUsageDiagram(double used, double max, int length) {
        int usedChars = (int) ((used * length) / max);
        int freeChars = length - usedChars;
        ITextComponent openingBracket = new TextComponentString("[ ");
        openingBracket.getStyle().setColor(TextFormatting.DARK_GRAY);
        ITextComponent usedText = new TextComponentString(Strings.repeat(BAR_TRUE_CHARACTER, usedChars));
        usedText.getStyle().setColor(TextFormatting.YELLOW);
        ITextComponent freeText = new TextComponentString(" " + Strings.repeat(BAR_FALSE_CHARACTER, freeChars));
        freeText.getStyle().setColor(TextFormatting.GRAY);
        ITextComponent closingBracket = new TextComponentString(" ]");
        closingBracket.getStyle().setColor(TextFormatting.DARK_GRAY);
        return openingBracket.appendSibling(usedText).appendSibling(freeText).appendSibling(closingBracket);
    }
}

