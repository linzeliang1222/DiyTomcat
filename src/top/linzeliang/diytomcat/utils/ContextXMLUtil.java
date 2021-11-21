package top.linzeliang.diytomcat.utils;

import cn.hutool.core.io.FileUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * @Description: 解析server.xml配置文件工具
 * @Author: LinZeLiang
 * @Date: 2021-07-19
 */
public class ContextXMLUtil {

    /**
     *
     */
    public static String getWatchedResource() {
        try {
            String xml = FileUtil.readUtf8String(Constant.CONTEXT_XML_FILE);
            Document document = Jsoup.parse(xml);
            Element element = document.selectFirst("WatchedResource");
            return element.text();
        } catch (Exception e) {
            e.printStackTrace();
            return "WEB-INF/web.xml";
        }
    }
}
