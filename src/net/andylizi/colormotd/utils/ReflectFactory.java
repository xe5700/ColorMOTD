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
package net.andylizi.colormotd.utils;

import com.comphenix.protocol.wrappers.WrappedGameProfile;

import java.lang.reflect.*;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;

public final class ReflectFactory extends Object {
    public static WrappedGameProfile createGameProfile(String name) throws Throwable {
        //use ProtocolLib
        return new WrappedGameProfile(UUID.nameUUIDFromBytes(name.getBytes("UTF-8")), name);
    }

    public static Player[] getPlayers() {
        try {
            Method onlinePlayerMethod = Server.class.getMethod("getOnlinePlayers");
            if (onlinePlayerMethod.getReturnType().equals(Collection.class)) {
                return ((List<? extends Player>) onlinePlayerMethod.invoke(Bukkit.getServer())).toArray(new Player[0]);
            } else {
                return (Player[]) onlinePlayerMethod.invoke(Bukkit.getServer());
            }
        } catch (IndexOutOfBoundsException ex) {
            return new Player[0];
        } catch (ReflectiveOperationException ex) {
            ex.printStackTrace();
        }
        return new Player[0];
    }
}
