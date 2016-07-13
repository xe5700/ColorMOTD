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

import java.lang.reflect.Method;
import net.ess3.api.IEssentials;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class EssentialsHook extends Object{
    private static IEssentials ess = null;
    public static Method getTPS;

    public boolean init() throws UnsupportedOperationException{
        if(ess == null){
            Plugin essPlugin = Bukkit.getPluginManager().getPlugin("Essentials");
            if(essPlugin == null){
                try{
                    return false;
                }finally{
                    throw new UnsupportedOperationException("找不到Essentials插件!");
                }
            }
            ess = (IEssentials) essPlugin;
            return true;
        }else{
            return true;
        }
    }
    public double getTPS(){
        init();
        return ess.getTimer().getAverageTPS();
    }
}
