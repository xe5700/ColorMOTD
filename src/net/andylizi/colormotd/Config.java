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
package net.andylizi.colormotd;

import com.comphenix.protocol.wrappers.WrappedServerPing;
import java.awt.image.*;

import java.io.*;
import java.lang.reflect.Field;
import java.text.DecimalFormat;

import java.util.*;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import static net.andylizi.colormotd.Main.logger;
import net.andylizi.colormotd.utils.AttributionUtil;
import net.andylizi.colormotd.utils.StateFormater;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.yaml.snakeyaml.DumperOptions;

public final class Config extends Object {

    private final File dataFolder;
    private final File configFile;
    public FileConfiguration config;

    public List<String> motdList;
    public String online;
    public List<String> playerList;
    public List<WrappedServerPing.CompressedImage> icon;
    public WrappedServerPing.CompressedImage smodeIcon;
    public AttributionUtil.AttributionServer attributionServer;
    public DecimalFormat decimalFormater;
    public boolean showDelay;
    public boolean useBungeeCord;
    public boolean useRedisBungee;
    public boolean autoUpdate;
    public String smodeMotd;
    public String smodeKickCause;

    public Config(File dataFolder) {
        this.dataFolder = dataFolder;
        this.configFile = new File(dataFolder, "config.yml");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public void loadConfig() throws Throwable {
        logger.info("└正在加载配置文件..");
        AttributionUtil.attributionTemp.clear();
        try {
            String oldConfigStr;
            reloadConfig();
            oldConfigStr = config.saveToString();
            config.options().header(Main.getInstance().getDescription().getFullName() + " Config\r\nhttp://www.mcbbs.net/thread-448326-1-1.html");
            config.addDefault("Motd", Arrays.asList(new String[]{"&b感谢使用ColorMOTD - 这是第一条默认消息\\n&d现在时间: &e%DATE% %TIME%", "&b欢迎使用ColorMOTD - 这是第二条默认消息\\n&d在线人数: &e%ONLINE%", "&b感谢使用ColorMOTD - 这是第三条默认消息\\n&d您的位置: &e%LOC%&d,服务商: &e%ISP%"}));
            config.addDefault("OnlineMsg", "&a在线人数: &b%ONLINE%&d/&2%MAXPLAYER%");
            config.addDefault("Players", Arrays.asList(new String[]{"&b这里是默认的悬浮文字信息", "&e可以显示多行", "&a支持变量,比如%TIME%"}));
            config.addDefault("ServiceModeMOTD", "&c服务器维护中,请等待维护完成...");
            config.addDefault("ServiceModeKickCause", "&c服务器维护中,请等待维护完成再进入服务器!");
            config.addDefault("useBungeeCord", false);
            config.addDefault("redisBungee", false);
            config.addDefault("AttributionServer", "ip138");
            config.addDefault("TPSFormat", "0.0");
            config.addDefault("showDelay", false);
            config.addDefault("Auto-Update", true);

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
                attributionServer = AttributionUtil.AttributionServer.valueOf(config.getString("AttributionServer").toUpperCase());
            } catch (IllegalArgumentException ex) {
                logger.log(Level.WARNING, "├您指定的归属地服务器\"{0}\"不存在,请检查你的配置文件中的\"AttributionServer\"项,可用的值为: {1} 将使用默认值\"IP138\"", new Object[]{config.getString("AttributionServer"), AttributionUtil.AttributionServer.list()});
                attributionServer = AttributionUtil.AttributionServer.IP138;
            }
            try {
                decimalFormater = new DecimalFormat(config.getString("TPSFormat"));
            } catch (IllegalArgumentException ex) {
                logger.log(Level.WARNING, "├您指定的TPS显示格式\"{0}\"不是一个合法的格式,请检查你的配置文件中的\"TPSFormat\"项.正确的梨子:\"0.00\"代表显示两位小数比如19.73 .将使用默认值\"0.0\"", new Object[]{config.getString("TPSFormat")});
                decimalFormater = new DecimalFormat("0.0");
            }
            showDelay = config.getBoolean("showDelay");
            autoUpdate = config.getBoolean("Auto-Update");
            Main.smode = config.getBoolean("ServiceMode");

            config.set("DefenseMode", null);

            if (!oldConfigStr.equals(config.saveToString())) {
                saveConfig();
            }
        } catch (InvalidConfigurationException ex) {
            Throwable cause = ex;
            while (cause.getCause() != null) {
                cause = ex.getCause();
            }
            throw cause;
        }
        icon = loadIcons();

        loadStateFormater();
    }

    public void loadStateFormater() {
        File file = new File(dataFolder, "formater.js");
        if (!file.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(Main.class.getResourceAsStream("/res/default/formater.js"), "UTF-8"))) {
                try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
                    String buffer = null;
                    while ((buffer = reader.readLine()) != null) {
                        writer.append(buffer).append("\r\n");
                    }
                }
            } catch (IOException ex) {
                logger.severe("├在加载TPSFormater时出错,请检查\"formater.js\",将无法使用%STATE%标签!");
                ex.printStackTrace();
            }
        }
        try {
            Main.stateFormater = new StateFormater(file);
        } catch (IOException ex) {
            logger.severe("├在加载TPSFormater时出错,请检查\"formater.js\",将无法使用%STATE%标签!");
            ex.printStackTrace();
        }
    }

    public List<WrappedServerPing.CompressedImage> loadIcons() throws IOException {
        logger.info("   └正在加载图标...");
        List<WrappedServerPing.CompressedImage> iconList = new ArrayList<>();
        if (dataFolder.list().length == 1) {
            releaseFile(dataFolder, "/res/default/1.png");
            releaseFile(dataFolder, "/res/default/2.png");
            releaseFile(dataFolder, "/res/default/3.png");
            releaseFile(dataFolder, "/res/default/serviceModeIcon.png");
        }
        for (File file : dataFolder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (!pathname.isFile()) {
                    return false;
                }
                String fileName = pathname.getName().toLowerCase();
                return (fileName.endsWith(".png")
                        || fileName.endsWith(".jpg")
                        || fileName.endsWith(".gif")
                        || fileName.endsWith(".ico")
                        || fileName.endsWith(".bmp"));
            }
        })) {
            String fileName = file.getName().toLowerCase();
            BufferedImage image = ImageIO.read(file);
            if(image == null) continue;
            if (!fileName.endsWith(".png")) {
                logger.log(Level.WARNING, "    ├图标\"{0}\"格式不正确,正在进行格式转换...", file.getName());
                File newFile = new File(file.getParentFile(),
                        fileName.replace(".jpg", ".png")
                        .replace(".gif", ".png")
                        .replace(".ico", ".png")
                        .replace(".bmp", ".png"));
                file.delete();
                file = conversion(image, newFile);
                fileName = newFile.getName().toLowerCase();
            }
            if (!(image.getWidth() <= 64 && image.getHeight() <= 64)) {
                logger.log(Level.WARNING, "    ├无法加载\"{0}\",因为服务器图标的尺寸不能大于64*64!", file.getName());
                continue;
            }
            if (fileName.equalsIgnoreCase("serviceModeIcon.png")) {
                smodeIcon = WrappedServerPing.CompressedImage.fromPng(image);
                logger.info("    ├成功加载维护模式下的图标");
                continue;
            }
            iconList.add(WrappedServerPing.CompressedImage.fromPng(image));
            logger.log(Level.INFO, "    ├成功加载图标\"{0}\"", file.getName());
        }
        if (smodeIcon == null) {
            logger.info("    ├找不到维护模式下的图标\"serviceModeIcon.png\",将使用正常状态下的图标");
        }
        return iconList;
    }

    public File conversion(BufferedImage image, File toFile) throws IOException {
        ImageIO.write(image, "png", toFile);
        return toFile;
    }

    public void configLoadFailed() {
        motdList = Arrays.asList(new String[]{"§4§l配置文件加载失败!!请检查你的配置文件!!!\n§6§l配置文件加载失败!!请检查你的配置文件!!!"});
        online = "§4§lERROR";
        playerList = Arrays.asList(new String[]{"§4配置文件出错"});
        icon = new ArrayList<>();
        try {
            icon.add(WrappedServerPing.CompressedImage.fromPng(ImageIO.read(Config.class.getResourceAsStream("/res/default/error.png"))));
        } catch (Exception ex) {
        }
    }

    public static void releaseFile(File folder, String fileName) throws IOException {
        try (InputStream input = Config.class.getResourceAsStream(fileName)) {
            if (input == null) {
                return;
            }
            File toFile = new File(folder, new File(fileName).getName());
            try (BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(toFile),1024)) {
                byte buffer[] = new byte[1024];
                int size = 0;
                while ((size = input.read(buffer)) != -1) {
                    output.write(buffer, 0, size);
                    output.flush();
                }
            }
        }
    }

    private void reloadConfig() throws IOException, InvalidConfigurationException {
        config = new YamlConfiguration();
        if (configFile.exists()) {
            byte[] data = new byte[128];
            int size = -1;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream in = new FileInputStream(configFile)) {
                while((size = in.read(data)) != -1){
                    out.write(data,0,size);
                }
                data = out.toByteArray();
            }
            String str = new String(data, "UTF-8");
            if(str.contains(String.valueOf((char)65533))){
                logger.info("   ├配置文件编码为GBK,尝试转换为UTF-8...");
                str = new String(data, "GBK");
                saveConfig(str, "UTF-8");
            }else{
                logger.info("   ├配置文件编码为UTF-8...");
            }
            config.loadFromString(str);
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
        } catch (ReflectiveOperationException ex) {
        }
    }

    public void saveConfig() throws IOException {
        saveConfig(config.saveToString(), "UTF-8");
    }

    public void saveConfig(String content, String contentCharset) throws IOException {
        try (OutputStream output = new FileOutputStream(configFile)) {
            output.write(convert(content).getBytes("UTF-8"));
        }
    }

    private static String convert(String utfString) {
        StringBuilder sb = new StringBuilder();
        char[] chars = utfString.toCharArray();
        if (chars[0] != '\\') {
            sb.append(chars[0]);
        }

        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '\\' && chars.length - i >= 5 && chars[i + 1] == 'u') {
                sb.append((char) Integer.parseInt((utfString.substring(i + 2, i + 6)), 16));
                i += 5;
            } else {
                sb.append(chars[i]);
            }
        }

        return sb.toString().replace("\\\\n", "\\n");
    }
}
