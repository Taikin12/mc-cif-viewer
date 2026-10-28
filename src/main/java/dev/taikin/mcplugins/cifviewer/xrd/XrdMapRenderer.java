package dev.taikin.mcplugins.cifviewer.xrd;

import java.awt.Color;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MinecraftFont;

public final class XrdMapRenderer extends MapRenderer {

    private static final int LEFT = 10;
    private static final int RIGHT = 126;
    private static final int TOP = 14;
    private static final int BOTTOM = 112;
    private static final Color BACKGROUND = new Color(18, 18, 26);
    private static final Color PLOT = new Color(8, 8, 14);
    private static final Color GRID = new Color(45, 45, 60);
    private static final Color AXIS = new Color(210, 210, 210);
    private static final Color FILL = new Color(0, 90, 110);
    private static final Color LINE = new Color(0, 230, 255);
    private static final Color STRIP = new Color(220, 220, 220);

    private final XrdPattern pattern;
    private final String title;

    public XrdMapRenderer(XrdPattern pattern) {
        super(false);
        this.pattern = pattern;
        String name = ascii(pattern.name());
        this.title = "XRD " + (name.length() > 16 ? name.substring(0, 16) : name);
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                canvas.setPixelColor(x, y, BACKGROUND);
            }
        }
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 128; x++) {
                canvas.setPixelColor(x, y, STRIP);
            }
        }
        for (int y = 116; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                canvas.setPixelColor(x, y, STRIP);
            }
        }
        for (int y = TOP; y <= BOTTOM; y++) {
            for (int x = LEFT; x <= RIGHT; x++) {
                canvas.setPixelColor(x, y, PLOT);
            }
        }

        for (double tick = Math.ceil(pattern.twoThetaMin() / 10.0) * 10.0;
                tick <= pattern.twoThetaMax(); tick += 10.0) {
            int x = x(tick);
            for (int y = TOP; y <= BOTTOM; y++) {
                canvas.setPixelColor(x, y, GRID);
            }
        }
        for (int step = 1; step <= 3; step++) {
            int y = BOTTOM - (BOTTOM - TOP) * step / 4;
            for (int x = LEFT; x <= RIGHT; x++) {
                canvas.setPixelColor(x, y, GRID);
            }
        }

        for (int x = LEFT; x <= RIGHT; x++) {
            double value = sample(x);
            int height = (int) Math.round(value * (BOTTOM - TOP));
            int peak = BOTTOM - height;
            for (int y = peak; y <= BOTTOM; y++) {
                canvas.setPixelColor(x, y, FILL);
            }
            canvas.setPixelColor(x, peak, LINE);
        }
        for (int y = TOP; y <= BOTTOM; y++) {
            canvas.setPixelColor(LEFT, y, AXIS);
        }
        for (int x = LEFT; x <= RIGHT; x++) {
            canvas.setPixelColor(x, BOTTOM, AXIS);
        }

        canvas.drawText(2, 2, MinecraftFont.Font, title);
        canvas.drawText(LEFT, 118, MinecraftFont.Font, number(pattern.twoThetaMin()));
        String max = number(pattern.twoThetaMax());
        canvas.drawText(RIGHT - max.length() * 6 + 1, 118, MinecraftFont.Font, max);
        canvas.drawText(50, 118, MinecraftFont.Font, "2theta");
    }

    private double sample(int x) {
        double value = pattern.twoThetaMin()
                + (x - LEFT) * (pattern.twoThetaMax() - pattern.twoThetaMin()) / (RIGHT - LEFT);
        double position = (value - pattern.twoThetaMin()) / pattern.step();
        int index = (int) Math.floor(position);
        if (index < 0) {
            return 0.0;
        }
        if (index >= pattern.intensity().length - 1) {
            return pattern.intensity()[pattern.intensity().length - 1];
        }
        double fraction = position - index;
        return pattern.intensity()[index] * (1.0 - fraction) + pattern.intensity()[index + 1] * fraction;
    }

    private int x(double twoTheta) {
        double ratio = (twoTheta - pattern.twoThetaMin())
                / (pattern.twoThetaMax() - pattern.twoThetaMin());
        return LEFT + (int) Math.round(ratio * (RIGHT - LEFT));
    }

    private String number(double value) {
        return Integer.toString((int) Math.round(value));
    }

    private String ascii(String value) {
        StringBuilder text = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            text.append(character >= 0x20 && character <= 0x7e ? character : '?');
        }
        return text.toString();
    }
}
