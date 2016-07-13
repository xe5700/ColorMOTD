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
package net.andylizi.colormotd.bukkit.utils;

import java.lang.reflect.Method;
import static net.andylizi.colormotd.bukkit.BukkitMain.logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class FakePlayersOnlineHook extends Object{
    private Plugin hookPlugin;
    private Method getOnlinePlayers;
    private Method getMaxPlayers;
    
    public boolean init() throws UnsupportedOperationException {
        if(hookPlugin == null || getOnlinePlayers == null || getMaxPlayers == null){
            hookPlugin = Bukkit.getPluginManager().getPlugin("FakePlayersOnline");
            if(hookPlugin == null){
                throw new UnsupportedOperationException("找不到FakePlayersOnline插件!");
            }
            try {
                getOnlinePlayers = hookPlugin.getClass().getMethod("getPlayersOnline");
                getMaxPlayers = hookPlugin.getClass().getMethod("getMaxPlayers");
            } catch (NoSuchMethodException ex) {
                throw new UnsupportedOperationException("无法与FakePlayersOnline插件建立连接!");
            }
            return true;
        }else{
            return true;
        }
    }
    
    public int getOnlinePlayers(){
        try {
            return (int) getOnlinePlayers.invoke(hookPlugin);
        } catch (ReflectiveOperationException ex) {
            logger.severe("在获取FakePlayersOnline在线人数的过程中发生错误:");
            ex.printStackTrace();
            return 0;
        }
    }
    
    public int getMaxPlayers(){
        try {
            return (int) getMaxPlayers.invoke(hookPlugin);
        } catch (ReflectiveOperationException ex) {
            logger.severe("在获取FakePlayersOnline最大人数的过程中发生错误:");
            ex.printStackTrace();
            return 0;
        }
    }
}
