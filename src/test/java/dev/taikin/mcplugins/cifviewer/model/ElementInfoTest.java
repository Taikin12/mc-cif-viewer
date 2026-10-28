package dev.taikin.mcplugins.cifviewer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ElementInfoTest {

    @Test
    void parsesOxidationStates() {
        assertEquals(1, ElementInfo.oxidationState("Na1+"));
        assertEquals(1, ElementInfo.oxidationState("Na+"));
        assertEquals(-1, ElementInfo.oxidationState("Cl1-"));
        assertEquals(-2, ElementInfo.oxidationState("O2-"));
        assertEquals(3, ElementInfo.oxidationState("Fe3+"));
        assertEquals(3, ElementInfo.oxidationState("Fe+3"));
        assertEquals(0, ElementInfo.oxidationState("Na1"));
    }

    @Test
    void ionicRadiiTouchForNaCl() {
        double distance = ElementInfo.ionicRadius("Na", 1) + ElementInfo.ionicRadius("Cl", -1);

        assertEquals(2.82, distance, 0.02);
    }

    @Test
    void usesChargeSpecificIonicRadius() {
        assertEquals(0.645, ElementInfo.ionicRadius("Fe", 3), 1.0e-9);
        assertEquals(0.78, ElementInfo.ionicRadius("Fe", 2), 1.0e-9);
    }
}
