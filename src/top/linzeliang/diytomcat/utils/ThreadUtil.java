package top.linzeliang.diytomcat.utils;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @Description: 线程池工具类
 * @Author: LinZeLiang
 * @Date: 2021-06-18
 */
public class ThreadUtil {
    /**
     * 创建默认大小为20的线程池
     * 1. 第一个参数 20 表示线程池初始有20根 核心线程数。
     * 2. 第二个参数 100 表示最多会有 100根线程。
     * 3. 第三个和第四个参数 表示如果 新增加出来的线程如果空闲时间超过 60秒，那么就会被回收，最后保留 20根线程。
     * 4. 第五个参数 new LinkedBlockingQueue<Runnable>(10)， 表示当有很多请求短时间过来，使得20根核心线程都满了
     * 之后，并不会马上分配新的线程处理更多的请求， 而是把这些请求放过在 这个 LinkedBlockingQueue里， 当核心线
     * 程忙过来了，就会来处理 这个队列里的请求。 只有当处理不过来的请求数目超过 了 10个之后，才会增加更多的线程来
     * 处理。
     */
    private static final ThreadPoolExecutor THREAD_POOL = new ThreadPoolExecutor(20, 100, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(10));

    /**
     * 获取线程执行程序
     */
    public static void run(Runnable runnable) {
        THREAD_POOL.execute(runnable);
    }
}
