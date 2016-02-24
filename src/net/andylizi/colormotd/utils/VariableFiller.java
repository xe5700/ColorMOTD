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
package net.andylizi.colormotd.utils;

import com.comphenix.protocol.wrappers.WrappedServerPing;
import java.awt.image.BufferedImage;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.logging.Level;

import javax.script.ScriptException;
import net.andylizi.colormotd.Main;
import static net.andylizi.colormotd.Main.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;

public final class VariableFiller extends BukkitRunnable{
    static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    static String date;
    static String time;
    static int online = -1;
    
    private VariableFiller(){
        new BukkitRunnable() {
            @Override
            public void run() {
                date = DATE_FORMAT.format(new Date());
            }
        }.runTaskTimerAsynchronously(Main.getInstance(), 0L, 20L * 60L * 60L * 12L);
    }
    
    static{
        DATE_FORMAT.setTimeZone(TimeZone.getDefault());
        TIME_FORMAT.setTimeZone(TimeZone.getDefault());
        new VariableFiller().runTaskTimerAsynchronously(Main.getInstance(), 0L, 20L * 2L);
    }
    
    public static WrappedServerPing.CompressedImage randomIcon(List<WrappedServerPing.CompressedImage> iconList) {
        if (smode) {
            if (config.smodeIcon != null) {
                return config.smodeIcon;
            }
        }
        try {
            return (iconList.isEmpty() ?
                    WrappedServerPing.CompressedImage.fromPng(new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB))
                    :
                    (iconList.size() == 1 ? iconList.get(0) : iconList.get(new Random().nextInt(iconList.size()))));
        } catch (IOException ex) {
            return null;
        }
    }

    public static String randomMotd(List<String> motdList) {
        if (smode) {
            if (config.smodeMotd != null) {
                if (!config.smodeMotd.trim().isEmpty()) {
                    return config.smodeMotd;
                }
            }
        }
        return (motdList.isEmpty() ? "无MOTD" : (motdList.size() == 1 ? motdList.get(0) : motdList.get(new Random().nextInt(motdList.size()))));
    }

    public static String getDate() {
        if(date == null){
            date = DATE_FORMAT.format(new Date());
        }
        return date;
    }

    public static String getTime() {
        if(time == null){
            time = TIME_FORMAT.format(new Date());
        }
        return time;
    }

    public static String replace(String str, String ip) {
        str = str.replace("%TIME%", getTime());
        str = str.replace("%DATE%", getDate());
        
        if((str.contains("%TPS%")||str.contains("%STATE%")) && essentials){
            double tps = 20;
            try {
                tps = essHook.getTPS();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            str = str.replace("%TPS%", config.decimalFormater.format(tps));
            try {
                if(stateFormater == null){
                    logger.warning("初始化TPSFormater失败,无法使用%STATE%标签,可能无法读取\"formater.js\"");
                    str = str.replace("%STATE%", "{无法格式化}");
                }else{
                    str = str.replace("%STATE%", stateFormater.format(tps));
                }
            } catch (ScriptException ex) {
                logger.severe("在执行TPS格式化过程中发生错误,可能是formater.js格式错误");
                ex.printStackTrace();
                str = str.replace("%STATE%", "{格式化出错}");
            }
        }else{
            str = str.replace("%TPS%", "{无法获取}");
            str = str.replace("%STATE%", "{无法获取}");
        }
        str = str.replace("\\n", "\n");
        if (str.contains("%LOC%") || str.contains("%ISP%")) {
            String result = AttributionUtil.getResult(ip);
            if (str.contains("%LOC%")) {
                str = str.replace("%LOC%", AttributionUtil.formatLocation(result));
            }
            if (str.contains("%ISP%")) {
                str = str.replace("%ISP%", AttributionUtil.formatISP(result));
            }
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
    public void run() {
        time = TIME_FORMAT.format(new Date());
        online = ReflectFactory.getPlayers().length;
    }
}
