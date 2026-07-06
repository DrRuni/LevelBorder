package runi.myddns.levelborder.Utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class ColorUtil {

    public static Component borderColor(String text) {

        int startR = 0x7F, startG = 0xD8, startB = 0xFF;
        int endR = 0x1E, endG = 0x3A, endB = 0x5A;

        Component result = Component.empty();
        int len = Math.max(1, text.length() - 1);

        for (int i = 0; i < text.length(); i++) {
            float t = i / (float) len;

            int r = (int) (startR + (endR - startR) * t);
            int g = (int) (startG + (endG - startG) * t);
            int b = (int) (startB + (endB - startB) * t);

            result = result.append(
                    Component.text(
                            String.valueOf(text.charAt(i)),
                            TextColor.color(r, g, b)
                    )
            );
        }

        return result;
    }

    public static Component borderColorScrolling(String text, float tick) {

        int startR = 0x7F, startG = 0xD8, startB = 0xFF;
        int endR = 0x1E, endG = 0x3A, endB = 0x5A;

        Component result = Component.empty();
        int length = Math.max(1, text.length() - 1);

        for (int i = 0; i < text.length(); i++) {

            float offset = i / (float) length;
            float wave = offset * 1.8f + tick * 0.04f;

            float t = (float) (Math.sin(wave * Math.PI * 2) * 0.5f + 0.5f);

            int r = (int) (startR + (endR - startR) * t);
            int g = (int) (startG + (endG - startG) * t);
            int b = (int) (startB + (endB - startB) * t);

            result = result.append(
                    Component.text(
                            String.valueOf(text.charAt(i)),
                            TextColor.color(r, g, b)
                    )
            );
        }

        return result;
    }
}