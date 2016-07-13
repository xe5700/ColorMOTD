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
package net.andylizi.colormotd.bungee;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.andylizi.colormotd.ColorMOTD;
import net.andylizi.colormotd.bungee.listener.JoinListener;
import net.andylizi.colormotd.bungee.listener.MOTDListener;
import net.andylizi.colormotd.bungee.metrics.Metrics;
import net.andylizi.colormotd.bungee.metrics.Metrics.Graph;
import net.andylizi.colormotd.bungee.metrics.Metrics.Plotter;
import net.andylizi.colormotd.bungee.utils.BungeePlaceholderFiller;
import net.andylizi.colormotd.Updater;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

public class BungeeMain extends Plugin{
    public static BungeeColorMOTD colormotd;
    public static BungeeMain plugin;
    public static Logger logger;
    public static BungeeConfig config;

    public BungeeMain() {
        plugin = this;
    }
    
    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        logger = getLogger();
        logger.info(getDescription().getName() + " v" + getDescription().getVersion() +" 开始加载...");
        try{
            config = new BungeeConfig(getLogger(), getDataFolder());
            config.loadConfig();
            colormotd = new BungeeColorMOTD(this, config);
            ProxyServer.getInstance().getScheduler().schedule(this, new Runnable() {
                int i = 0;
                @Override
                public void run() {
                    colormotd.placeholderFiller.reflushTime();
                    if(++i % 60 == 0){
                        i = 0;
                        MOTDListener.requestCounter = 0;
                    }
                }
            }, 1, 1, TimeUnit.SECONDS);
        }catch(Exception ex){
            logger.log(Level.SEVERE, "│├在加载配置文件过程中发生严重错误,请检查你的配置文件!!!");
            ex.printStackTrace();
            config.configLoadFailed();
        }
        
        //更新提示
        if (config.updateChecker) {
            getProxy().getScheduler().schedule(this, colormotd.updater = new Updater(colormotd, getLogger(), null, "Bungee" +
                            getDescription().getName() + " " + getDescription().getVersion() + 
                    getProxy().getConfig().getListeners().iterator().next().getQueryPort() + "/Updater/" + Updater.UPDATER_VERSION), 
                    ColorMOTD.taskSync(10L * 1000L), 60L * 60L * 1000L,
                    TimeUnit.MILLISECONDS);
        } else {
            logger.info("├您在配置文件里禁止了更新提示,那记得经常去发布贴检查有没有新版本哦~");
        }
        
        logger.info("├正在注册MOTD监听器...");
        getProxy().getPluginManager().registerListener(this, new JoinListener());
        getProxy().getPluginManager().registerListener(this, new MOTDListener((BungeePlaceholderFiller) colormotd.placeholderFiller));
        getProxy().getPluginManager().registerCommand(this, new CommandHandler(this));
        
        try{
            Metrics metrics = new Metrics(this);
            Graph graph = metrics.createGraph("RequestCount");
            graph.addPlotter(new Plotter("Request Count") {
                @Override
                public int getValue() {
                    return MOTDListener.requestCounter;
                }
            });
            metrics.addGraph(graph);
            metrics.start();
        }catch(Exception ex){
            logger.log(Level.WARNING, "├启动Metrics失败: {0}", ex);
        }
        
        logger.info(this.getDescription().getName() + " v" + 
                this.getDescription().getVersion() + "加载完成,用时" + (System.currentTimeMillis() - startTime) + "毫秒");
    }

    @Override
    public void onDisable() {
        try {
            if(colormotd != null)
                if (colormotd.updater != null) {
                    colormotd.updater.cancel();
                    colormotd.updater = null;
                }
        } catch (Exception ex) {}
    }
    
}
