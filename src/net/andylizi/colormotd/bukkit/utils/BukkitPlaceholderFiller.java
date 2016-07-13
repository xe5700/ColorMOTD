/*
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

import com.comphenix.protocol.wrappers.WrappedServerPing.CompressedImage;

import java.util.List;
import java.util.logging.Level;
import javax.script.ScriptException;

import net.andylizi.colormotd.bukkit.BukkitConfig;
import net.andylizi.colormotd.utils.BasePlaceholderFiller;
import static net.andylizi.colormotd.bukkit.BukkitMain.*;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class BukkitPlaceholderFiller extends BasePlaceholderFiller{
    protected final BukkitConfig config;
    protected int online = -1;
    
    public BukkitPlaceholderFiller(BukkitConfig config) {
        super(config);
        this.config = config;
    }
    
    @Override
    public Object randomIcon() {
        if (config.smode) {
            if (config.compressedSmodeIcon != null) {
                return config.compressedSmodeIcon;
            }
        }
        List<CompressedImage> iconList = config.compressedIcon;
        return (iconList.isEmpty() ? null : (iconList.size() == 1 ? iconList.get(0) 
                : iconList.get(random.nextInt(iconList.size()))));
    }
    
    @Override
    public String fill(String str, String ip) {
        str = super.fill(str, ip);
        if(essentials && (str.contains("%TPS%") || str.contains("%STATE%"))){
            double tps = 20;
            try {
                tps = essHook.getTPS();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            str = str.replace("%TPS%", config.decimalFormater.format(tps));
            try {
                if(config.stateFormater == null){
                    logger.warning("初始化TPSFormater失败,无法使用%STATE%标签,可能无法读取\"formater.js\"");
                    str = str.replace("%STATE%", "{无法格式化}");
                }else{
                    str = str.replace("%STATE%", config.stateFormater.format(tps));
                }
            } catch (ScriptException ex) {
                logger.severe("在执行TPS格式化的过程中发生错误,可能是formater.js格式错误");
                ex.printStackTrace();
                str = str.replace("%STATE%", "{格式化出错}");
            }
        }else{
            str = str.replace("%TPS%", "{无法获取}");
            str = str.replace("%STATE%", "{无法获取}");
        }
        if (str.contains("%ONLINE%") || str.contains("%MAXPLAYER%")) {
            if(fakePlayersOnline){
                str = str.replace("%ONLINE%", Integer.toString(fakePlayersOnlineHook.getOnlinePlayers()));
                str = str.replace("%MAXPLAYER%", Integer.toString(fakePlayersOnlineHook.getMaxPlayers()));
            }else if (config.useBungeeCord || config.useRedisBungee) {
                str = str.replace("%MAXPLAYER%", Integer.toString(Bukkit.getMaxPlayers()));
                try {
                    str = str.replace("%ONLINE%", Integer.toString(BungeeChannel.online));
                } catch (Throwable t) {
                    logger.log(Level.SEVERE, "从BungeeCord或RedisBungee获取在线人数失败!请确认您的服务器在使用BungeeCord或RedisBungee");
                    t.printStackTrace();
                    str = str.replace("%ONLINE%", Integer.toString(ReflectFactory.getPlayers().length));
                }
            } else {
                str = str.replace("%MAXPLAYER%", Integer.toString(Bukkit.getMaxPlayers()));
                str = str.replace("%ONLINE%", Integer.toString(online == -1 ? (online = ReflectFactory.getPlayers().length) : online));
            }
        }
        return ChatColor.translateAlternateColorCodes('&', str);
    }

    @Override
    public void reflushTime() {
        online = ReflectFactory.getPlayers().length;
    }
}
