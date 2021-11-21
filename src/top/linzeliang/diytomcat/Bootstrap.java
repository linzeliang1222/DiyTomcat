package top.linzeliang.diytomcat;

import top.linzeliang.diytomcat.classloader.CommonClassLoader;

import java.lang.reflect.Method;

/**
 * @Description: Tomcat服务器开启入口
 * @Author: LinZeLiang
 * @Date: 2021-06-17
 */
public class Bootstrap {
    public static void main(String[] args) throws Exception {
        // 创建服务器对象
        // Server server = new Server();
        // 开启服务
        // server.start();

        CommonClassLoader commonClassLoader = new CommonClassLoader();

        Thread.currentThread().setContextClassLoader(commonClassLoader);

        String serverClassName = "top.linzeliang.diytomcat.catalina.Server";

        Class<?> serverClazz = commonClassLoader.loadClass(serverClassName);

        Object serverObject = serverClazz.newInstance();

        Method start = serverClazz.getMethod("start");

        start.invoke(serverObject);

        // System.out.println(serverClazz.getClassLoader());
        // 不能关闭，否则后续就不能使用了
        // commonClassLoader.close();
    }
}