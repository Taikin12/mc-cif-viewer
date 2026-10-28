package dev.taikin.mcplugins.cifviewer.xrd;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Image;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapFont;
import org.bukkit.map.MapView;
import org.junit.jupiter.api.Test;

class XrdMapRendererTest {

    @Test
    void rendersWithoutThrowing() {
        int points = 401;
        double[] x = new double[points];
        double[] y = new double[points];
        for (int index = 0; index < points; index++) {
            x[index] = 10.0 + index * 0.2;
            y[index] = index % 20 == 0 ? 1.0 : 0.05;
        }
        XrdPattern pattern = new XrdPattern("test", 1.5406, 10.0, 90.0, 0.2, x, y, 10);
        TestCanvas canvas = new TestCanvas();

        new XrdMapRenderer(pattern).render(null, canvas, null);

        assertTrue(canvas.painted());
        assertTrue(canvas.textCalls() > 0);
    }

    private static final class TestCanvas implements MapCanvas {

        private final Color[][] pixels = new Color[128][128];
        private int textCalls;

        @Override
        public MapView getMapView() {
            return null;
        }

        @Override
        public MapCursorCollection getCursors() {
            return null;
        }

        @Override
        public void setCursors(MapCursorCollection cursors) {
        }

        @Override
        public void setPixelColor(int x, int y, Color color) {
            if (x >= 0 && x < 128 && y >= 0 && y < 128) {
                pixels[x][y] = color;
            }
        }

        @Override
        public Color getPixelColor(int x, int y) {
            return pixels[x][y];
        }

        @Override
        public Color getBasePixelColor(int x, int y) {
            return pixels[x][y];
        }

        @Override
        public void setPixel(int x, int y, byte color) {
        }

        @Override
        public byte getPixel(int x, int y) {
            return 0;
        }

        @Override
        public byte getBasePixel(int x, int y) {
            return 0;
        }

        @Override
        public void drawImage(int x, int y, Image image) {
        }

        @Override
        public void drawText(int x, int y, MapFont font, String text) {
            textCalls++;
        }

        boolean painted() {
            for (Color[] row : pixels) {
                for (Color color : row) {
                    if (color != null) {
                        return true;
                    }
                }
            }
            return false;
        }

        int textCalls() {
            return textCalls;
        }
    }
}
