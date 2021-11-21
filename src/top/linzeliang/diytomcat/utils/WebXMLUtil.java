package top.linzeliang.diytomcat.utils;

import cn.hutool.core.io.FileUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import top.linzeliang.diytomcat.catalina.Context;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description: web.xml工具类
 * @Author: LinZeLiang
 * @Date: 2021-06-28
 */
public class WebXMLUtil {

    // 使用将后缀名和mime_type一一对应
    private static Map<String, String> mimeTypeMapping = new HashMap<>();

    /**
     * 获取欢迎页地址
     *
     * @param context 应用
     */
    public static String getWelcomeFile(Context context) {
        String xml = FileUtil.readUtf8String(Constant.WEB_XML_FILE);
        Document document = Jsoup.parse(xml);

        // 获取配置文件中的元素集合
        Elements elements = document.select("welcome-file");
        // 获取配置文件中欢迎页的文件
        for (Element element : elements) {
            String welcomeFileName = element.text();
            File file = new File(context.getDocBase(), welcomeFileName);
            // 只要其中一个文件存在直接退出遍历
            if (file.exists()) {
                return file.getName();
            }
        }

        // 如果没有配置默认的欢迎页文件，那么就是默认index.html
        return "index.html";
    }

    /**
     * 初始化mimeTypeMapping
     */
    public static void initMimeType() {
        String xml = FileUtil.readUtf8String(Constant.WEB_XML_FILE);
        Document document = Jsoup.parse(xml);

        Elements elements = document.select("mime-mapping");
        for (Element element : elements) {
            String extension = element.selectFirst("extension").text();
            String mimeType = element.selectFirst("mime-type").text();
            mimeTypeMapping.put(extension, mimeType);
        }
    }

    /**
     * @description 通过扩展名获取mime类型
     * 这里做了 synchronized 线程安全的处理， 因为会调用 initMimeType 进行初始化，如果两个线程同时来，那么可能导致被初始化两次
     *
     * @param extension 扩展名
     */
    public static synchronized String getMimeType(String extension) {
        // 如果mimeTypeMapping是空的，那么就先初始化
        if (mimeTypeMapping.isEmpty()) {
            initMimeType();
        }

        // 获取mime类型
        String mimeType = mimeTypeMapping.get(extension);

        // 如果没有对应的mime类型，就默认text/html
        if (null == mimeType) {
            return "text/html";
        }

        return mimeType;
    }
}
