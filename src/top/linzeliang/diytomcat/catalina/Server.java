package top.linzeliang.diytomcat.catalina;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import cn.hutool.system.SystemUtil;
import org.apache.tools.ant.types.resources.selectors.Exists;
import top.linzeliang.diytomcat.http.Request;
import top.linzeliang.diytomcat.http.Response;
import top.linzeliang.diytomcat.utils.Constant;
import top.linzeliang.diytomcat.utils.ServerXMLUtil;
import top.linzeliang.diytomcat.utils.WebXMLUtil;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @Description: Server服务器类
 * @Author: LinZeLiang
 * @Date: 2021-06-23
 */
public class Server {
    private final Service service;

    public Server() {
        // 创建Service服务对象
        service = new Service(this);
    }

    /**
     * 开启服务器
     */
    public void start() {
        TimeInterval timeInterval = DateUtil.timer();
        // 加载时日志信息
        logJVM();
        // 初始化服务器
        init();
        LogFactory.get().info("Server startup in {} ms", timeInterval.intervalMs());
    }

    /**
     * 初始化服务器
     */
    private void init() {
        service.start();
    }

    /**
     * 日志信息
     */
    private static void logJVM() {
        // 配置参数
        Map<String, String> infos = new LinkedHashMap<>();
        infos.put("Server version", "DiyTomcat/1.0.1");
        infos.put("Server build", "2021-06-18 10:27:04");
        infos.put("Server number", "1.0.1");
        infos.put("OS Name", SystemUtil.get("os.name"));
        infos.put("OS Version", SystemUtil.get("os.version"));
        infos.put("Architecture", SystemUtil.get("os.arch"));
        infos.put("Java Home", SystemUtil.get("java.home"));
        infos.put("JVM Version", SystemUtil.get("java.runtime.version"));
        infos.put("JVM Vendor", SystemUtil.get("java.vm.specification.vendor"));
        Set<String> keys = infos.keySet();
        // 输出日志信息
        for (String key : keys) {
            LogFactory.get().info(key + "\t\t" + infos.get(key));
        }
    }
}
