package top.linzeliang.diytomcat.utils;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import top.linzeliang.diytomcat.catalina.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description: 解析server.xml配置文件
 * @Author: LinZeLiang
 * @Date: 2021-06-22
 */
public class ServerXMLUtil {

    /**
     * 解析Context
     */
    public static List<Context> getContexts(Host host) {
        // 存放Context结果
        List<Context> result = new ArrayList<>();
        // 先读取配置文件
        String xml = FileUtil.readUtf8String(Constant.SERVER_XML_FILE);
        // 讲配置文件解析成Document格式
        Document document = Jsoup.parse(xml);

        // 获取所有Context节点元素
        Elements elements = document.select("Context");
        // 遍历获取的Context节点集
        for (Element element : elements) {
            // 获取每个Context的path
            String path = element.attr("path");
            // 获取每个Context的docBase
            String docBase = element.attr("docBase");
            // 获取是否热部署，默认为true
            Boolean reloadable = Convert.toBool(element.attr("reloadable"), true);
            // 创建context
            Context context = new Context(path, docBase, host, reloadable);
            // 将context添加到result中
            result.add(context);
        }

        return result;
    }

    /**
     * 解析Service，获取Service的name
     */
    public static String getServiceName() {
        // 先读取配置文件
        String xml = FileUtil.readUtf8String(Constant.SERVER_XML_FILE);
        // 讲配置文件解析成Document格式
        Document document = Jsoup.parse(xml);

        // 获取Service元素
        Element service = document.selectFirst("Service");
        // 返回Service的name的属性值
        return service.attr("name");
    }

    /**
     * 获取Engine引擎
     */
    public static String getEngineDefaultHost() {
        // 先读取配置文件
        String xml = FileUtil.readUtf8String(Constant.SERVER_XML_FILE);
        // 讲配置文件解析成Document格式
        Document document = Jsoup.parse(xml);

        // 获取Engine元素
        Element engine = document.selectFirst("Engine");
        // 返回defaultHost属性值
        return engine.attr("defaultHost");
    }

    /**
     * 获取指定Engine下的所有Host
     *
     * @param engine 指定的引擎
     */
    public static List<Host> getHosts(Engine engine) {
        // 创建一个List用于存放结果
        List<Host> result = new ArrayList<>();
        // 先读取配置文件
        String xml = FileUtil.readUtf8String(Constant.SERVER_XML_FILE);
        // 讲配置文件解析成Document格式
        Document document = Jsoup.parse(xml);

        // 获取所有的主机元素
        Elements elements = document.select("Host");
        // 依次遍历每个元素
        for (Element element : elements) {
            // 获取主机名字
            String name = element.attr("name");
            // 创建主机
            Host host = new Host(name, engine);
            // 将主机添加到结果集中
            result.add(host);
        }

        // 返回指定引擎的所有主机集合
        return result;
    }

    /**
     * 获取Connectors集合
     */
    public static List<Connector> getConnectors(Service service) {
        List<Connector> result = new ArrayList<>();

        String xml = FileUtil.readUtf8String(Constant.SERVER_XML_FILE);
        Document document = Jsoup.parse(xml);
        Elements elements = document.select("Connector");

        for (Element element : elements) {
            // 获取属性
            int port = Convert.toInt(element.attr("port"));
            String compression = elements.attr("compression");
            int compressionMinSize = Convert.toInt(element.attr("compressionMinSize"), 0);
            String noCompressionUserAgents = element.attr("noCompressionUserAgents");
            String compressableMimeType = element.attr("compressableMimeType");
            // 创建Connector，将这些属性设置到Connector中
            Connector connector = new Connector(service);
            connector.setPort(port);
            connector.setCompression(compression);
            connector.setCompressionMinSize(compressionMinSize);
            connector.setNoCompressionUserAgents(noCompressionUserAgents);
            connector.setCompressableMimeType(compressableMimeType);

            result.add(connector);
        }
        return result;
    }
}
