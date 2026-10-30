package dev.taikin.mcplugins.cifviewer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import dev.taikin.miviewer.common.ElementInfo;
import java.io.File;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class CifConfigTest {

    @Test
    void parsesSphereModeAndColors() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("render-mode", "sphere");
        configuration.set("sphere-shadow", false);
        configuration.set("atom-colors.Na", "#112233");
        configuration.set("default-atom-color", "#010203");

        CifConfig config = new CifConfig(configuration, new File("."), Logger.getLogger("CifConfigTest"));

        assertEquals(CifConfig.RenderMode.MODEL, config.renderMode());
        assertFalse(config.sphereShadow());
        assertArrayEquals(new int[] {0x11, 0x22, 0x33}, config.atomColor("Na"));
        assertArrayEquals(new int[] {0x01, 0x02, 0x03}, config.atomColor("Cl"));
    }

    @Test
    void usesElementPaletteWhenNoOverrideIsSet() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("render-mode", "symbol");

        CifConfig config = new CifConfig(configuration, new File("."), Logger.getLogger("CifConfigTest"));

        assertEquals(CifConfig.RenderMode.SYMBOL, config.renderMode());
        assertArrayEquals(ElementInfo.color("Cl"), config.atomColor("Cl"));
    }
}
