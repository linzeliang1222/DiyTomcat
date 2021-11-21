package top.linzeliang.diytomcat.classloader;

import cn.hutool.core.util.StrUtil;
import top.linzeliang.diytomcat.catalina.Context;
import top.linzeliang.diytomcat.utils.Constant;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description: TODO
 * @Author: LinZeLiang
 * @Date: 2021-07-24
 */
public class JspClassLoader extends URLClassLoader {

    /**
     * 用于jsp文件和JspClassLoader映射
     */
    private static Map<String, JspClassLoader> jspToJspClassLoader = new HashMap<>();

    private JspClassLoader(Context context) {
        super(new URL[]{}, context.getWebappClassLoader());

        try {
            String subFolder;
            String path = context.getPath();
            if ("/".equals(path)) {
                subFolder = "_";
            } else {
                subFolder = StrUtil.subAfter(path, "/", false);
            }
            File classesFolder = new File(Constant.WORK_FOLDER, subFolder);
            URL url = new URL("file:" + classesFolder.getAbsolutePath() + "/");
            this.addURL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 让jsp文件和JspClassLoader取消关联
     */
    public static void invalidJspClassLoader(String uri, Context context) {
        String key = context.getPath() + "/" + uri;
        jspToJspClassLoader.remove(key);
    }

    public static synchronized JspClassLoader getJspClassLoader(String uri, Context context) {
        String key = context.getPath() + "/" + uri;
        JspClassLoader jspClassLoader = jspToJspClassLoader.get(key);
        if (null == jspClassLoader) {
            jspClassLoader = new JspClassLoader(context);
            jspToJspClassLoader.put(key, jspClassLoader);
        }
        return jspClassLoader;
    }
}
