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

import net.andylizi.colormotd.ColorMOTD;
import net.andylizi.colormotd.bungee.utils.BungeePlaceholderFiller;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;

public class BungeeColorMOTD extends ColorMOTD{
    public BungeeColorMOTD(BungeeMain plugin, BungeeConfig config) {
        super(new BungeePlaceholderFiller(config), plugin.getLogger());
    }
    
    @Override
    public void broadcast(String msg) {
        for(String m : 
                (msg.startsWith("\n") ? msg : msgPrefix + msg).replace("\n", "\n" + msgPrefix).split("\n")){
            ProxyServer.getInstance().getConsole().sendMessage(TextComponent.fromLegacyText(m));
        }
    }

    @Override
    public void disablePlugin() {}
}
