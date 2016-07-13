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

import java.awt.image.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

import net.andylizi.colormotd.utils.StateFormater;

public abstract class Config extends Object {
    protected final Logger logger;
    protected final File dataFolder;
    protected final File configFile;

    public boolean smode = false;
    public List<String> motdList;
    public String online;
    public List<String> playerList;
    public List<BufferedImage> icon;
    public BufferedImage smodeIcon;
    public DecimalFormat decimalFormater;
    public StateFormater stateFormater;
    public boolean showDelay;
    public boolean useBungeeCord;
    public boolean useRedisBungee;
    public boolean updateChecker;
    public String smodeMotd;
    public String smodeKickCause;

    public Config(Logger logger, File dataFolder) {
        this.logger = logger;
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

    public abstract void loadConfig() throws Exception;

    public void loadStateFormater() {
        File file = new File(dataFolder, "formater.js");
        if (!file.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass()
                    .getResourceAsStream("/res/default/formater.js"), "UTF-8"))) {
                try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
                    String buffer = null;
                    while ((buffer = reader.readLine()) != null) {
                        writer.append(buffer).append("\r\n");
                    }
                }
            } catch (IOException ex) {
                logger.severe("│├在加载TPSFormater时出错,请检查\"formater.js\",将无法使用%STATE%标签!");
                ex.printStackTrace();
            }
        }
        try {
            stateFormater = new StateFormater(file);
        } catch (IOException ex) {
            logger.severe("│├在加载TPSFormater时出错,请检查\"formater.js\",将无法使用%STATE%标签!");
            ex.printStackTrace();
        }
    }

    public List<BufferedImage> loadIcons() throws IOException {
        logger.info("│├正在加载图标...");
        List<BufferedImage> iconList = new ArrayList<>();
        if (dataFolder.list().length == 1) {
            releaseFile(dataFolder, "/res/default/1.png", null);
            releaseFile(dataFolder, "/res/default/2.png", null);
            releaseFile(dataFolder, "/res/default/3.png", null);
            releaseFile(dataFolder, "/res/default/serviceModeIcon.png", null);
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
                logger.log(Level.WARNING, "││├图标\"{0}\"格式不正确,正在进行格式转换...", file.getName());
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
                logger.log(Level.WARNING, "││├无法加载\"{0}\",因为服务器图标的尺寸不能大于64*64!", file.getName());
                continue;
            }
            if (fileName.equalsIgnoreCase("serviceModeIcon.png")) {
                smodeIcon = image;
                logger.info("││├成功加载维护模式下的图标");
                continue;
            }
            iconList.add(image);
            logger.log(Level.INFO, "││├成功加载图标\"{0}\"", file.getName());
        }
        if (smodeIcon == null) {
            logger.info("││├找不到维护模式下的图标\"serviceModeIcon.png\",将使用正常状态下的图标");
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
            icon.add(ImageIO.read(Config.class.getResourceAsStream("/res/default/error.png")));
        } catch (Exception ex) {}
    }

    public static void releaseFile(File folder, String fileName, String to) throws IOException {
        Files.copy(Config.class.getResourceAsStream(fileName), new File(folder, 
                new File(to == null ? fileName : to).getName()).toPath());
    }

    protected String readConfigString() throws IOException{
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
            logger.info("│├配置文件编码为GBK,尝试转换为UTF-8...");
            str = new String(data, "GBK");
            saveConfig(str, StandardCharsets.UTF_8);
        }else{
            logger.info("│├配置文件编码为UTF-8...");
        }
        return str;
    }
    
    public abstract void updateSmode(boolean smode);
    
    public abstract void saveConfig() throws IOException;

    public void saveConfig(String str) throws IOException {
        saveConfig(str, StandardCharsets.UTF_8);
    }

    public void saveConfig(String content, Charset contentCharset) throws IOException {
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
