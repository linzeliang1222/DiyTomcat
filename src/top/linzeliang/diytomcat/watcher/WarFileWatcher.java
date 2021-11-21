package top.linzeliang.diytomcat.watcher;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.io.watch.Watcher;
import top.linzeliang.diytomcat.catalina.Host;
import top.linzeliang.diytomcat.utils.Constant;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;

/**
 * @Description: 动态部署war文件
 * @Author: LinZeLiang
 * @Date: 2021-07-25
 */
public class WarFileWatcher {

    private WatchMonitor watchMonitor;

    public WarFileWatcher(Host host) {
        this.watchMonitor = WatchUtil.createAll(Constant.WEBAPP_FOLDER, 1, new Watcher() {
            @Override
            public void onCreate(WatchEvent<?> event, Path currentPath) {
                dealWith(event, currentPath);
            }

            @Override
            public void onModify(WatchEvent<?> event, Path currentPath) {
                dealWith(event, currentPath);
            }

            @Override
            public void onDelete(WatchEvent<?> event, Path currentPath) {
                dealWith(event, currentPath);
            }

            @Override
            public void onOverflow(WatchEvent<?> event, Path currentPath) {
                dealWith(event, currentPath);
            }

            /**
             * war文件变动的处理
             */
            private void dealWith(WatchEvent<?> event, Path currentPath) {
                synchronized (WarFileWatcher.class) {
                    String fileName = event.context().toString();
                    if (fileName.toLowerCase().endsWith(".war") && ENTRY_CREATE.equals(event.kind())) {
                        File warFile = FileUtil.file(Constant.WEBAPP_FOLDER, fileName);
                        host.loadWar(warFile);
                    }
                }
            }
        });
    }

    public void start() {
        watchMonitor.start();
    }

    public void stop() {
        watchMonitor.interrupt();
    }
}
