package top.linzeliang.diytomcat.catalina;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.log.LogFactory;
import top.linzeliang.diytomcat.utils.ServerXMLUtil;

import java.util.List;

/**
 * @Description: 代表Service服务
 * @Author: LinZeLiang
 * @Date: 2021-06-23
 */
public class Service {
    private String name;
    private Engine engine;
    private Server server;
    private List<Connector> connectors;

    public Service(Server server) {
        this.name = ServerXMLUtil.getServiceName();
        this.engine = new Engine(this);
        this.server = server;
        // 初始化Connectors
        this.connectors = ServerXMLUtil.getConnectors(this);
    }

    /**
     * 获取引擎
     */
    public Engine getEngine() {
        return engine;
    }

    public void start() {
        init();
    }

    private void init() {
        TimeInterval timeInterval = DateUtil.timer();
        for (Connector connector : connectors) {
            // 将每一个端口都初始化
            connector.init();
        }
        // 日志信息
        LogFactory.get().info("Initialization processed in {} ms", timeInterval.intervalMs());
        for (Connector connector : connectors) {
            // 将每一个端口都开启
            connector.start();
        }
    }
}