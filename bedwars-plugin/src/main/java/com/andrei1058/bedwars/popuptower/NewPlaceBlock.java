package com.andrei1058.bedwars.popuptower;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.IArena; // Dodano import
import com.andrei1058.bedwars.api.arena.team.TeamColor;
import com.andrei1058.bedwars.api.region.Region;
import com.andrei1058.bedwars.arena.Arena;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class NewPlaceBlock {
    public NewPlaceBlock(Block b, String xyz, TeamColor color, Player p, boolean ladder, int ladderdata) {
        int x = Integer.parseInt(xyz.split(", ")[0]);
        int y = Integer.parseInt(xyz.split(", ")[1]);
        int z = Integer.parseInt(xyz.split(", ")[2]);
        if (b.getRelative(x, y, z).getType().equals(Material.AIR)) {
            
            // POPRAWKA: Pobieramy arenę do zmiennej i sprawdzamy, czy nie jest nullem
            IArena arena = Arena.getArenaByPlayer(p);
            if (arena == null) {
                return; // Jeśli gracz nie jest już na arenie, przerywamy stawianie bloku
            }

            for (Region r : arena.getRegionsList()) // Używamy zmiennej 'arena' zamiast ponownego pobierania
                if (r.isInRegion(b.getRelative(x, y, z).getLocation()))
                    return;

            if (!ladder)
                BedWars.nms.placeTowerBlocks(b, arena, color, x, y, z);
            else
                BedWars.nms.placeLadder(b, x, y, z, arena, ladderdata);
        }

    }
}
