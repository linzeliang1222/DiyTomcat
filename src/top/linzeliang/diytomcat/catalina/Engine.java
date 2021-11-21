package top.linzeliang.diytomcat.catalina;

import top.linzeliang.diytomcat.utils.ServerXMLUtil;

import java.util.List;

/**
 * @Description: 代表Engine引擎
 * @Author: LinZeLiang
 * @Date: 2021-06-22
 */
public class Engine {
    private final String defaultHost;
    private final List<Host> hosts;
    private final Service service;

    public Engine(Service service) {
        // 初始化
        this.defaultHost = ServerXMLUtil.getEngineDefaultHost();
        this.hosts = ServerXMLUtil.getHosts(this);
        this.service = service;
        // 检查默认主机是否存在
        checkDefault();
    }

    /**
     * 检查默认主机是否存在
     */
    private void checkDefault() {
        if (null == getDefaultHost()) {
            throw new RuntimeException("the defaultHost " + defaultHost + " doesn't exist!");
        }
    }

    /**
     * 获取默认主机
     */
    public Host getDefaultHost() {
        // 遍历所有的主机
        for (Host host : hosts) {
            // 如果存在默认主机，就直接返回
            if (host.getName().equals(defaultHost)) {
                return host;
            }
        }
        return null;
    }

    /**
     * 获取service
     */
    public Service getService() {
        return service;
    }
}
