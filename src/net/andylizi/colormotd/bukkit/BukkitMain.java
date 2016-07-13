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
package net.andylizi.colormotd.bukkit;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.utility.MinecraftVersion;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.andylizi.colormotd.utils.*;
import net.andylizi.colormotd.ColorMOTD;
import net.andylizi.colormotd.Updater;
import net.andylizi.colormotd.bukkit.listener.JoinListener;
import net.andylizi.colormotd.bukkit.listener.MOTDPacketListener;
import net.andylizi.colormotd.bukkit.metrics.Metrics;
import net.andylizi.colormotd.bukkit.metrics.Metrics.Graph;
import net.andylizi.colormotd.bukkit.metrics.Metrics.Plotter;
import net.andylizi.colormotd.bukkit.utils.BungeeChannel;
import net.andylizi.colormotd.bukkit.utils.EssentialsHook;
import net.andylizi.colormotd.bukkit.utils.FakePlayersOnlineHook;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class BukkitMain extends JavaPlugin implements Plugin {
    public static ColorMOTD colormotd;
    public static BukkitMain plugin;
    public static Logger logger;

    protected static ProtocolManager pm;
    public static BukkitConfig config;

    public static String protocolLibVersion;
    public static EssentialsHook essHook;
    public static FakePlayersOnlineHook fakePlayersOnlineHook;

    public static boolean essentials;
    public static boolean fakePlayersOnline;

    public BukkitMain() throws AssertionError {
        for (StackTraceElement t : Thread.currentThread().getStackTrace()) {
            if (t.getClassName().equals("org.bukkit.plugin.java.PluginClassLoader")) {
                plugin = this;//单例
                return;
            }
        }
        throw new AssertionError("此类无法被实例化");
    }

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        logger = getLogger();

        //连接ProtocolLib
        Plugin protocolLib = Bukkit.getPluginManager().getPlugin("ProtocolLib");
        if (protocolLib != null && protocolLib.isEnabled()) {
            protocolLibVersion = protocolLib.getDescription().getVersion();
            logger.log(Level.INFO, "├使用 ProtocolLib {0} 作为前置", protocolLibVersion);

            pm = ProtocolLibrary.getProtocolManager();
            MinecraftVersion version = pm.getMinecraftVersion();
            if (version.compareTo(MinecraftVersion.WORLD_UPDATE) < 0) {
                colormotd.severe("此插件最低只能在1.7.2 (World Update)服务器上运行!");
            }
        } else {
            colormotd.severe("无法连接到ProtocolLib\n\t\t请确认您是否安装了ProtocolLib插件作为前置?");
            return;
        }

        Bukkit.getPluginManager().registerEvents(new JoinListener(), this);

        //加载配置文件
        try {
            config = new BukkitConfig(getLogger(), getDataFolder());
            config.loadConfig();
            logger.info("│├载入成功");
            colormotd = new BukkitColorMOTD(this, config);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "│├在加载配置文件过程中发生严重错误,请检查你的配置文件!!!");
            ex.printStackTrace();
            config.configLoadFailed();
        }

        //启动MetricsLite
        try {
            Metrics metrics = new Metrics(this);
            Graph graph = metrics.createGraph("RequestCount");
            graph.addPlotter(new Plotter("Request Count") {
                @Override
                public int getValue() {
                    return MOTDPacketListener.requestCounter;
                }
            });
            metrics.addGraph(graph);
            metrics.start();
        } catch (Exception t) {
            logger.log(Level.WARNING, "├启动Metrics失败: {0}", t);
        }

        essentials = (Bukkit.getPluginManager().getPlugin("Essentials") != null);
        fakePlayersOnline = (Bukkit.getPluginManager().getPlugin("FakePlayersOnline") != null);
        if (essentials || fakePlayersOnline) 
            logger.info("│├软前置加载...");
        if (essentials) {
            try {
                essHook = new EssentialsHook();
                logger.info("│├找到Essentials,建立连接...");
            } catch (UnsupportedOperationException ex) {
                logger.log(Level.SEVERE, "││├连接Essentials失败!将无法使用变量%TPS%和%STATE%!");
                essentials = false;
            }
        } else {
            logger.warning("├找不到Essentials,无法使用变量%TPS%和%STATE%!");
        }

        if (fakePlayersOnline) {
            try {
                (fakePlayersOnlineHook = new FakePlayersOnlineHook()).init();
                logger.info("│├找到FakePlayersOnline,建立连接...");
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "││├连接FakePlayersOnline失败!");
                ex.printStackTrace();
                fakePlayersOnline = false;
            }
        }

        //更新提示
        if (config.updateChecker) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                    colormotd.updater = new Updater(colormotd, getLogger(), 
                            new File(Bukkit.getUpdateFolderFile(), getFile().getName()), 
                    getDescription().getFullName() + "/" + Bukkit.getPort() + "/Updater/" + Updater.UPDATER_VERSION), 
                    ColorMOTD.taskSync(10L * 1000L) / 50L, 60L * 60L * 20L);
        } else {
            logger.info("├您在配置文件里禁止了更新提示,那记得经常去发布贴检查有没有新版本哦~");
        }

        //BungeeCord/RedisBungee
        try {
            if (config.useBungeeCord || config.useRedisBungee) {
                logger.log(Level.INFO, "├尝试建立与Bungee的连接...");
                new BungeeChannel(this);
            }
        } catch (Exception t) {
            logger.log(Level.SEVERE, "│├连接Bungee失败!无法获取Bungee服务器真实在线人数");
            t.printStackTrace();
            config.useBungeeCord = config.useRedisBungee = false;
        } finally {
            //添加MOTD监听器
            try {
                logger.info("├正在注册MOTD监听器...");
                pm.addPacketListener(new MOTDPacketListener(this, colormotd.placeholderFiller, pm));
            } catch (Error e) {
                colormotd.severe("在试图注册监听器过程中出现严重错误,此插件可能不支持服务器上的ProtocolLib版本(" + protocolLibVersion + ")!");
                e.printStackTrace();
            } catch (Exception e) {
                colormotd.severe("在注册监听器过程中出现严重错误!ProtocolLib版本" + protocolLibVersion);
                e.printStackTrace();
            }

            //注册命令
            CommandHandler commandExecutor = new CommandHandler(this);
            Bukkit.getPluginCommand("cmotdr").setExecutor(commandExecutor);
            Bukkit.getPluginCommand("smode").setExecutor(commandExecutor);
            Bukkit.getPluginCommand("colormotd").setExecutor(commandExecutor);
            System.out.println(this.getDescription().getFullName() + "加载完成,用时" + (System.currentTimeMillis() - startTime) + "毫秒");
        }

        //关闭已知MOTD插件列表中的所有冲突插件
        new BukkitRunnable() {
            @Override
            public void run() {
                for (String str : "ServerlistMOTD,PlayerCountMessage,MOTDColor,YuleMotd,AnimMOTD,SexyMotd,UniqueMOTD,InGameMotd,MultiMOTD,PPMOTD,ServerMotdManager,BetterMOTD,PowerMOTD".split(",")) {
                    Plugin p = Bukkit.getPluginManager().getPlugin(str);
                    if (p == null) continue;
                    Bukkit.getPluginManager().disablePlugin(p);
                }
            }
        }.runTask(Bukkit.getPluginManager().getPlugin("ProtocolLib"));
    }

    @Override
    public void saveConfig() {
        try {
            if (config != null) {
                config.saveConfig();
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "无法保存配置文件");
            ex.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        try {
            //清空归属地缓存
            AttributionUtil.attributionTemp.clear();
        } catch (Exception ex) {}

        try {
            if(colormotd != null)
                if (colormotd.updater != null) {
                    colormotd.updater.cancel();
                    colormotd.updater = null;
                }
        } catch (Exception ex) {}

        try {
            pm.removePacketListeners(this);
        } catch (Exception ex) {}
    }
}
