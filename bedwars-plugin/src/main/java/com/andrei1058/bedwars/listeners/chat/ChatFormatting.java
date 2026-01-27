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
        Language language = getPlayerLanguage(p);

        // in shared mode we don't want messages from outside the arena to be seen in game
        if (getServerType() == ServerType.SHARED && Arena.getArenaByPlayer(p) == null) {
            e.getRecipients().removeIf(pl -> Arena.getArenaByPlayer(pl) != null);
            // Brak return tutaj, aby kod mógł przejść do formatowania lobby w bloku else poniżej,
            // jeśli serwer nie jest w trybie BUNGEE (gdzie SHARED zwykle oznacza brak obsługi lobby przez ten plugin).
            // Ale dla bezpieczeństwa, w trybie SHARED plugin zwykle nie obsługuje czatu na spawnie.
            // Jeśli chcesz formatowanie na spawnie w trybie SHARED, usuń poniższy return lub zmodyfikuj logikę.
            // Zostawiam standardowe zachowanie (return), ale dodaję logikę dla MULTIARENA poniżej.
             if (!config.getBoolean(ConfigPath.GENERAL_CHAT_FORMATTING)) return;
        }

        // handle chat color for the MESSAGE content
        if (Permissions.hasPermission(p, Permissions.PERMISSION_CHAT_COLOR, Permissions.PERMISSION_VIP, Permissions.PERMISSION_ALL)) {
            e.setMessage(translate(e.getMessage()));
        }

        // handle lobby world for multi arena
        if (getServerType() == ServerType.MULTIARENA && p.getWorld().getName().equalsIgnoreCase(BedWars.getLobbyWorld())) {
            setRecipients(e, p.getWorld().getPlayers());
        }

        // handle arena chat
        if (Arena.getArenaByPlayer(p) != null) {
            IArena a = Arena.getArenaByPlayer(p);

            // Czat dla obserwatorów (spectators)
            if (a.isSpectator(p)) {
                // POPRAWKA 1: Jeśli gracz ma permisję admina/OP, widzą go wszyscy (gracze + inni obserwatorzy)
                if (p.hasPermission(Permissions.PERMISSION_ALL) || p.isOp() || p.hasPermission("bedwars.admin")) {
                    setRecipients(e, a.getPlayers(), a.getSpectators());
                } else {
                    // Zwykły gracz widzi tylko innych obserwatorów
                    setRecipients(e, a.getSpectators());
                }
                
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
            if (isShouting(msg, language)) {
                if (!(p.hasPermission(Permissions.PERMISSION_SHOUT_COMMAND) || p.hasPermission(Permissions.PERMISSION_ALL))) {
                    e.setCancelled(true);
                    p.sendMessage(Language.getMsg(p, Messages.COMMAND_NOT_FOUND_OR_INSUFF_PERMS));
                    return;
                }
                if (ShoutCommand.isShoutCooldown(p)) {
                    e.setCancelled(true);
                    p.sendMessage(language.m(Messages.COMMAND_COOLDOWN)
                            .replace("{seconds}", String.valueOf(Math.round(ShoutCommand.getShoutCooldown(p))))
                    );
                    return;
                }

                ShoutCommand.updateShout(p);
                setRecipients(e, a.getPlayers(), a.getSpectators());

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
            if (a.getMaxInTeam() == 1) {
                setRecipients(e, a.getPlayers(), a.getSpectators());
            } else {
                setRecipients(e, team.getMembers());
            }

            e.setFormat(parsePHolders(language.m(Messages.FORMATTING_CHAT_TEAM), p, team));
        } else {
            // POPRAWKA 2: Obsługa formatowania na LOBBY (gdy gracz nie jest na arenie)
            // Używamy ConfigPath.GENERAL_CHAT_FORMATTING (formatChat: true w configu)
            if (config.getBoolean(ConfigPath.GENERAL_CHAT_FORMATTING)) {
                // Upewnij się, że w messages_en.yml (i innych językach) masz sekcję:
                // chat:
                //   lobby: ...
                // co odpowiada Messages.FORMATTING_CHAT_LOBBY w kodzie źródłowym.
                // Jeśli kompilator wyrzuci błąd, że nie ma takiego pola, upewnij się, że masz najnowsze API.
                // Jeśli nie masz, możesz tu wpisać stringa na sztywno lub użyć innej zmiennej.
                // Standardowo w BW1058 jest to Messages.FORMATTING_CHAT_LOBBY.
                try {
                    e.setFormat(parsePHolders(language.m(Messages.FORMATTING_CHAT_LOBBY), p, null));
                } catch (NoSuchFieldError err) {
                    // Fallback jeśli używasz starej wersji API
                    e.setFormat(parsePHolders("{vPrefix}{player}{vSuffix}: {message}", p, null));
                }
            }
        }
    }

    /**
     * Tłumaczy kody kolorów, w tym HEX (&#RRGGBB oraz #RRGGBB).
     */
    public static String translate(String message) {
        try {
            Pattern pattern = Pattern.compile("(&#|#)([A-Fa-f0-9]{6})");
            Matcher matcher = pattern.matcher(message);
            while (matcher.find()) {
                String fullMatch = matcher.group();
                String colorCode = matcher.group(2);
                Object colorObj = net.md_5.bungee.api.ChatColor.class
                        .getMethod("of", String.class)
                        .invoke(null, "#" + colorCode);
                message = message.replace(fullMatch, colorObj.toString());
                matcher = pattern.matcher(message);
            }
        } catch (Exception ignored) {
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static String parsePHolders(String content, Player player, @Nullable ITeam team) {
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

        String processed = SupportPAPI.getSupportPAPI().replace(player, content);
        processed = translate(processed);
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
