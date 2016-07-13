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

import java.io.IOException;
import java.util.logging.Logger;

import net.andylizi.colormotd.ColorMOTD;
import net.andylizi.colormotd.Updater;
import static net.andylizi.colormotd.ColorMOTD.msgPrefix;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class CommandHandler extends Command{
    private final BungeeMain plugin;
    private final Logger logger;
    private final BungeeConfig config;
    private final ColorMOTD colormotd;
    
    public CommandHandler(BungeeMain plugin) {
        super("colormotd", "colormotd.*");
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.config = plugin.config;
        this.colormotd = plugin.colormotd;
    }
    
    @Override
    public void execute(final CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(msgPrefix + "§c参数不足!可用的子命令: reload,update,smode,version");
            return;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("colormotd.reload")) {
                sender.sendMessage(msgPrefix + "§c你没有权限使用该命令哦!");
                return;
            }
            try {
                logger.info("读取配置文件...");
                config.loadConfig();
                sender.sendMessage(msgPrefix + "§a配置已重载");
            } catch (Exception ex) {
                sender.sendMessage(msgPrefix + "§4§l在加载配置文件过程中发生严重错误,请检查你的配置文件!!!");
                ex.printStackTrace();
                sender.sendMessage(msgPrefix + "§4§l错误详细信息请查看后台");
            }
            return;
        } else if (args[0].equalsIgnoreCase("update")) {
            if (!sender.hasPermission("colormotd.update")) {
                sender.sendMessage(msgPrefix + "§c你没有权限使用该命令哦!");
                return;
            }
            sender.sendMessage(msgPrefix + "§a检查ing...");
            ProxyServer.getInstance().getScheduler().runAsync(plugin, new Runnable() {
                @Override
                public void run() {
                    colormotd.updater.run();
                    if (colormotd.updater.exception != null) {
                        sender.sendMessage(msgPrefix + "§c检查更新出错: " + colormotd.updater.exception);
                        if (!Updater.DEBUG) {
                            colormotd.updater.exception.printStackTrace();
                        }
                        colormotd.updater.exception = null;
                    }
                    if (colormotd.updater.newVersion == null) {
                        sender.sendMessage(msgPrefix + "§a您当前已经是最新版本了哦!");
                    }
                }
            });
            return;
        } else if (args[0].equalsIgnoreCase("version")) {
            sender.sendMessage("§5┌ §a" + plugin.getDescription().getName() + " " + 
                    plugin.getDescription().getVersion() + " §9made by andylizi");
            sender.sendMessage("§5| §dRunning in " + plugin.getProxy().getVersion());
            sender.sendMessage("§5| §dJVM by " + System.getProperty("java.version"));
            if (sender instanceof ProxiedPlayer) {
                sender.sendMessage(new ComponentBuilder("| ")
                        .color(ChatColor.DARK_PURPLE)
                        .append(ColorMOTD.MCBBS_RELEASE_URL)
                            .color(ChatColor.YELLOW)
                            .underlined(true)
                            .event(new ClickEvent(Action.OPEN_URL, ColorMOTD.MCBBS_RELEASE_URL))
                            .event(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("点击在浏览器中打开链接")))
                        .create());
            } else {
                sender.sendMessage("§5| §e"+ColorMOTD.MCBBS_RELEASE_URL);
            }
            return;
        } else if (args[0].equalsIgnoreCase("smode")) {
            if (!sender.hasPermission("colormotd.smode")) {
                sender.sendMessage(msgPrefix + "§c你没有权限使用该命令哦!");
                return;
            }
            if (!config.smode) {
                sender.sendMessage(msgPrefix + "§e维护模式已启动");
            } else {
                sender.sendMessage(msgPrefix + "§a维护模式已关闭");
            }
            config.updateSmode(config.smode = !config.smode);
            try {
                config.saveConfig();
            } catch(IOException ex) {
                ex.printStackTrace();
            }
            return;
        } else {
            sender.sendMessage(msgPrefix + "§c没有这个子命令!可用的子命令: reload,update,smode,version");
            return;
        }
    }
}
