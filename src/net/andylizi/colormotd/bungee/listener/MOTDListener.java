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
package net.andylizi.colormotd.bungee.listener;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import net.andylizi.colormotd.bungee.utils.BungeePlaceholderFiller;
import static net.andylizi.colormotd.bungee.BungeeMain.config;

import net.md_5.bungee.api.Favicon;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.ServerPing.PlayerInfo;
import net.md_5.bungee.api.ServerPing.Players;
import net.md_5.bungee.api.ServerPing.Protocol;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class MOTDListener implements Listener{
    private static final PlayerInfo[] PLAYER_INFO_ARRAY = new PlayerInfo[0];
    
    private BungeePlaceholderFiller placeholderFiller;
    public static volatile int requestCounter;

    public MOTDListener(BungeePlaceholderFiller placeholderFiller) {
        this.placeholderFiller = placeholderFiller;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onServerListPing(ProxyPingEvent event){
        final String ip = event.getConnection().getAddress().getHostString();
        final ListenerInfo listenerInfo = event.getConnection().getListener();
        
        ServerPing response = event.getResponse() == null ? new ServerPing() : event.getResponse();
        response.setDescriptionComponent(TextComponent
                .fromLegacyText(placeholderFiller.fill(placeholderFiller.randomMotd(config.motdList), ip, listenerInfo))[0]);
        response.setFavicon((Favicon) placeholderFiller.randomIcon());
        if (!config.showDelay) {
            response.setVersion(new Protocol(placeholderFiller.fill(config.online, ip, listenerInfo), -1));
        }
        if (!config.playerList.isEmpty()) {
            response.setPlayers(new Players(0, ProxyServer.getInstance().getOnlineCount(), 
                    Lists.transform(config.playerList, new Function<String, PlayerInfo>() {
                @Override
                public PlayerInfo apply(String f) {
                    String str = placeholderFiller.fill(f, ip, listenerInfo);
                    return new PlayerInfo(str, UUID.nameUUIDFromBytes(str.getBytes(StandardCharsets.UTF_8)));
                }
            }).toArray(PLAYER_INFO_ARRAY)));
        }
        event.setResponse(response);
        requestCounter++;
    }
}
