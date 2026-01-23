package com.andrei1058.bedwars.listeners;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.events.gameplay.GameEndEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.UUID;

public class GameEndListener implements Listener {

    @EventHandler
    public void cleanInventoriesAndDroppedItems(@NotNull GameEndEvent event) {
        if (event.getArena().getPlayers().isEmpty()) {
            return;
        }

        // Przygotowanie przedmiotu "Rematch"
        ItemStack rematchItem = BedWars.nms.createItemStack(Material.PAPER.name(), 1, (short) 0);
        ItemMeta meta = rematchItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§aZagraj ponownie §7(Prawy klik)");
            meta.setLore(Arrays.asList("§7Kliknij, aby dołączyć", "§7do nowej gry!"));
            rematchItem.setItemMeta(meta);
        }
        // Dodajemy tag REMATCH_<nazwaGrupy>
        rematchItem = BedWars.nms.addCustomData(rematchItem, "REMATCH_" + event.getArena().getGroup());

        // Czyść ekwipunek i daj przedmiot wszystkim graczom w arenie
        for (Player p : event.getArena().getPlayers()) {
            p.getInventory().clear();
            p.getInventory().setItem(4, rematchItem); // Slot 4 (środek paska)
            p.updateInventory();
        }

        // Usuń leżące przedmioty
        World game = event.getArena().getWorld();
        for (Entity item : game.getEntities()) {
            if (item instanceof Item || item instanceof ItemStack){
                item.remove();
            }
        }
    }
}
