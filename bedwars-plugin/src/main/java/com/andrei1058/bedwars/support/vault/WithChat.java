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

package com.andrei1058.bedwars.support.vault;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WithChat implements Chat {

    private static net.milkbowl.vault.chat.Chat chat;
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    @Override
    public String getPrefix(Player p) {
        return colorize(chat.getPlayerPrefix(p));
    }

    @Override
    public String getSuffix(Player p) {
        return colorize(chat.getPlayerSuffix(p));
    }

    public static void setChat(net.milkbowl.vault.chat.Chat chat) {
        WithChat.chat = chat;
    }

    private String colorize(String message) {
        if (message == null) return "";
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
            // Fallback for versions older than 1.16 where ChatColor.of doesn't exist
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
