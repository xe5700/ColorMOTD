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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

import net.andylizi.colormotd.Config;
import net.md_5.bungee.api.ChatColor;

public abstract class BasePlaceholderFiller {
    protected final Config config;
    protected final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    protected final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    protected String date;
    protected String time;
    protected Random random = new Random();
    
    public BasePlaceholderFiller(Config config){
        this.config = config;
        DATE_FORMAT.setTimeZone(TimeZone.getDefault());
        TIME_FORMAT.setTimeZone(TimeZone.getDefault());
    }
    
    public abstract Object randomIcon();

    public String randomMotd(List<String> motdList) {
        if (config.smode) {
            if (config.smodeMotd != null) {
                if (!config.smodeMotd.trim().isEmpty()) {
                    return config.smodeMotd;
                }
            }
        }
        return (motdList.isEmpty() ? "无MOTD" : (motdList.size() == 1 ? motdList.get(0) : motdList.get(random.nextInt(motdList.size()))));
    }

    public String getDate() {
        if(date == null){
            date = DATE_FORMAT.format(new Date());
        }
        return date;
    }

    public String getTime() {
        if(time == null){
            time = TIME_FORMAT.format(new Date());
        }
        return time;
    }

    public String fill(String str, String ip){
        str = str.replace("%TIME%", getTime());
        str = str.replace("%DATE%", getDate());
        str = str.replace("\\n", "\n");
        if(str.contains("%RANDOM%")){
            str = str.replace("%RANDOM%", String.valueOf(random.nextInt(10)));
        }
        if (str.contains("%LOC%") || str.contains("%ISP%")) {
            String result = AttributionUtil.getResult(ip);
            if (str.contains("%LOC%")) {
                str = str.replace("%LOC%", AttributionUtil.formatLocation(result));
            }
            if (str.contains("%ISP%")) {
                str = str.replace("%ISP%", AttributionUtil.formatISP(result));
            }
        }
        return ChatColor.translateAlternateColorCodes('&', str);
    }

    public void reflushTime(){
        Date date = new Date();
        this.time = TIME_FORMAT.format(date);
        this.date = DATE_FORMAT.format(date);
    }
}
