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

package com.andrei1058.bedwars.commands.bedwars.subcmds.regular;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.command.ParentCommand;
import com.andrei1058.bedwars.api.command.SubCommand;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.arena.Misc;
import com.andrei1058.bedwars.arena.SetupSession;
import com.andrei1058.bedwars.commands.bedwars.MainCommand;
import com.andrei1058.bedwars.stats.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.andrei1058.bedwars.api.language.Language.getMsg;

public class CmdStats extends SubCommand {

    public CmdStats(ParentCommand parent, String name) {
        super(parent, name);
        setPriority(16);
        showInList(false);
        setDisplayInfo(com.andrei1058.bedwars.commands.bedwars.MainCommand.createTC("§6 ▪ §7/"+ MainCommand.getInstance().getName()+" "+getSubCommandName(), "/"+getParent().getName()+" "+getSubCommandName(), "§fOpens the stats GUI. Usage: /"+getParent().getName()+" "+getSubCommandName()+" [player]"));
    }

    private static ConcurrentHashMap<UUID, Long> statsCoolDown = new ConcurrentHashMap<>();

    @Override
    public boolean execute(String[] args, CommandSender s) {
        if (s instanceof ConsoleCommandSender) return false;
        Player p = (Player) s;
        IArena a = Arena.getArenaByPlayer(p);
        if (a != null){
            if (!(a.getStatus() == GameState.starting || a.getStatus() == GameState.waiting)){
                if (!a.isSpectator(p)){
                    return false;
                }
            }
        }
        if (statsCoolDown.containsKey(p.getUniqueId())){
            if (System.currentTimeMillis() - 3000 >= statsCoolDown.get(p.getUniqueId())) {
                statsCoolDown.replace(p.getUniqueId(), System.currentTimeMillis());
            } else {
                //wait 3 seconds
                return true;
            }
        } else {
            statsCoolDown.put(p.getUniqueId(), System.currentTimeMillis());
        }
        if (args.length == 0 || args[0].equalsIgnoreCase(p.getName())) {
            Misc.openStatsGUI(p);
            return true;
        }

        /* another player: cached stats if online here, otherwise look the name up in the database */
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target != null && BedWars.getStatsManager().getUnsafe(target.getUniqueId()) != null) {
            Misc.openStatsGUI(p, null, target);
            return true;
        }
        String name = args[0];
        Bukkit.getScheduler().runTaskAsynchronously(BedWars.plugin, () -> {
            PlayerStats stats = BedWars.getRemoteDatabase().fetchStatsByName(name);
            if (stats == null) {
                Bukkit.getScheduler().runTask(BedWars.plugin, () -> {
                    if (p.isOnline()) p.sendMessage(getMsg(p, Messages.COMMAND_TP_PLAYER_NOT_FOUND));
                });
                return;
            }
            Misc.openStatsGUI(p, stats, null);
        });
        return true;
    }

    @Override
    public List<String> getTabComplete() {
        List<String> names = new ArrayList<>();
        for (Player pl : Bukkit.getOnlinePlayers()) names.add(pl.getName());
        return names;
    }


    @Override
    public boolean canSee(CommandSender s, com.andrei1058.bedwars.api.BedWars api) {
        if (s instanceof ConsoleCommandSender) return false;

        Player p = (Player) s;
        if (Arena.isInArena(p)) return false;

        if (SetupSession.isInSetupSession(p.getUniqueId())) return false;
        return hasPermission(s);
    }

    public static ConcurrentHashMap<UUID, Long> getStatsCoolDown() {
        return statsCoolDown;
    }
}
