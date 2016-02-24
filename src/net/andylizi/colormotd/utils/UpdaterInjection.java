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

public final class UpdaterInjection extends Object{
    public static void inject(Object protocolLib) throws ReflectiveOperationException{
        injectStaticUpdater(injectDefaultUpdater(protocolLib));
    }
    private static Class<?> injectDefaultUpdater(Object protocolLib) throws ReflectiveOperationException{
        Field field = protocolLib.getClass().getDeclaredField("updater");
        field.setAccessible(true);
        Object updater = field.get(protocolLib);
        Class<?> updaterClass = updater.getClass();
        Class<?> updateTypeClass = Class.forName(updaterClass.getName()+"$UpdateType", false, updaterClass.getClassLoader());
        
        Field updateTypeField = updaterClass.getDeclaredField("type");
        updateTypeField.setAccessible(true);
        updateTypeField.set(updater, updateTypeClass.getEnumConstants()[1]);
        
        return updaterClass;
    }
    private static void injectStaticUpdater(Class<?> updaterClass) throws ReflectiveOperationException{
        Field field = updaterClass.getDeclaredField("NO_UPDATE_TAG");
        field.setAccessible(true);
        Field fieldModifier = field.getClass().getDeclaredField("modifiers");
        fieldModifier.setAccessible(true);
        fieldModifier.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        try {
            field.set(null, new String[]{".","","-DEV","-PRE","-SNAPSHOT"});
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw ex;
        }
    }
}
