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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.logging.Level;

import net.andylizi.colormotd.bukkit.BukkitMain;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.messaging.PluginMessageListener;

public final class BungeeChannel extends BukkitRunnable implements PluginMessageListener {
    private static BungeeChannel instance;
    private static boolean state = false;
    private static byte[] askAllMsg;
    public static int online = 0;
    
    private Plugin plugin;

    public BungeeChannel(Plugin plugin) {
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, "RedisBungee");
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, "BungeeCord", this);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, "RedisBungee", this);
        runTaskTimer(plugin, 3 * 20L, 3 * 20L);
        instance = this;
        this.plugin = plugin;
    }

    public static BungeeChannel getInstance() {
        return instance;
    }

    @Override
    public synchronized void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!(channel.equals("BungeeCord") || channel.equals("RedisBungee"))) {
            return;
        }
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
        try {
            String subChannel = in.readUTF();
            if (subChannel.equals("PlayerCount")) {
                String server = in.readUTF();
                if (!server.equals("ALL")) {
                    return;
                }
                online = in.readInt();
            }
        } catch (EOFException e) {
        } catch (IOException e) {
            BukkitMain.logger.log(Level.SEVERE, "在试图接收Bungee在线人数过程中出现错误:");
            e.printStackTrace();
        }
    }

    public void askPlayerCount() {
        Player[] players = ReflectFactory.getPlayers();
        if (players.length > 0) {
            if(askAllMsg == null){
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(b);
                try {
                    out.writeUTF("PlayerCount");
                    out.writeUTF("ALL");
                } catch (IOException e) {
                    BukkitMain.logger.log(Level.SEVERE, "你肯定是中奖了");
                    e.printStackTrace();
                }
                askAllMsg = b.toByteArray();
            }
            players[0].sendPluginMessage(plugin, BukkitMain.config.useRedisBungee ? "RedisBungee" : "BungeeCord", askAllMsg);
        } else {
            if (state) {
                online = 0;
            }
            state = !state;
        }
    }

    @Override
    public void run() {
        askPlayerCount();
    }
}
