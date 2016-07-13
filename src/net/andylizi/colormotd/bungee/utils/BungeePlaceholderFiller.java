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
package net.andylizi.colormotd.bungee.utils;

import java.util.List;

import net.andylizi.colormotd.bungee.BungeeConfig;
import net.andylizi.colormotd.utils.BasePlaceholderFiller;

import net.md_5.bungee.api.Favicon;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;

public class BungeePlaceholderFiller extends BasePlaceholderFiller{
    private final BungeeConfig config;
    
    public BungeePlaceholderFiller(BungeeConfig config) {
        super(config);
        this.config = config;
    }
    
    @Override
    public Object randomIcon() {
        if (config.smode) {
            if (config.faviconSmode != null) {
                return config.faviconSmode;
            }
        }
        List<Favicon> iconList = this.config.favicons;
        return (iconList.isEmpty() ? null : (iconList.size() == 1 ? iconList.get(0) 
                : iconList.get(random.nextInt(iconList.size()))));
    }

    @Override
    public String fill(String str, String ip) {
        str = super.fill(str, ip);
        if (str.contains("%ONLINE%")) {
            str = str.replace("%ONLINE%", String.valueOf(ProxyServer.getInstance().getOnlineCount()));
        }
        return str;
    }
    
    public String fill(String str, String ip, ListenerInfo listenerInfo){
        return fill(str, ip).replace("%MAXPLAYER%", String.valueOf(listenerInfo.getMaxPlayers()));
    }
}
