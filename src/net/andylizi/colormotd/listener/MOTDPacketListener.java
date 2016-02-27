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
package net.andylizi.colormotd.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;

import com.comphenix.protocol.events.ListenerOptions;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;

import com.comphenix.protocol.injector.GamePhase;

import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedServerPing;
import com.google.common.collect.ImmutableList;

import java.util.*;
import java.util.logging.Level;

import net.andylizi.colormotd.Main;
import static net.andylizi.colormotd.Main.*;
import static net.andylizi.colormotd.utils.VariableFiller.*;
import net.andylizi.colormotd.utils.ReflectFactory;

import org.bukkit.plugin.Plugin;

public final class MOTDPacketListener extends Object implements PacketListener {

    private Main plugin;
    private ProtocolManager pm;

    private final ListeningWhitelist receivingWhitelist;
    private final ListeningWhitelist sendingWhitelist;

    public MOTDPacketListener(Main plugin, ProtocolManager pm) throws Throwable {
        this.plugin = plugin;
        this.pm = pm;

        this.sendingWhitelist = ListeningWhitelist.newBuilder()
                .priority(ListenerPriority.NORMAL)
                .types(PacketType.Status.Server.OUT_SERVER_INFO)  //监听PacketStatusOutServerInfo包
                .gamePhase(GamePhase.LOGIN)
                .options(new ListenerOptions[]{ListenerOptions.ASYNC})
                .build();
        this.receivingWhitelist = ListeningWhitelist.newBuilder()
                .priority(ListenerPriority.NORMAL)
                .types(PacketType.Status.Client.IN_PING) //监听PacketStatusInPing包
                .gamePhase(GamePhase.LOGIN)
                .options(new ListenerOptions[]{ListenerOptions.ASYNC})
                .build();
    }

    @Override
    public synchronized void onPacketSending(PacketEvent event) {
        final String ip = event.getPlayer().getAddress().toString().split(":")[0].replace("/", "");
        if (event.getPacketType().equals(PacketType.Status.Server.OUT_SERVER_INFO)) {
            try {
                WrappedServerPing ping = null;

                try {
                    ping = event.getPacket().getServerPings().getValues().get(0);
                } catch (IndexOutOfBoundsException ex) {
                    return;
                }

                ping.setMotD(replace(randomMotd(config.motdList), ip));
                ping.setFavicon(randomIcon(config.icon));
                if (!config.showDelay) {
                    ping.setVersionProtocol(-1);
                    ping.setVersionName(replace(config.online, ip));
                }
                if (!config.playerList.isEmpty()) {
                    List<WrappedGameProfile> profileList = new ArrayList<>();
                    for (String str : config.playerList) {
                        str = replace(str, ip);
                        profileList.add(ReflectFactory.createGameProfile(str));
                    }
                    ping.setPlayers(ImmutableList.copyOf(profileList));
                    ping.setPlayersVisible(true);
                } else {
                    ping.setPlayersVisible(false);
                }
                event.getPacket().getServerPings().getValues().set(0, ping);
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "[{0}]在向客户端发送ping包时出现严重错误!ProtocolLib版本{1}", new Object[]{Main.getInstance().getDescription().getVersion(), protocolLibVersion});
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        if (!config.showDelay) {
            event.setCancelled(true);
            return;
        }
    }

    @Override
    public ListeningWhitelist getSendingWhitelist() {
        return this.sendingWhitelist;
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return this.receivingWhitelist;
    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }

    public void unRegister(Plugin plugin) {
        pm.removePacketListeners(plugin);
    }
}
