/*
 * ColorMOTD 
 * Copyright (C) 2016 andylizi
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.andylizi.colormotd.listener;

import net.andylizi.colormotd.Main;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerLoginEvent;

public final class JoinListener extends Object implements org.bukkit.event.Listener{
    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerLoginEvent e){
        if(Main.smode){
            if(e.getPlayer().isOp()||e.getPlayer().hasPermission("colormotd.smode.join")){
                e.allow();
            }else{
                e.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, ChatColor.translateAlternateColorCodes('&', Main.config.smodeKickCause));
            }
        }
    }
}
