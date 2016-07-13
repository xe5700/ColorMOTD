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
package net.andylizi.colormotd.bukkit;

import net.andylizi.colormotd.ColorMOTD;
import net.andylizi.colormotd.bukkit.utils.BukkitPlaceholderFiller;
import net.andylizi.colormotd.bukkit.utils.ReflectFactory;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class BukkitColorMOTD extends ColorMOTD{
    private final Plugin plugin;

    public BukkitColorMOTD(Plugin plugin, BukkitConfig config) {
        super(new BukkitPlaceholderFiller(config), plugin.getLogger());
        this.plugin = plugin;
    }

    @Override
    public void broadcast(String msg) {
        String[] msgs = (msg.startsWith("\n") ? msg : msgPrefix + msg).replace("\n", "\n" + msgPrefix).split("\n");
        Bukkit.getConsoleSender().sendMessage(msgs);
        for (CommandSender sender : ReflectFactory.getPlayers()) {
            if (sender.isOp() || sender.hasPermission("colormotd.update.warning")) {
                sender.sendMessage(msgs);
            }
        }
    }

    @Override
    public void disablePlugin() {
        if(plugin.isEnabled())
            Bukkit.getPluginManager().disablePlugin(plugin);
    }
}
