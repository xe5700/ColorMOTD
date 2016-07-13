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

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public final class AttributionUtil extends Object {
    public static Map<String, String> attributionTemp;
    private static JSONParser paser = new JSONParser();
    public static AttributionServer attributionServer = AttributionServer.TAOBAO;

    static {
        attributionTemp = new HashMap<>();
        new Timer("ColorMOTDAttributionTempResetTimer",true).schedule(new TimerTask() {
            @Override
            public void run() {
                if (attributionTemp == null) {
                    attributionTemp = new HashMap<>();
                }
                attributionTemp.clear();
            }
        }, 1L, 20L * 60L * 30L); //0.5 hour
    }

    public static String getResult(String address) {
        address = address.replace("/", "");
        if (address.contains(":")) {
            String s[] = address.split(":");
            address = s[0];
        }
        if (attributionTemp.containsKey(address)) {
            return attributionTemp.get(address);
        }
        String result = null;
        switch (attributionServer) {
            case TAOBAO: {
                result = TAOBAO.getResult(address);
                break;
            }
            case IP138: {
                result = IP138.getResult(address);
                break;
            }
            default: {
                throw new UnsupportedOperationException("未被加入的归属地服务器\"" + attributionServer + "\",如果你看到这条错误信息说明你中大奖了,赶紧反馈作者");
            }
        }
        if(result != null) attributionTemp.put(address, result);
        return result;
    }

    public static String formatLocation(String result) {
        switch (attributionServer) {
            case TAOBAO: {
                return TAOBAO.formatLocation(result);
            }
            case IP138: {
                return IP138.formatLocation(result);
            }
            default: {
                throw new UnsupportedOperationException("未被加入的归属地服务器\"" + attributionServer + "\",如果你看到这条错误信息说明你中大奖了,赶紧反馈作者");
            }
        }
    }

    public static String formatISP(String result) {
        switch (attributionServer) {
            case TAOBAO: {
                return TAOBAO.formatISP(result);
            }
            case IP138: {
                return IP138.formatISP(result);
            }
            default: {
                throw new UnsupportedOperationException("未被加入的归属地服务器\"" + attributionServer + "\",如果你看到这条错误信息说明你中大奖了,赶紧反馈作者");
            }
        }
    }

    public static class IP138 {

        private static final String URL = "http://www.ip138.com/ips138.asp";

        public static String getResult(String address) {
            String result = sendGet(URL, "action=2&ip=" + address,"gbk");
            Pattern pattern = Pattern.compile("<li>本站主数据：(.*?)</li>");
            Matcher matcher = pattern.matcher(result);
            if (matcher.find()) {
                return matcher.group(1);
            } else {
                return "获取失败 获取失败";
            }
        }

        public static String formatLocation(String result) {
            String[] args = result.split(" ");
            if (args.length <= 0) {
                return "获取失败";
            }
            if(args.length == 1){
                return result;
            }
            StringBuilder loc = new StringBuilder();
            for (int i = 0; i < args.length - 1; i++) {
                loc.append(args[i]);
            }
            return loc.toString();
        }

        public static String formatISP(String result) {
            String[] args = result.split(" ");
            if (args.length <= 1) {
                return "未知";
            }
            return args[args.length - 1];
        }
    }

    public static class TAOBAO {

        private static final String URL = "http://ip.taobao.com/service/getIpInfo.php";

        public static String getResult(String address) {
            String result = sendGet(URL, "ip=" + address, "utf-8");
            try {
                return result;
            } finally {
                attributionTemp.put(address, result);
            }
        }

        public static String formatLocation(String result) {
            try {
                JSONObject json = (JSONObject) paser.parse(result);
                paser.reset();
                JSONObject data = (JSONObject) json.get("data");
                String contry = (String) data.get("country");
                String region = (String) data.get("region");
                String city = (String) data.get("city");
                if (contry.equals("中国")) {
                    contry = "";
                }
                if (region.equals(city)) {
                    region = "";
                }
                return contry + region + city;
            } catch (Exception t) {
                return "获取失败";
            }
        }

        public static String formatISP(String result) {
            try {
                JSONObject json = (JSONObject) paser.parse(result);
                paser.reset();
                JSONObject data = (JSONObject) json.get("data");
                return (((String)data.get("isp")).isEmpty() ? "未知" : (String)data.get("isp"));
            } catch (Exception t) {
                return "获取失败";
            }
        }

    }

    private static String sendGet(String url, String param, String charset) {
        StringBuilder result = new StringBuilder(1024);
        BufferedReader in = null;
        try {
            String urlNameString = url + "?" + param;
            URL realUrl = new URL(urlNameString);
            HttpURLConnection conn = (HttpURLConnection) realUrl.openConnection();
            conn.setRequestProperty("User-Agent", "ColorMOTD/"+UUID.randomUUID());
            conn.setRequestProperty("Accept-Charset", charset);
            conn.setUseCaches(true);
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(3000);
            conn.connect();
            in = new BufferedReader(new InputStreamReader(conn.getInputStream(), Charset.forName(charset)), 1024);
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
            conn.disconnect();
        } catch (Exception e) {
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
            }
        }
        return result.toString();
    }

    public static enum AttributionServer {
        TAOBAO,
        IP138;

        public static String list() {
            StringBuilder sb = new StringBuilder();
            for (AttributionServer as : values()) {
                sb.append(as.toString().toLowerCase()).append(',');
            }
            sb.delete(sb.length()-1, sb.length());
            return sb.toString();
        }
    }
}
