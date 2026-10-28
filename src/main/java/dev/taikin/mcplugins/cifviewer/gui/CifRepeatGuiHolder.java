package dev.taikin.mcplugins.cifviewer.gui;

import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class CifRepeatGuiHolder implements InventoryHolder {

    private final UUID playerId;
    private Inventory inventory;

    public CifRepeatGuiHolder(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID playerId() {
        return playerId;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    void inventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
