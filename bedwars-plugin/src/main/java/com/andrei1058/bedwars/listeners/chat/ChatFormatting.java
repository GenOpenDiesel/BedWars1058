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

package com.andrei1058.bedwars.listeners.chat;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.configuration.ConfigPath;
import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.language.Messages;
import com.andrei1058.bedwars.api.server.ServerType;
import com.andrei1058.bedwars.arena.Arena;
import com.andrei1058.bedwars.commands.shout.ShoutCommand;
import com.andrei1058.bedwars.configuration.Permissions;
import com.andrei1058.bedwars.support.papi.SupportPAPI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.andrei1058.bedwars.BedWars.*;
import static com.andrei1058.bedwars.api.language.Language.getMsg;
import static com.andrei1058.bedwars.api.language.Language.getPlayerLanguage;

public class ChatFormatting implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        if (e == null) return;
        Player p = e.getPlayer();

        // in shared mode we don't want messages from outside the arena to be seen in game
        if (getServerType() == ServerType.SHARED && Arena.getArenaByPlayer(p) == null) {
            e.getRecipients().removeIf(pl -> Arena.getArenaByPlayer(pl) != null);
            return;
        }

        // handle chat color for the MESSAGE content
        if (Permissions.hasPermission(p, Permissions.PERMISSION_CHAT_COLOR, Permissions.PERMISSION_VIP, Permissions.PERMISSION_ALL)) {
            e.setMessage(translate(e.getMessage()));
        }

        Language language = getPlayerLanguage(p);

        // handle lobby world for multi arena
        if (getServerType() == ServerType.MULTIARENA && p.getWorld().getName().equalsIgnoreCase(BedWars.getLobbyWorld())) {
            setRecipients(e, p.getWorld().getPlayers());
        }

        // handle arena chat
        if (Arena.getArenaByPlayer(p) != null) {
            IArena a = Arena.getArenaByPlayer(p);

            // Czat dla obserwatorów (spectators)
            if (a.isSpectator(p)) {
                setRecipients(e, a.getSpectators());
                e.setFormat(parsePHolders(language.m(Messages.FORMATTING_CHAT_SPECTATOR), p, null));
                return;
            }

            // Czat w lobby oczekiwania (waiting/starting)
            if (a.getStatus() == GameState.waiting || a.getStatus() == GameState.starting) {
                setRecipients(e, a.getPlayers());
                e.setFormat(parsePHolders(language.m(Messages.FORMATTING_CHAT_WAITING), p, null));
                return;
            }

            ITeam team = a.getTeam(p);
            String msg = e.getMessage();

            // --- LOGIKA KRZYKU (SHOUT) ---
            // Jeśli wiadomość zaczyna się od "!" (lub "shout"), widzą ją wszyscy
            if (isShouting(msg, language)) {
                // Sprawdzenie uprawnień
                if (!(p.hasPermission(Permissions.PERMISSION_SHOUT_COMMAND) || p.hasPermission(Permissions.PERMISSION_ALL))) {
                    e.setCancelled(true);
                    p.sendMessage(Language.getMsg(p, Messages.COMMAND_NOT_FOUND_OR_INSUFF_PERMS));
                    return;
                }
                // Sprawdzenie cooldownu
                if (ShoutCommand.isShoutCooldown(p)) {
                    e.setCancelled(true);
                    p.sendMessage(language.m(Messages.COMMAND_COOLDOWN)
                            .replace("{seconds}", String.valueOf(Math.round(ShoutCommand.getShoutCooldown(p))))
                    );
                    return;
                }

                ShoutCommand.updateShout(p);

                // Ustawiamy odbiorców na WSZYSTKICH (gracze + obserwatorzy)
                setRecipients(e, a.getPlayers(), a.getSpectators());

                // Usuwamy prefix "!" z wiadomości
                msg = clearShout(msg, language);
                if (msg.isEmpty()) {
                    e.setCancelled(true);
                    return;
                }
                e.setMessage(msg);
                e.setFormat(parsePHolders(language.m(Messages.FORMATTING_CHAT_SHOUT), p, team));
                return;
            }

            // --- LOGIKA CZATU DRUŻYNOWEGO ---
            // Jeśli wiadomość NIE jest krzykiem, widzi ją tylko drużyna

            // Wyjątek dla trybu Solo (1 osoba w teamie) - tam czat zwykły widzą wszyscy
            // (Jeśli chcesz, aby w Solo też trzeba było krzyczeć, usuń ten warunek if i zostaw tylko else)
            if (a.getMaxInTeam() == 1) {
                setRecipients(e, a.getPlayers(), a.getSpectators());
            } else {
                // W trybach drużynowych (Doubles, 3v3, 4v4) widzą tylko członkowie Twojego teamu
                setRecipients(e, team.getMembers());
            }

            e.setFormat(parsePHolders(language.m(Messages.FORMATTING_CHAT_TEAM), p, team));
            return;
        }
    } // <--- Dodano brakującą klamrę zamykającą metodę onChat

    /**
     * Tłumaczy kody kolorów, w tym HEX (&#RRGGBB oraz #RRGGBB).
     * Metoda jest static, aby można jej było użyć w parsePHolders.
     */
    public static String translate(String message) {
        // Obsługa HEX dla wersji 1.16+
        try {
            // Regex łapie zarówno &#RRGGBB jak i #RRGGBB
            Pattern pattern = Pattern.compile("(&#|#)([A-Fa-f0-9]{6})");
            Matcher matcher = pattern.matcher(message);
            while (matcher.find()) {
                String fullMatch = matcher.group();   // np. #FF0000 lub &#FF0000
                String colorCode = matcher.group(2);  // np. FF0000

                // Używamy refleksji, aby kod kompilował się na API 1.8.8,
                // ale pobierał metodę 'of' dynamicznie na serwerach 1.16+
                Object colorObj = net.md_5.bungee.api.ChatColor.class
                        .getMethod("of", String.class)
                        .invoke(null, "#" + colorCode);

                message = message.replace(fullMatch, colorObj.toString());
                matcher = pattern.matcher(message);
            }
        } catch (Exception ignored) {
            // Ignorujemy błędy na starszych wersjach serwera (poniżej 1.16)
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static String parsePHolders(String content, Player player, @Nullable ITeam team) {
        // Wstawianie zmiennych (prefixy, suffixy mogą zawierać kolory HEX!)
        content = content
                .replace("{vPrefix}", getChatSupport().getPrefix(player))
                .replace("{vSuffix}", getChatSupport().getSuffix(player))
                .replace("{playername}", player.getName())
                .replace("{level}", getLevelSupport().getLevel(player))
                .replace("{player}", player.getDisplayName());

        if (team != null) {
            String teamFormat = getMsg(player, Messages.FORMAT_PAPI_PLAYER_TEAM_TEAM)
                    .replace("{TeamColor}", team.getColor().chat() + "")
                    .replace("{TeamName}", team.getDisplayName(Language.getPlayerLanguage(player)).toUpperCase());
            content = content.replace("{team}", teamFormat);
        }

        // Obsługa PlaceholderAPI
        String processed = SupportPAPI.getSupportPAPI().replace(player, content);

        // KLUCZOWA POPRAWKA: Tłumaczenie kolorów w CAŁYM formacie (prefixy, ranga itp.)
        // Wcześniej tłumaczone było tylko e.getMessage(), a format pozostawał "surowy"
        processed = translate(processed);

        // Na końcu wstawiamy placeholder wiadomości (która jest już pokolorowana w onChat)
        return processed.replace("{message}", "%2$s");
    }

    private static boolean isShouting(String msg, Language lang) {
        return msg.startsWith("!") || msg.startsWith("shout") ||
                msg.startsWith("SHOUT") || msg.startsWith(lang.m(Messages.MEANING_SHOUT));
    }

    private static String clearShout(String msg, Language lang) {
        if (msg.startsWith("!")) msg = msg.replaceFirst("!", "");
        if (msg.startsWith("SHOUT")) msg = msg.replaceFirst("SHOUT", "");
        if (msg.startsWith("shout")) msg = msg.replaceFirst("shout", "");
        if (msg.startsWith(lang.m(Messages.MEANING_SHOUT))) {
            msg = msg.replaceFirst(lang.m(Messages.MEANING_SHOUT), "");
        }
        return msg.trim();
    }

    @SafeVarargs
    public static void setRecipients(AsyncPlayerChatEvent event, List<Player>... target) {
        if (!config.getBoolean(ConfigPath.GENERAL_CHAT_GLOBAL)) {
            event.getRecipients().clear();
            for (List<Player> list : target) {
                event.getRecipients().addAll(list);
            }
        }
    }
}
