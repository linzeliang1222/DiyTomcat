package top.linzeliang.diytomcat.classloader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @Description: 公共类加载器
 * @Author: LinZeLiang
 * @Date: 2021-07-20
 */
public class CommonClassLoader extends URLClassLoader {
    public CommonClassLoader() {
        super(new URL[]{});

        try {
            // 工作目录
            File workingFolder = new File(System.getProperty("user.dir"));
            // jar包目录
            File libFolder = new File(workingFolder, "lib");
            // jar包文件集合
            File[] jarFiles = libFolder.listFiles();
            for (File file : jarFiles) {
                // 只扫描jar结尾的包
                if (file.getName().endsWith(".jar")) {
                    URL url = new URL("file:" + file.getAbsolutePath());
                    this.addURL(url);
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
