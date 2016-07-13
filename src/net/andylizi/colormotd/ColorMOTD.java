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
package net.andylizi.colormotd;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Logger;

import net.andylizi.colormotd.utils.BasePlaceholderFiller;

public abstract class ColorMOTD {
    public static final int buildVersion = 26;
    public static final String msgPrefix = "§6[§aColorMOTD§6]§r ";
    public static final String MCBBS_RELEASE_URL = "http://www.mcbbs.net/thread-448326-1-1.html";

    public BasePlaceholderFiller placeholderFiller;
    public Updater updater;
    public Logger logger;

    public ColorMOTD(BasePlaceholderFiller placeholderFiller, Logger logger) {
        this.placeholderFiller = placeholderFiller;
        this.logger = logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void severe(String msg) {
        System.err.println("**********************************************");
        System.err.println("\t");
        System.err.println(msg);
        System.err.println("\t");
        System.err.println("**********************************************");
        if (logger != null) {
            logger.severe("插件将停止运行");
        } else {
            System.err.println("[ColorMOTD] 插件将停止运行");
        }
        disablePlugin();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {}
    }
    
    public abstract void broadcast(String msg);
    
    public static long taskSync(long interval) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.ENGLISH);
        final long old = cal.getTimeInMillis();
        final long t = interval;
        long curTime = old;
        long d = curTime / t;
        while (curTime % t != 0) {
            curTime = t * ++d;
            cal.setTimeInMillis(curTime);
        }
        return cal.getTimeInMillis() - old;
    }
    
    public abstract void disablePlugin();
}
