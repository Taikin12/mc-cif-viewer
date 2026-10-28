package dev.taikin.mcplugins.cifviewer.xrd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class XraySourceTest {

    @Test
    void resolvesTargetMetals() {
        assertEquals(1.54056, XraySource.wavelength("Cu"), 1.0e-9);
        assertEquals(0.70930, XraySource.wavelength("MoKa1"), 1.0e-9);
    }

    @Test
    void acceptsNumericWavelength() {
        assertEquals(1.23, XraySource.wavelength("1.23"), 1.0e-9);
    }

    @Test
    void rejectsUnknownSource() {
        assertThrows(IllegalArgumentException.class, () -> XraySource.wavelength("unknown"));
    }
}
