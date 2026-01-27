package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.events.gameplay.GameEndEvent;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class GameEndListener implements Listener {

    @EventHandler
    public void cleanInventoriesAndDroppedItems(@NotNull GameEndEvent event) {
        if (event.getArena().getPlayers().isEmpty()) {
            return;
        }

        // Clear inventories and give leave item to ALL players (winners and losers)
        for (Player player : event.getArena().getPlayers()) {
            // Clear inventory for everyone
            player.getInventory().clear();

            // Create leave item (Bed) - "sb-leave" is the internal tag for the quit item
            ItemStack leaveItem = BedWars.nms.createItemStack(
                    BedWars.getForCurrentVersion("BED", "RED_BED", "RED_BED"),
                    1,
                    (short) 0
            );

            ItemMeta im = leaveItem.getItemMeta();
            if (im != null) {
                im.setDisplayName(Language.getMsg(player, Messages.ARENA_SPECTATOR_LEAVE_ITEM_NAME));
                im.setLore(Language.getList(player, Messages.ARENA_SPECTATOR_LEAVE_ITEM_LORE));
                leaveItem.setItemMeta(im);
            }

            // FIX: Zmieniono tag z "sb-leave" na "RUNCOMMAND_bw leave", aby obsłużyć kliknięcie przez Interact.java
            leaveItem = BedWars.nms.addCustomData(leaveItem, "RUNCOMMAND_bw leave");

            // Set item in the last slot (8)
            player.getInventory().setItem(8, leaveItem);
            player.updateInventory();
        }

        // clear dropped items
        World game = event.getArena().getWorld();
        for (Entity item : game.getEntities()) {
            if (item instanceof Item || item instanceof ItemStack){
                item.remove();
            }
        }
    }
}
