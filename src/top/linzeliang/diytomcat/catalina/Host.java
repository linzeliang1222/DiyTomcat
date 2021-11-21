package top.linzeliang.diytomcat.catalina;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import top.linzeliang.diytomcat.utils.Constant;
import top.linzeliang.diytomcat.utils.ServerXMLUtil;
import top.linzeliang.diytomcat.watcher.WarFileWatcher;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: 代表主机
 * @Author: LinZeLiang
 * @Date: 2021-06-22
 */
public class Host {
    /**
     * 主机名
     */
    private final String name;
    /**
     * 用于存放路径和Context的映射
     */
    private final Map<String, Context> contextMap;
    /**
     * 所属引擎
     */
    private final Engine engine;

    public Host(String name, Engine engine) {
        this.name = name;
        this.contextMap = new HashMap<>();
        this.engine = engine;

        // 扫描应用资源
        // 扫描webapps目录下的资源
        scanContextOnWebAppsFolder();
        // 扫描配置文件中配置的资源
        scanContextsInServerXML();
        // 扫描war包的web项目，并且加载它
        scanWarOnWebappsFolder();

        // 开启war动态监听部署
        new WarFileWatcher(this).start();
    }

    /**
     * 扫描webapps目录下的应用
     */
    private void scanContextOnWebAppsFolder() {
        // 获取webapps目录下的所有文件和文件夹
        File[] folders = Constant.WEBAPP_FOLDER.listFiles();
        for (File folder : folders) {
            // 如果是文件就跳过
            if (!folder.isDirectory()) {
                continue;
            }
            // 加载文件夹
            loadContext(folder);
        }
    }

    /**
     * 加载文件夹
     */
    private void loadContext(File folder) {
        // 获取文件夹名称
        String path = folder.getName();
        // 如果是ROOT，就是默认的路径，即/，其他的就是文件夹名称
        if ("ROOT".equals(path)) {
            path = "/";
        } else {
            path = "/" + path;
        }

        // 获取绝对路径
        String docBase = folder.getAbsolutePath();
        // 创建应用，webapps目录下的应用自动开启热部署
        Context context = new Context(path, docBase, this, true);
        // 添加到路径与应用映射表中
        contextMap.put(path, context);
    }

    /**
     * 扫描server.xml中的Context添加到contextMap中
     */
    private void scanContextsInServerXML() {
        // 先扫描获取得到的context集合
        List<Context> contexts = ServerXMLUtil.getContexts(this);
        // 添加到contextMap中
        for (Context context : contexts) {
            contextMap.put(context.getPath(), context);
        }
    }

    /**
     * 重新加载应用
     */
    public void reload(Context context) {
        // 输出日志
        LogFactory.get().info("Reloading Context with name [{}] has started", context.getPath());

        // 获取context的信息
        String path = context.getPath();
        String docBase = context.getDocBase();
        boolean reloadable = context.isReloadable();

        // 先停止改动过的context的监听
        context.stop();
        // 将改动过的context从contextMap中删除
        contextMap.remove(path);
        // 重新生成一个新的context
        Context newContext = new Context(path, docBase, this, reloadable);
        // 重新添加到contextMap
        contextMap.put(newContext.getPath(), newContext);

        // 再输出日志
        LogFactory.get().info("Reloading Context with name [{}] has completed", context.getPath());
    }

    public Context getContext(String path) {
        return contextMap.get(path);
    }

    public String getName() {
        return name;
    }

    /**
     * 把文件夹加载为Context
     */
    public void load(File folder) {
        String path = folder.getName();
        if ("ROOT".equals(path)) {
            path = "/";
        } else {
            path = "/" + path;
        }
        String docBase = folder.getAbsolutePath();
        Context context = new Context(path, docBase, this, false);
        contextMap.put(context.getPath(), context);
    }

    /**
     * 把war文件解压为目录，并把文件夹加载为Context
     */
    public void loadWar(File warFile) {
        // war文件名
        String fileName = warFile.getName();
        // war文件部署后生成的文件夹的名称
        String folderName = StrUtil.subBefore(fileName, ".", true);
        // 检测context是否存在
        Context context = getContext("/" + folderName);
        if (null != context) {
            return;
        }
        // 检测是否有对应的文件夹
        File folder = new File(Constant.WEBAPP_FOLDER, folderName);
        if (folder.exists()) {
            return;
        }
        // 复制war文件到war名称的文件夹下，因为jar命令只支持解压到当前目录下
        File tempWarFile = FileUtil.file(Constant.WEBAPP_FOLDER, folderName, fileName);
        File contextFolder = tempWarFile.getParentFile();
        // 创建war同名的文件夹
        contextFolder.mkdir();
        FileUtil.copyFile(warFile, tempWarFile);
        // 解压
        String command = "jar xvf " + fileName;
        Process process = RuntimeUtil.exec(null, contextFolder, command);
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 解压之后删除临时的war
        tempWarFile.delete();
        // 然后创建新的context
        load(contextFolder);
    }

    /**
     *
     */
    private void scanWarOnWebappsFolder() {
        File folder = FileUtil.file(Constant.WEBAPP_FOLDER);
        File[] files = folder.listFiles();
        for (File file : files) {
            // 如果部署war文件夹就跳过
            if (!file.getName().endsWith(".war")) {
                continue;
            }
            loadWar(file);
        }
    }
}
