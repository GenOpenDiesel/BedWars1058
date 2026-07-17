package com.andrei1058.bedwars.arena.upgrades;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.andrei1058.bedwars.BedWars.config;
import static com.andrei1058.bedwars.BedWars.plugin;

public class HealPoolTask extends BukkitRunnable {

    private ITeam bwt;
    private int maxX, minX, maxY, minY, maxZ, minZ;
    private IArena arena;
    private Random r = new Random();
    private Location l;

    // CopyOnWriteArrayList: async particle task and game-end cleanup share this list
    private static final List<HealPoolTask> healPoolTasks = new CopyOnWriteArrayList<>();

    public HealPoolTask(ITeam bwt){
        this.bwt = bwt;
        if (bwt == null || bwt.getSpawn() == null){
            removeForTeam(this.bwt);
            cancel();
            return;
        }
        int radius = bwt.getArena().getConfig().getInt(ConfigPath.ARENA_ISLAND_RADIUS);
        Location teamspawn = bwt.getSpawn();
        this.maxX = (teamspawn.getBlockX() + radius);
        this.minX = (teamspawn.getBlockX() - radius);
        this.maxY = (teamspawn.getBlockY() + radius);
        this.minY = (teamspawn.getBlockY() - radius);
        this.maxZ = (teamspawn.getBlockZ() + radius);
        this.minZ = (teamspawn.getBlockZ() - radius);
        this.arena = bwt.getArena();
        this.runTaskTimerAsynchronously(plugin, 0, 80L);
        healPoolTasks.add(this);
    }

    @Override
    public void run(){
        //null checks
        if ((bwt == null) || (bwt.getSpawn() == null) || (arena == null)){
            healPoolTasks.remove(this);
            cancel();
            return;
        }

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    l = new Location(arena.getWorld(), x + .5, y + .5, z +.5);
                    if (l.getBlock().getType() != Material.AIR) continue;
                    int chance = r.nextInt(9);
                    if (chance == 0) {
                        if (config.getBoolean(ConfigPath.GENERAL_CONFIGURATION_HEAL_POOL_SEEN_TEAM_ONLY)) {
                            for (Player p : bwt.getMembers()) {
                                BedWars.nms.playVillagerEffect(p, l);
                            }
                        }
                        else
                        {
                            for (Player p : arena.getPlayers()) {
                                BedWars.nms.playVillagerEffect(p, l);
                            }
                        }
                    }
                }
            }
        }
    }

    public static boolean exists(IArena arena, ITeam bwt){
        if (healPoolTasks.isEmpty()) return false;
        for (HealPoolTask hpt : healPoolTasks) {
            if (hpt.getArena() == arena && hpt.getBwt() == bwt) return true;
        }
        return false;
    }

    public static void removeForArena(IArena a){
        if (healPoolTasks.isEmpty() || a == null) return;
        healPoolTasks.removeIf(hpt -> {
            if (hpt == null) return true;
            if (a.equals(hpt.getArena())) {
                hpt.cancel();
                return true;
            }
            return false;
        });
    }

    public static void removeForArena(String a){
        if (healPoolTasks.isEmpty() || a == null) return;
        healPoolTasks.removeIf(hpt -> {
            if (hpt == null || hpt.getArena() == null) return true;
            if (a.equals(hpt.getArena().getWorldName())) {
                hpt.cancel();
                return true;
            }
            return false;
        });
    }

    public static void removeForTeam(ITeam team){
        if (healPoolTasks.isEmpty() || team == null) return;
        healPoolTasks.removeIf(hpt -> {
            if (hpt == null) return true;
            if (team.equals(hpt.getBwt())) {
                hpt.cancel();
                return true;
            }
            return false;
        });
    }

    public ITeam getBwt() {return bwt;}

    public IArena getArena() {return arena;}
}
