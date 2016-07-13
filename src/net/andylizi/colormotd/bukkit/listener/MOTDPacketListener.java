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
package net.andylizi.colormotd.bukkit.listener;

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
import com.comphenix.protocol.wrappers.WrappedServerPing.CompressedImage;
import com.google.common.collect.ImmutableList;

import java.util.*;
import java.util.logging.Level;
import java.nio.charset.StandardCharsets;

import net.andylizi.colormotd.bukkit.BukkitMain;
import net.andylizi.colormotd.utils.BasePlaceholderFiller;
import static net.andylizi.colormotd.bukkit.utils.ReflectFactory.getPingPacketType;
import static net.andylizi.colormotd.bukkit.utils.ReflectFactory.getServerInfoPacketType;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class MOTDPacketListener extends Object implements PacketListener {
    private BukkitMain plugin;
    private BasePlaceholderFiller placeholderFiller;
    private ProtocolManager pm;

    private final ListeningWhitelist receivingWhitelist;
    private final ListeningWhitelist sendingWhitelist;
    private final PacketType PACKET_SERVER_INFO;
    
    public static volatile int requestCounter;

    public MOTDPacketListener(final BukkitMain plugin, BasePlaceholderFiller placeholderFiller, ProtocolManager pm) throws Exception {
        this.plugin = plugin;
        this.placeholderFiller = placeholderFiller;
        this.pm = pm;

        this.PACKET_SERVER_INFO = getServerInfoPacketType();
        this.sendingWhitelist = ListeningWhitelist.newBuilder()
                .priority(ListenerPriority.HIGH)
                .types(PACKET_SERVER_INFO)  //监听PacketStatusOutServerInfo包
                .gamePhase(GamePhase.LOGIN)
                .options(new ListenerOptions[]{ListenerOptions.ASYNC})
                .build();
        this.receivingWhitelist = ListeningWhitelist.newBuilder()
                .priority(ListenerPriority.HIGH)
                .types(getPingPacketType()) //监听PacketStatusInPing包
                .gamePhase(GamePhase.LOGIN)
                .options(new ListenerOptions[]{ListenerOptions.ASYNC})
                .build();
        
        new BukkitRunnable() {
            int i = 0;
            @Override
            public void run() {
                plugin.colormotd.placeholderFiller.reflushTime();
                if(++i % 60 == 0){
                    requestCounter = i = 0;
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20L, 20L);
    }

    @Override
    public synchronized void onPacketSending(PacketEvent event) {
        final String ip = event.getPlayer().getAddress().getHostString();
        if (event.getPacketType().equals(PACKET_SERVER_INFO)) {
            try {
                WrappedServerPing ping = null;

                try {
                    ping = event.getPacket().getServerPings().getValues().get(0);
                } catch (IndexOutOfBoundsException ex) {
                    return;
                }

                ping.setMotD(placeholderFiller.fill(placeholderFiller.randomMotd(plugin.config.motdList), ip));
                ping.setFavicon((CompressedImage) placeholderFiller.randomIcon());
                if (!plugin.config.showDelay) {
                    ping.setVersionProtocol(-1);
                    ping.setVersionName(placeholderFiller.fill(plugin.config.online, ip));
                }
                if (!plugin.config.playerList.isEmpty()) {
                    List<WrappedGameProfile> profileList = new ArrayList<>();
                    for (String str : plugin.config.playerList) {
                        profileList.add(createGameProfile(placeholderFiller.fill(str, ip)));
                    }
                    ping.setPlayers(ImmutableList.copyOf(profileList));
                    ping.setPlayersVisible(true);
                }
                event.getPacket().getServerPings().getValues().set(0, ping);
                requestCounter++;
            } catch (Throwable e) {
                plugin.logger.log(Level.SEVERE, "[{0}]在向客户端发送ping包时出现严重错误!ProtocolLib版本{1}", 
                        new Object[]{plugin.getDescription().getVersion(), plugin.protocolLibVersion});
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        if (!plugin.config.showDelay) {
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
    
    public static WrappedGameProfile createGameProfile(String name) throws Throwable {
        return new WrappedGameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name);
    }
}
