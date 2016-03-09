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

import java.lang.reflect.*;
import org.bukkit.plugin.Plugin;

public class UpdaterInjection extends Object {

    public static void inject(Plugin protocolLib) throws ReflectiveOperationException {
        Class<?> updaterClass = injectDefaultUpdater(protocolLib);
        if(updaterClass == null){
            return;
        }else{
            if(updaterClass.getPackage().getName().equals("com.comphenix.protocol.metrics")){
                v3_4.injectStaticUpdater(updaterClass);
            }
        }
    }

    private static Class<?> injectDefaultUpdater(Plugin protocolLib) throws ReflectiveOperationException {
        Field field;
        try{
            field = protocolLib.getClass().getDeclaredField("updater");
        }catch(NoSuchFieldException ex){
            return null;
        }
        field.setAccessible(true);
        Object updater = field.get(protocolLib);
        Class<?> updaterClass = updater.getClass();
        while(!updaterClass.getName().endsWith(".Updater")){
            updaterClass = updaterClass.getSuperclass();
        }
        Class<?> updateTypeClass = Class.forName(updaterClass.getName() + "$UpdateType", false, updaterClass.getClassLoader());

        Field updateTypeField = updaterClass.getDeclaredField("type");
        updateTypeField.setAccessible(true);
        Enum[] es = (Enum[])updateTypeClass.getEnumConstants();
        for(Enum e:es){
            if(e.name().equalsIgnoreCase("NO_VERSION_CHECK")){
                updateTypeField.set(updater, e);
                break;
            }
        }
        field.set(protocolLib, updater);

        return updaterClass;
    }

    private static class v3_4 {
        private static void injectStaticUpdater(Class<?> updaterClass) throws ReflectiveOperationException {
            Field field = updaterClass.getDeclaredField("NO_UPDATE_TAG");
            field.setAccessible(true);
            Field fieldModifier = field.getClass().getDeclaredField("modifiers");
            fieldModifier.setAccessible(true);
            fieldModifier.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            try {
                field.set(null, new String[]{".", ""});
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                throw ex;
            }
        }
    }
}
