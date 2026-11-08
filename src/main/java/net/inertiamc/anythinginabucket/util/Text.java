package net.inertiamc.anythinginabucket.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/** Converts {@code &} colour codes and {@code &#RRGGBB} hex to chat components, with italics off. */
public final class Text {

    private Text() {}

    public static MutableComponent of(String legacy) {
        MutableComponent root = Component.empty().withStyle(Style.EMPTY.withItalic(false));
        Style style = Style.EMPTY.withItalic(false);
        StringBuilder run = new StringBuilder();
        for (int i = 0; i < legacy.length(); i++) {
            char c = legacy.charAt(i);
            if (c == '&' && i + 1 < legacy.length() && legacy.charAt(i + 1) == '#' && hex(legacy, i + 2)) {
                flush(root, run, style);
                style = Style.EMPTY.withItalic(false).withColor(TextColor.fromRgb(Integer.parseInt(legacy.substring(i + 2, i + 8), 16)));
                i += 7;
                continue;
            }
            if (c == '&' && i + 1 < legacy.length()) {
                ChatFormatting format = ChatFormatting.getByCode(Character.toLowerCase(legacy.charAt(i + 1)));
                if (format != null) {
                    flush(root, run, style);
                    if (format == ChatFormatting.RESET) style = Style.EMPTY.withItalic(false);
                    else if (format.ordinal() <= ChatFormatting.WHITE.ordinal()) style = Style.EMPTY.withItalic(false).applyFormat(format);
                    else style = style.applyFormat(format);
                    i++;
                    continue;
                }
            }
            run.append(c);
        }
        flush(root, run, style);
        return root;
    }

    private static boolean hex(String text, int from) {
        if (from + 6 > text.length()) return false;
        for (int i = from; i < from + 6; i++) {
            if (Character.digit(text.charAt(i), 16) < 0) return false;
        }
        return true;
    }

    private static void flush(MutableComponent root, StringBuilder run, Style style) {
        if (run.isEmpty()) return;
        root.append(Component.literal(run.toString()).withStyle(style));
        run.setLength(0);
    }
}
