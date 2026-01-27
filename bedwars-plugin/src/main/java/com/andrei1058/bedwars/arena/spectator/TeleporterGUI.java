/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact e-mail: andrew.dascalu@gmail.com
 */

package com.andrei1058.bedwars.arena.spectator;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.arena.Arena;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.andrei1058.bedwars.BedWars.nms;
import static com.andrei1058.bedwars.api.language.Language.getList;
import static com.andrei1058.bedwars.api.language.Language.getMsg;

@SuppressWarnings("WeakerAccess")
public class TeleporterGUI {

    //Don't remove "_" because it's used as a separator somewhere
    public static final String NBT_SPECTATOR_TELEPORTER_GUI_HEAD = "spectatorTeleporterGUIhead_";
    // Pattern for HEX colors: &#RRGGBB
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private static HashMap<Player, Inventory> refresh = new HashMap<>();

    /**
     * Refresh the Teleporter GUI for a player
     */
    public static void refreshInv(Player p, Inventory inv) {
        if (p.getOpenInventory() == null) return;
        IArena arena = Arena.getArenaByPlayer(p);
        if (arena == null) {
            p.closeInventory();
            return;
        }
        List<Player> players = arena.getPlayers();
        for (int i = 0; i < inv.getSize(); i++) {
            if (i < players.size()) {
                inv.setItem(i, createHead(players.get(i), p));
            } else {
                inv.setItem(i, new ItemStack(Material.AIR));
            }
        }
    }

    /**
     * Opens the Teleporter GUI to a Player
     */
    public static void openGUI(Player p) {
        IArena arena = Arena.getArenaByPlayer(p);
        if (arena == null) return;

        int playerCount = arena.getPlayers().size();
        int size = (playerCount % 9) == 0 ? playerCount : ((int) Math.ceil(playerCount / 9.0)) * 9;

        if (size > 54) {
            size = 54;
        }

        Inventory inv = Bukkit.createInventory(p, size, getMsg(p, Messages.ARENA_SPECTATOR_TELEPORTER_GUI_NAME));
        refreshInv(p, inv);
        refresh.put(p, inv);
        p.openInventory(inv);
    }

    /**
     * Get a HashMap of players with Teleporter GUI opened
     */
    public static HashMap<Player, Inventory> getRefresh() {
        return refresh;
    }

    /**
     * Refresh the Teleporter GUI for all players with it opened
     */
    public static void refreshAllGUIs() {
        for (Map.Entry<Player, Inventory> e : new HashMap<>(getRefresh()).entrySet()) {
            refreshInv(e.getKey(), e.getValue());
        }
    }

    /**
     * Create a player head
     */
/**
     * Create a player head
     */
    private static ItemStack createHead(Player targetPlayer, Player GUIholder) {
        ItemStack i = nms.getPlayerHead(targetPlayer, null);
        ItemMeta im = i.getItemMeta();
        assert im != null;
        IArena currentArena = Arena.getArenaByPlayer(targetPlayer);
        ITeam targetPlayerTeam = currentArena.getTeam(targetPlayer);

        // Fetch display name with placeholders replaced
        String displayName = getMsg(GUIholder, Messages.ARENA_SPECTATOR_TELEPORTER_GUI_HEAD_NAME)
                .replace("{vPrefix}", BedWars.getChatSupport().getPrefix(targetPlayer))
                .replace("{vSuffix}", BedWars.getChatSupport().getSuffix(targetPlayer))
                .replace("{team}", targetPlayerTeam.getDisplayName(Language.getPlayerLanguage(GUIholder)))
                .replace("{teamColor}", String.valueOf(targetPlayerTeam.getColor().chat()))
                // ZMIANA: Dodano kolor drużyny przed nickiem gracza
                .replace("{player}", targetPlayerTeam.getColor().chat() + targetPlayer.getDisplayName())
                .replace("{playername}", targetPlayerTeam.getColor().chat() + targetPlayer.getName());

        // Apply HEX color translation to the final string
        im.setDisplayName(colorize(displayName));

        List<String> lore = new ArrayList<>();
        String health = String.valueOf((int)targetPlayer.getHealth() * 100 / targetPlayer.getHealthScale());
        for (String s : getList(GUIholder, Messages.ARENA_SPECTATOR_TELEPORTER_GUI_HEAD_LORE)) {
            // Apply replacements and then colorize
            lore.add(colorize(s.replace("{health}", health).replace("{food}", String.valueOf(targetPlayer.getFoodLevel()))));
        }
        im.setLore(lore);
        i.setItemMeta(im);
        return nms.addCustomData(i, NBT_SPECTATOR_TELEPORTER_GUI_HEAD + targetPlayer.getName());
    }

    /**
     * Remove a player from the refresh list and close gui
     */
    public static void closeGUI(Player p) {
        if (getRefresh().containsKey(p)) {
            refresh.remove(p);
            p.closeInventory();
        }
    }

    /**
     * Translate HEX colors and standard codes
     */
    private static String colorize(String message) {
        // Only run HEX logic on 1.16+
        try {
            Matcher matcher = HEX_PATTERN.matcher(message);
            while (matcher.find()) {
                String hexCode = matcher.group(1);
                // Use reflection to fix compilation on older APIs that are missing ChatColor.of()
                java.lang.reflect.Method ofMethod = ChatColor.class.getMethod("of", String.class);
                String replacement = ofMethod.invoke(null, "#" + hexCode).toString();
                message = message.replace("&#" + hexCode, replacement);
            }
        } catch (Exception ignored) {
            // Fallback for older versions (pre 1.16)
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
