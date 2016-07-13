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
package net.andylizi.colormotd.bukkit;

import com.comphenix.protocol.wrappers.WrappedServerPing.CompressedImage;

import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.andylizi.colormotd.Config;
import net.andylizi.colormotd.ColorMOTD;
import net.andylizi.colormotd.utils.AttributionUtil;
import static net.andylizi.colormotd.bukkit.BukkitMain.plugin;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.LineBreak;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.InvalidConfigurationException;

public class BukkitConfig extends Config{
    public FileConfiguration config;
    public List<CompressedImage> compressedIcon;
    public CompressedImage compressedSmodeIcon;
    
    public BukkitConfig(Logger logger, File dataFolder) {
        super(logger, dataFolder);
    }
    
    @Override
    public void loadConfig() throws Exception {
        logger.info("│├正在载入配置文件..");
        AttributionUtil.attributionTemp.clear();
        try {
            String oldConfigStr;
            reloadConfig();
            oldConfigStr = config.saveToString();
            config.options().header(plugin.getDescription().getFullName() + " Config\r\n"+ColorMOTD.MCBBS_RELEASE_URL);
            config.addDefault("Motd", Arrays.asList(
                    new String[]{
                        "&b感谢使用ColorMOTD - 这是第一条默认消息\\n&d现在时间: &e%DATE% %TIME%", 
                        "&b欢迎使用ColorMOTD - 这是第二条默认消息\\n&d在线人数: &e%ONLINE%", 
                        "&b感谢使用ColorMOTD - 这是第三条默认消息\\n&d您的位置: &e%LOC%&d,服务商: &e%ISP%"
                    }));
            config.addDefault("OnlineMsg", "&a在线人数 &6%ONLINE%&7/&6%MAXPLAYER%");
            config.addDefault("Players", Arrays.asList(
                    new String[]{
                        "&b这里是默认的悬浮文字信息", 
                        "&e可以显示多行", 
                        "&a支持变量,比如%TIME%"}));
            config.addDefault("ServiceModeMOTD", "&c服务器维护中,请等待维护完成...");
            config.addDefault("ServiceModeKickCause", "&c服务器维护中,请等待维护完成再进入服务器!");
            config.addDefault("useBungeeCord", false);
            config.addDefault("redisBungee", false);
            config.addDefault("AttributionServer", "taobao");
            config.addDefault("TPSFormat", "0.0");
            config.addDefault("showDelay", false);
            config.addDefault("UpdateChecker", config.getBoolean("Auto-Update", true));

            config.options().copyDefaults(true);
            config.options().copyHeader();

            motdList = config.getStringList("Motd");
            online = config.getString("OnlineMsg");
            playerList = config.getStringList("Players");
            smodeMotd = config.getString("ServiceModeMOTD");
            smodeKickCause = config.getString("ServiceModeKickCause");
            useBungeeCord = config.getBoolean("useBungeeCord");
            useRedisBungee = config.getBoolean("redisBungee");
            try {
                AttributionUtil.attributionServer = AttributionUtil.AttributionServer.valueOf(config.getString("AttributionServer").toUpperCase());
            } catch (IllegalArgumentException ex) {
                logger.log(Level.WARNING, "│├您指定的归属地服务器\"{0}\"不存在,请检查你的配置文件中的\"AttributionServer\"项,可用的值为: {1} 将使用默认值\"TAOBAO\"", 
                        new Object[]{config.getString("AttributionServer"), AttributionUtil.AttributionServer.list()});
                AttributionUtil.attributionServer = AttributionUtil.AttributionServer.TAOBAO;
            }
            try {
                decimalFormater = new DecimalFormat(config.getString("TPSFormat"));
            } catch (IllegalArgumentException ex) {
                logger.log(Level.WARNING, "│├您指定的TPS显示格式\"{0}\"不是一个合法的格式,请检查你的配置文件中的\"TPSFormat\"项.正确的梨子:\"0.00\"代表显示两位小数比如19.73 .将使用默认值\"0.0\"", new Object[]{config.getString("TPSFormat")});
                decimalFormater = new DecimalFormat("0.0");
            }
            showDelay = config.getBoolean("showDelay");
            updateChecker = config.getBoolean("UpdateChecker");
            smode = config.getBoolean("ServiceMode");
            config.set("DefenseMode", null);

            String configString = config.saveToString();
            if (!oldConfigStr.equals(configString)) {
                saveConfig(configString);
            }
        } catch (InvalidConfigurationException ex) {
            Throwable cause = ex;
            while (cause.getCause() != null) {
                cause = ex.getCause();
            }
            if(cause instanceof Exception)
                throw (Exception) cause;
            else throw new Exception(cause);
        }
        icon = loadIcons();
        loadStateFormater();
    }

    @Override
    public List<BufferedImage> loadIcons() throws IOException {
        List<BufferedImage> icons = super.loadIcons();
        compressedIcon = new ArrayList<>(icons.size());
        for(BufferedImage image : icons)
            compressedIcon.add(CompressedImage.fromPng(image));
        if(smodeIcon != null)
            compressedSmodeIcon = CompressedImage.fromPng(smodeIcon);
        return icons;
    }

    @Override
    public void saveConfig() throws IOException {
        saveConfig(config.saveToString());
    }
    
    private void reloadConfig() throws IOException, InvalidConfigurationException {
        config = new YamlConfiguration();
        if (configFile.exists()) {
            config.loadFromString(readConfigString());
        }
        DumperOptions yamlOptions = null;
        try {
            Field f = YamlConfiguration.class.getDeclaredField("yamlOptions");
            f.setAccessible(true);

            yamlOptions = new DumperOptions() {
                {
                    setLineBreak(LineBreak.getPlatformLineBreak());
                }

                @Override
                public void setAllowUnicode(boolean allowUnicode) {
                    super.setAllowUnicode(false);
                }

                @Override
                public void setLineBreak(DumperOptions.LineBreak lineBreak) {
                    super.setLineBreak(LineBreak.getPlatformLineBreak());
                }
            };
            f.set(config, yamlOptions);
        } catch (ReflectiveOperationException ex) {}
    }

    @Override
    public void updateSmode(boolean smode) {
        config.set("ServiceMode", smode);
    }
}
