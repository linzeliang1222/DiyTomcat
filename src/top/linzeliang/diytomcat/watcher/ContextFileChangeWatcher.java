package top.linzeliang.diytomcat.watcher;

import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.io.watch.Watcher;
import cn.hutool.log.LogFactory;
import top.linzeliang.diytomcat.catalina.Context;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

/**
 * @Description: Context文件改变监听器
 * @Author: LinZeLiang
 * @Date: 2021-07-20
 */
public class ContextFileChangeWatcher {
    /**
     * monitor 是真正其作用的监听器
     */
    private WatchMonitor monitor;
    /**
     * stop 标记是否已经暂停。
     */
    private boolean stop = false;

    public ContextFileChangeWatcher(Context context) {

        // 通过WatchUtil.createAll创建监听器
        this.monitor = WatchUtil.createAll(context.getDocBase(), Integer.MAX_VALUE, new Watcher() {
            @Override
            public void onCreate(WatchEvent<?> event, Path currentPath) {
                dealWith(event);
            }

            @Override
            public void onModify(WatchEvent<?> event, Path currentPath) {
                dealWith(event);
            }

            @Override
            public void onDelete(WatchEvent<?> event, Path currentPath) {
                dealWith(event);
            }

            @Override
            public void onOverflow(WatchEvent<?> event, Path currentPath) {
                dealWith(event);
            }

            private void dealWith(WatchEvent<?> event) {
                synchronized (ContextFileChangeWatcher.class) {
                    // 取得当前发生变化的文件或者文件夹名称
                    String fileName = event.context().toString();
                    if (stop) {
                        // 表示已经重载过了，后面再来的消息就不管了
                        return;
                    }
                    // 表示只应对 jar class 和 xml 发生的变化，其他的不需要重启，比如 html ,txt等，没必要重启
                    if (fileName.endsWith(".jar") || fileName.endsWith(".class") || fileName.endsWith(".xml")) {
                        // 标记一下，表示重载过了
                        stop = true;
                        // 打印日志
                        LogFactory.get().info(ContextFileChangeWatcher.this + " 检测到了Web应用下的重要文件的变化 {} ", fileName);
                        // 进行重载
                        context.reload();
                    }
                }
            }
        });
        // 守护线程
        this.monitor.setDaemon(true);
    }

    /**
     * 启动
     */
    public void start() {
        monitor.start();
    }

    /**
     * 停止
     */
    public void stop() {
        monitor.close();
    }

}
