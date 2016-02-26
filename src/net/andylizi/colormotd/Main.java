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
package net.andylizi.colormotd;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.utility.MinecraftVersion;

import java.io.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.andylizi.colormotd.utils.*;

import net.andylizi.colormotd.listener.JoinListener;

import net.andylizi.colormotd.metrics.MetricsLite;

import net.andylizi.colormotd.listener.MOTDPacketListener;
import net.andylizi.colormotd.commands.CommandHandler;
import net.andylizi.colormotd.updater.Updater;


import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class Main extends JavaPlugin implements Plugin {

    public static Main plugin;
    public static Logger logger;
    public static final int buildVersion = 14;
    
    protected static ProtocolManager pm;
    public static final String msgPrefix = "§6[§aColorMOTD§6]§r ";
    public static Config config;

    public static String protocolLibVersion;
    public static StateFormater stateFormater;
    public static Updater updater;
    public static EssentialsHook essHook;
    public static FakePlayersOnlineHook fakePlayersOnlineHook;

    public static boolean smode = false;
    
    public static boolean essentials;
    public static boolean fakePlayersOnline;

    public Main() {
        plugin = this;
    }

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        logger = getLogger();

        Plugin protocolLib = Bukkit.getPluginManager().getPlugin("ProtocolLib");
        if (protocolLib != null && protocolLib.isEnabled()) {
            protocolLibVersion = protocolLib.getDescription().getVersion();
            logger.log(Level.INFO, "├使用 ProtocolLib {0} 作为前置", protocolLibVersion);
            
            pm = ProtocolLibrary.getProtocolManager();
            MinecraftVersion version = pm.getMinecraftVersion();
            if (version.compareTo(MinecraftVersion.WORLD_UPDATE) < 0) {
                severe("此插件最低只能在1.7.2 (World Update)服务器上运行!");
            }
        } else {
            severe("└无法连接到ProtocolLib\n\t\t请确认您是否安装了ProtocolLib插件作为前置?");
            return;
        }

        try {
            if (Integer.valueOf(protocolLibVersion.replace(".", "")) < 360) {
                UpdaterInjection.inject(protocolLib);
            }
        } catch (Throwable ex) {
            //Ignore
        }

        Bukkit.getPluginManager().registerEvents(new JoinListener(), this);

        try {
            logger.info("├正在创建MOTD监听器...");
            pm.addPacketListener(new MOTDPacketListener(this, pm));
        } catch (Error e) {
            severe("在试图创建监听器过程中出现严重错误,此插件可能不支持服务器上的ProtocolLib版本("+protocolLibVersion+")!");
            e.printStackTrace();
        } catch (Throwable e) {
            severe("在创建监听器过程中出现严重错误!ProtocolLib版本"+protocolLibVersion);
            e.printStackTrace();
        }

        try {
            config = new Config(getDataFolder());
            config.loadConfig();
        } catch (Throwable ex) {
            logger.log(Level.SEVERE, "├在加载配置文件过程中发生严重错误,请检查你的配置文件!!!");
            ex.printStackTrace();
            config.configLoadFailed();
        }

        try {
            new MetricsLite(this).start();
        } catch (Throwable t) {
            logger.log(Level.WARNING, "├启动MetricsLite失败: {0}", t.toString());
        }

        essentials = (Bukkit.getPluginManager().getPlugin("Essentials") != null);

        if (essentials) {
            try {
                essHook = new EssentialsHook();
                logger.info("├找到Essentials,建立连接...");
            } catch (UnsupportedOperationException ex) {
                logger.log(Level.SEVERE, "├连接Essentials失败!将无法使用%TPS%变量和%STATE%变量!");
                essentials = false;
            }
        } else {
            logger.warning("├找不到Essentials,将无法使用%TPS%变量和%STATE%变量!");
        }

        fakePlayersOnline = (Bukkit.getPluginManager().getPlugin("FakePlayersOnline") != null);

        if (fakePlayersOnline) {
            try {
                (fakePlayersOnlineHook = new FakePlayersOnlineHook()).init();
                logger.info("├找到FakePlayersOnline,建立连接...");
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "└连接FakePlayersOnline失败!");
                ex.printStackTrace();
                fakePlayersOnline = false;
            }
        }

        if (config.autoUpdate) {
            new Timer("ColorMOTDUpdater", true).scheduleAtFixedRate((updater = new Updater(getFile().getName())), taskSync(10L * 1000L), 60L * 60L * 1000L);
        } else {
            logger.info("├您在配置文件里禁止了自动更新,那记得经常去发布贴检查有没有新版本哦~");
        }
        try {
            if (config.useBungeeCord || config.useRedisBungee) {
                logger.log(Level.INFO, "├尝试建立与Bungee的连接...");
                new BungeeChannel(this);
            }
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "└连接Bungee失败!无法获取Bungee服务器真实在线人数");
            t.printStackTrace();
            config.useBungeeCord = config.useRedisBungee = false;
        } finally {
            CommandHandler commandExecutor = new CommandHandler(this);
            Bukkit.getPluginCommand("cmotdr").setExecutor(commandExecutor);
            Bukkit.getPluginCommand("smode").setExecutor(commandExecutor);
            Bukkit.getPluginCommand("colormotd").setExecutor(commandExecutor);
            System.out.println(this.getDescription().getFullName() + "加载完成,用时" + (System.currentTimeMillis() - startTime) + "毫秒");
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                for (String str : "ServerlistMOTD,PlayerCountMessage,MOTDColor,YuleMotd,AnimMOTD,SexyMotd,UniqueMOTD,InGameMotd,MultiMOTD,PPMOTD,ServerMotdManager,BetterMOTD,PowerMOTD".split(",")) {
                    Plugin p = Bukkit.getPluginManager().getPlugin(str);
                    if (p == null) {
                        continue;
                    }
                    Bukkit.getPluginManager().disablePlugin(p);
                }
            }
        }.runTask(protocolLib);
    }

    @Override
    public void saveConfig() {
        try {
            if(config != null){
                config.saveConfig();
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Can not save config");
            ex.printStackTrace();
        }
    }

    public static void severe(String msg) {
        System.err.println("**********************************************");
        System.err.println("\t");
        System.err.println(msg);
        System.err.println("\t");
        System.err.println("**********************************************");
        logger.log(Level.SEVERE, "插件将停止运行");
        Bukkit.getPluginManager().disablePlugin(plugin);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
    }

    public static Main getInstance() {
        return plugin;
    }

    @Override
    public void onDisable() {
        try {
            AttributionUtil.attributionTemp.clear();
        } catch (Throwable ex) {
            //ignore
        }

        try {
            MetricsLite.stopAll();
        } catch (Throwable ex) {
            //ignore
        }

        try {
            if (updater != null) {
                updater.cancel();
                updater = null;
            }
        } catch (Throwable ex) {
            //ignore
        }

        try {
            pm.removePacketListeners(this);
        } catch (Throwable ex) {
            //ignore
        }
    }

    public static void broadcast(String msg) {
        List<CommandSender> senders = new LinkedList<>();
        senders.add(Bukkit.getConsoleSender());
        for (Player player : ReflectFactory.getPlayers()) {
            if (player.isOp() || player.hasPermission("colormotd.update.warning")) {
                senders.add(player);
            }
        }
        for (CommandSender sender : senders) {
            for (String str : msg.split("\n")) {
                sender.sendMessage(msgPrefix + str);
            }
        }
    }

    public static Date taskSync(long interval) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.ROOT);
        final long t = interval;
        long curTime = cal.getTimeInMillis();
        long d = curTime / t;
        while (curTime % t != 0) {
            curTime = t * ++d;
            cal.setTimeInMillis(curTime);
        }
        return cal.getTime();
    }
}
