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

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.andylizi.colormotd.Config;
import net.andylizi.colormotd.utils.AttributionUtil;

import net.md_5.bungee.api.Favicon;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class BungeeConfig extends Config{
    private final ConfigurationProvider provider;
    private Configuration config;
    private Configuration defaults;
    public List<Favicon> favicons;
    public Favicon faviconSmode;
    
    public BungeeConfig(Logger logger, File dataFolder) {
        super(logger, dataFolder);
        this.provider = ConfigurationProvider
                .getProvider(YamlConfiguration.class);
        this.defaults = new Configuration();
        defaults.set("Motd", Arrays.asList(
                new String[]{
                    "&b感谢使用ColorMOTD - 这是第一条默认消息\\n&d现在时间: &e%DATE% %TIME%", 
                    "&b欢迎使用ColorMOTD - 这是第二条默认消息\\n&d在线人数: &e%ONLINE%", 
                    "&b感谢使用ColorMOTD - 这是第三条默认消息\\n&d您的位置: &e%LOC%&d,服务商: &e%ISP%"
                }));
        defaults.set("OnlineMsg", "&a在线人数 &6%ONLINE%&7/&6%MAXPLAYER%");
        defaults.set("Players", Arrays.asList(
                new String[]{
                    "&b这里是默认的悬浮文字信息", 
                    "&e可以显示多行", 
                    "&a支持变量,比如%TIME%"
                }));
        defaults.set("ServiceModeMOTD", "&c服务器维护中,请等待维护完成...");
        defaults.set("ServiceModeKickCause", "&c服务器维护中,请等待维护完成再进入服务器!");
        defaults.set("AttributionServer", "taobao");
        defaults.set("showDelay", false);
        defaults.set("UpdateChecker", true);
    }
    
    @Override
    public void loadConfig() throws Exception {
        logger.info("│├正在载入配置文件..");
        AttributionUtil.attributionTemp.clear();
        reloadConfig();
        motdList = config.getStringList("Motd");
        online = config.getString("OnlineMsg");
        playerList = config.getStringList("Players");
        smodeMotd = config.getString("ServiceModeMOTD");
        smodeKickCause = config.getString("ServiceModeKickCause");
        try {
            AttributionUtil.attributionServer = AttributionUtil.AttributionServer.valueOf(config.getString("AttributionServer").toUpperCase());
        } catch (IllegalArgumentException ex) {
            logger.log(Level.WARNING, "│├您指定的归属地服务器\"{0}\"不存在,请检查你的配置文件中的\"AttributionServer\"项,可用的值为: {1} 将使用默认值\"IP138\"", new Object[]{config.getString("AttributionServer"), AttributionUtil.AttributionServer.list()});
            AttributionUtil.attributionServer = AttributionUtil.AttributionServer.IP138;
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
        icon = loadIcons();
    }
    
    @Override
    public List<BufferedImage> loadIcons() throws IOException {
        List<BufferedImage> icons = super.loadIcons();
        favicons = new ArrayList<>(icons.size());
        for(BufferedImage image : icons)
            favicons.add(Favicon.create(image));
        if(smodeIcon != null)
            faviconSmode = Favicon.create(smodeIcon);
        return icons;
    }
    
    private void reloadConfig() throws IOException{
        if(!dataFolder.exists())
            dataFolder.mkdir();
        if(!configFile.exists())
            releaseFile(dataFolder, "/bungee-config.yml", "config.yml");
        try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8))) {
            config = provider.load(reader, defaults);
        }
    }

    @Override
    public void updateSmode(boolean smode) {
        config.set("ServiceMode", smode);
    }

    @Override
    public void saveConfig() throws IOException {
        try (Writer writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8))) {
            provider.save(config, writer);
        }
    }
    
}
