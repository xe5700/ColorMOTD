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

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import net.andylizi.colormotd.ColorMOTD;
import net.andylizi.colormotd.Updater;
import static net.andylizi.colormotd.bukkit.BukkitMain.*;
import static net.andylizi.colormotd.ColorMOTD.msgPrefix;

public final class CommandHandler extends Object implements CommandExecutor {

    private BukkitMain plugin;

    public CommandHandler(BukkitMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("cmotdr")) {
            Bukkit.dispatchCommand(sender, "colormotd reload");
            return true;
        } else if (command.getName().equalsIgnoreCase("smode")) {
            Bukkit.dispatchCommand(sender, "colormotd smode");
            return true;
        } else if (command.getName().equalsIgnoreCase("colormotd")) {
            if (!sender.isOp() && !sender.hasPermission("colormotd")) {
                if (args.length == 1 && args[0].equalsIgnoreCase("version")); else {
                    sender.sendMessage(msgPrefix + "§c你没有权限使用该命令哦!");
                    return true;
                }
            }
            if (args.length < 1) {
                sender.sendMessage(msgPrefix + "§c参数不足!可用的子命令: reload,update,smode,version");
                return true;
            }
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.isOp() && !sender.hasPermission("colormotd.reload")) {
                    sender.sendMessage(msgPrefix + "§c你没有权限使用该子命令哦!");
                    return true;
                }
                try {
                    logger.info("读取配置文件...");
                    config.loadConfig();
                    sender.sendMessage(msgPrefix + "§a配置已重载");
                } catch (Throwable ex) {
                    sender.sendMessage(msgPrefix + "§4§l在加载配置文件过程中发生严重错误,请检查你的配置文件!!!");
                    ex.printStackTrace();
                    sender.sendMessage(msgPrefix + "§4§l错误详细信息请查看后台");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("update")) {
                if (!sender.isOp() && !sender.hasPermission("colormotd.update")) {
                    sender.sendMessage(msgPrefix + "§c你没有权限使用该子命令哦!");
                    return true;
                }
                sender.sendMessage(msgPrefix + "§a检查ing...");
                new BukkitRunnable() {
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
                }.runTaskAsynchronously(plugin);
                return true;
            } else if (args[0].equalsIgnoreCase("version")) {
                sender.sendMessage("§5┌ §a" + plugin.getDescription().getFullName() + " §9made by andylizi");
                sender.sendMessage("§5| §dSupport by ProtocolLib " + BukkitMain.protocolLibVersion);
                sender.sendMessage("§5| §dRunning in " + Bukkit.getBukkitVersion());
                sender.sendMessage("§5| §dRuntime by " + Bukkit.getVersion());
                sender.sendMessage("§5| §dJVM by " + System.getProperty("java.version"));
                if (sender instanceof Player) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + sender.getName() + " {text:\"§d| \",extra:[{text:\"§6§n"+ColorMOTD.MCBBS_RELEASE_URL+"\",clickEvent:{action:open_url,value:\""+ColorMOTD.MCBBS_RELEASE_URL+"\"},hoverEvent:{action:show_text,value:\"§2点击在浏览器中打开链接\"}}]}");
                } else {
                    sender.sendMessage("§d| §ehttp://www.mcbbs.net/thread-448326-1-1.html");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("smode")) {
                if (!sender.isOp() && !sender.hasPermission("colormotd.smode")) {
                    sender.sendMessage(msgPrefix + "§c你没有权限使用该子命令哦!");
                    return true;
                }
                if (!config.smode) {
                    sender.sendMessage(msgPrefix + "§e维护模式已启动" + (essentials ? ",如踢出所有非OP的玩家,请使用/kickall" : ""));
                } else {
                    sender.sendMessage(msgPrefix + "§a维护模式已关闭");
                }
                config.updateSmode(config.smode = !config.smode);
                plugin.saveConfig();
                return true;
            } else {
                sender.sendMessage(msgPrefix + "§c没有这个子命令!可用的子命令: reload,update,smode,version");
                return true;
            }
        }
        return false;
    }
}
