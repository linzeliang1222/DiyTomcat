package top.linzeliang.diytomcat.classloader;

import cn.hutool.core.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * @Description: WebappClassLoader类加载器
 * @Author: LinZeLiang
 * @Date: 2021-07-20
 */
public class WebappClassLoader extends URLClassLoader {

    public WebappClassLoader(String docBase, ClassLoader commonClassLoader) {
        super(new URL[]{}, commonClassLoader);

        try {
            // 扫描 Context 对应的 docBase 下的 classes 和 lib
            // 先获取WEB-INF目录
            File webInfoFolder = new File(docBase, "WEB-INF");
            // 存放字节码文件
            File classesFolder = new File(webInfoFolder, "classes");
            // 存放jar包的目录
            File libFolder = new File(webInfoFolder, "lib");

            // 把classes目录，通过addURL加进去
            // 注意，因为是目录，所以加的时候，要在结尾跟上 "/" , URLClassLoader才会把它当作目录来处理
            URL url = new URL("file:" + classesFolder.getAbsolutePath() + "/");
            this.addURL(url);

            // 把lib目录下的jar包通过addURL加进去
            List<File> jarFiles = FileUtil.loopFiles(libFolder);
            for (File file : jarFiles) {
                url = new URL("file:" + file.getAbsolutePath());
                this.addURL(url);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
