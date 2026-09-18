package com.andrei1058.bedwars.stats;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marks the stats GUI so clicks can be blocked no matter whose stats are shown
 * (the title contains the target's name, so title matching is not reliable).
 */
public class StatsGUIHolder implements InventoryHolder {

    private Inventory inventory;

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
