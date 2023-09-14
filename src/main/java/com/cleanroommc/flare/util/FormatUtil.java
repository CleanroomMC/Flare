package com.cleanroommc.flare.util;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.Locale;

public final class FormatUtil {

    private static final String[] SIZE_UNITS = {"bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};

    public static String percent(double value, double max) {
        double percent = (value * 100d) / max;
        return (int) percent + "%";
    }

    public static String formatBytes(long bytes) {
        if (bytes <= 0) {
            return "0 bytes";
        }
        int sizeIndex = (int) (Math.log(bytes) / Math.log(1024));
        return String.format(Locale.ENGLISH, "%.1f", bytes / Math.pow(1024, sizeIndex)) + " " + SIZE_UNITS[sizeIndex];
    }

    public static ITextComponent formatBytes(long bytes, TextFormatting colour, String suffix) {
        String value;
        String unit;
        if (bytes <= 0) {
            value = "0";
            unit = "KB" + suffix;
        } else {
            int sizeIndex = (int) (Math.log(bytes) / Math.log(1024));
            value = String.format(Locale.ENGLISH, "%.1f", bytes / Math.pow(1024, sizeIndex));
            unit = SIZE_UNITS[sizeIndex] + suffix;
        }
        ITextComponent valueText = new TextComponentString(value + " ");
        valueText.getStyle().setColor(colour);
        return valueText.appendSibling(new TextComponentString(unit));
    }

    public static String formatSeconds(long seconds) {
        if (seconds <= 0) {
            return "0s";
        }
        long second = seconds;
        long minute = second / 60;
        second = second % 60;
        StringBuilder sb = new StringBuilder();
        if (minute != 0) {
            sb.append(minute).append("m ");
        }
        if (second != 0) {
            sb.append(second).append("s ");
        }
        return sb.toString().trim();
    }

    private FormatUtil() { }

}
