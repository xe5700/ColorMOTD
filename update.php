<?php
error_reporting(0);
include_once '../../../api/Log.class.php';
$ip = Log::getIP();
$useragent = !isset($_SERVER['HTTP_USER_AGENT']) ? '机器人' : $_SERVER['HTTP_USER_AGENT'];
Log::log("[IP:{$ip}] [User-Agent:{$useragent}] 请求更新文件");
?>{"build":13,"version":"1.3.7","msg":"- 移除了README.md
\n* 重构代码
\n* 使用Bukkit自带的simplejson代替org.json
+\n 支持RedisBungee在线人数","url":"http://vcheck.windit.net/mc_plugin/andylizi/colormotd/ColorMOTD.jar"}