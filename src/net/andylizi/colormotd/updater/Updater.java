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
package net.andylizi.colormotd.updater;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Date;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

import net.andylizi.colormotd.Main;
import static net.andylizi.colormotd.Main.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public final class Updater extends TimerTask {

    private final String fileName;
    private String etag = null;
    public NewVersion newVersion;
    public Timer countdown;
    public Throwable exception;
    public static final boolean DEBUG = true;
    private static final int UPDATER_VERSION = 1;
    private static final String UPDATE_URL = "http://vcheck.windit.net/mc_plugin/andylizi/colormotd/update.json";
    private static String USER_AGENT;

    public Updater(String fileName) {
        this.fileName = fileName;
        USER_AGENT = plugin.getDescription().getFullName() + "/" + Bukkit.getPort() + "/Updater/" + UPDATER_VERSION;
    }

    private boolean checkUpdate() {
        if (new File(Bukkit.getUpdateFolderFile(), fileName).exists()) {
            cancel();
        }
        URL url;
        try {
            url = new URL(UPDATE_URL);
        } catch (MalformedURLException ex) {
            if (DEBUG) {
                ex.printStackTrace();
            }
            return false;
        }
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setUseCaches(false);
            conn.setRequestMethod("GET");
            conn.addRequestProperty("User-Agent", USER_AGENT);
            if (etag != null) {
                conn.addRequestProperty("If-None-Match", etag);
            }
            conn.addRequestProperty("Accept", "application/json,text/plain,*/*;charset=utf-8");
            conn.addRequestProperty("Accept-Encoding", "gzip");
            conn.addRequestProperty("Cache-Control", "no-cache");
            conn.addRequestProperty("Date", new Date().toString());
            conn.addRequestProperty("Connection", "close");

            conn.setDoInput(true);
            conn.setDoOutput(false);
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);

            conn.connect();

            if (conn.getHeaderField("ETag") != null) {
                etag = conn.getHeaderField("ETag");
            }
            if (DEBUG) {
                logger.log(Level.INFO, "ResponseCode: {0}", conn.getResponseCode());
                logger.log(Level.INFO, "ETag: {0}", etag);
            }
            if (conn.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                return false;
            } else if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return false;
            }

            BufferedReader input = null;
            if (conn.getContentEncoding() != null && conn.getContentEncoding().contains("gzip")) {
                input = new BufferedReader(new InputStreamReader(new GZIPInputStream(conn.getInputStream()), "UTF-8"), 1);
            } else {
                input = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"), 1);
            }
            
            StringBuilder builder = new StringBuilder();
            String buffer = null;
            while((buffer = input.readLine()) != null){
                builder.append(buffer).append('\n');
            }
            try {
                JSONObject jsonObj = (JSONObject) new JSONParser().parse(builder.toString());
                int build = ((Number) jsonObj.get("build")).intValue();
                if (build <= buildVersion) {
                    return false;
                }
                String version = (String) jsonObj.get("version");
                String msg = (String) jsonObj.get("msg");
                String download_url = (String) jsonObj.get("url");
                newVersion = new NewVersion(version, build, msg, download_url);
                return true;
            } catch (ParseException ex) {
                if (DEBUG) {
                    ex.printStackTrace();
                }
                exception = ex;
                return false;
            }
        } catch (SocketTimeoutException ex) {
            return false;
        } catch (IOException ex) {
            if (DEBUG) {
                ex.printStackTrace();
            }
            exception = ex;
            return false;
        }
    }

    @Override
    public void run() {
        newVersion = null;
        if (!Main.getInstance().isEnabled()) {
            cancel();
            return;
        }
        if (!checkUpdate()) {
            return;
        }
        Main.broadcast("§b*************************************************"
                + "\n§a§lColorMOTD有新版本啦!现在最新的版本是:§6§l§o" + newVersion.version
                + "\n§6更新日志:§r"
                + "\n" + ChatColor.translateAlternateColorCodes('&', newVersion.msg)
                + "\n§b*************************************************"
                + "\n§c<警告>将在30秒后自动开始下载更新!\n§c如果不想下载请在30秒之前输入§o§5/colormotd stopupdate§c!");
        cancel();
        (countdown = new Timer("UpdaterDownloadCountdown", true)).schedule(new TimerTask() {
            @Override
            public void run() {
                if (!Main.getInstance().isEnabled()) {
                    cancel();
                }
                broadcast("§e开始自动下载ColorMOTD " + newVersion.version + "...");
                newVersion.download();
            }
        }, 30L * 1000L);
    }

    public final class NewVersion implements Cloneable, Serializable {

        private static final long serialVersionUID = -24703045540636574L;

        private String version;
        private int build;
        private String msg;
        private String download_url;

        public NewVersion(String version, int build, String msg, String download_url) {
            this.version = version;
            this.build = build;
            this.msg = msg;
            this.download_url = download_url;
        }

        public void download() {
            URL url;
            try {
                url = new URL(download_url);
            } catch (MalformedURLException ex) {
                broadcast("§4自动更新下载失败,下载链接格式错误: §r" + ex.toString());
                ex.printStackTrace();
                return;
            }
            try {
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setUseCaches(false);
                conn.setRequestMethod("GET");
                conn.addRequestProperty("User-Agent", USER_AGENT);
                conn.addRequestProperty("Accept", "application/java-archive,application/octet-stream,*/*;");
                conn.addRequestProperty("Accept-Encoding", "gzip");
                conn.addRequestProperty("Cache-Control", "no-cache");
                conn.addRequestProperty("Date", new Date().toString());
                conn.addRequestProperty("Connection", "close");

                conn.setDoInput(true);
                conn.setReadTimeout(5000);
                conn.setConnectTimeout(3000);

                conn.connect();

                BufferedInputStream input;
                if (conn.getContentEncoding() != null && conn.getContentEncoding().contains("gzip")) {
                    input = new BufferedInputStream(new GZIPInputStream(conn.getInputStream()), 10240);
                } else {
                    input = new BufferedInputStream(conn.getInputStream(), 10240);
                }
                File file = new File(Bukkit.getUpdateFolderFile(), fileName);
                file.getParentFile().mkdirs();
                file.createNewFile();
                broadcast("§e连接成功,总大小" + conn.getContentLength() + "bytes,开始下载...");
                try (FileOutputStream output = new FileOutputStream(file, false)) {
                    byte[] buffer = new byte[1024];
                    while (input.read(buffer) != -1) {
                        output.write(buffer);
                    }
                }
                input.close();
                broadcast("§a自动更新完成,将在下次重启服务器后生效");
            } catch (IOException ex) {
                broadcast("§4自动更新下载失败: 下载时出错: §r§o" + ex.toString());
                ex.printStackTrace();
                return;
            }
            cancel();
        }

        /**
         * @return the version
         */
        public String getVersion() {
            return version;
        }

        /**
         * @return the build
         */
        public int getBuild() {
            return build;
        }

        /**
         * @return the msg
         */
        public String getMsg() {
            return msg;
        }

        /**
         * @return the download_url
         */
        public String getDownload_url() {
            return download_url;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 67 * hash + Objects.hashCode(this.version);
            hash = 67 * hash + this.build;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final NewVersion other = (NewVersion) obj;
            if (this.build != other.build) {
                return false;
            }
            return Objects.equals(this.version, other.version);
        }

        @Override
        public String toString() {
            return new StringBuilder().append('[').append("build=").append(build).append(",version=").append(version).append(",msg=").append(msg).append(",download_url=").append(download_url).append(']').toString();
        }

        @Override
        public Object clone() {
            return new NewVersion(version, build, msg, download_url);
        }

    }
}
