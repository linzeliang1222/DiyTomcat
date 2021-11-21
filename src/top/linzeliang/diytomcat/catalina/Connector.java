package top.linzeliang.diytomcat.catalina;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import top.linzeliang.diytomcat.http.Request;
import top.linzeliang.diytomcat.http.Response;
import top.linzeliang.diytomcat.utils.Constant;
import top.linzeliang.diytomcat.utils.WebXMLUtil;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @Description: 不同端口的Connector
 * @Author: LinZeLiang
 * @Date: 2021-07-19
 */
public class Connector implements Runnable {

    /**
     * 端口号
     */
    private int port;

    /**
     * 对应的父service
     */
    private Service service;

    /**
     * 表示是否启动，当等于 "on" 的时候，表示启动
     */
    private String compression;

    /**
     * 表示最小进行压缩的字节数
     */
    private int compressionMinSize;

    /**
     * 表示不进行压缩的浏览器
     */
    private String noCompressionUserAgents;

    /**
     * 表示哪些mimeType才需要进行压缩
     */
    private String compressableMimeType;

    public Connector(Service service) {
        this.service = service;
    }

    public Service getService() {
        return this.service;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try {

//            // 判断端口是否占用了
//            if (!NetUtil.isUsableLocalPort(port)) {
//                System.out.println(port + " 端口已经被占用了！");
//                return;
//            }

            // 创建服务端套接字
            ServerSocket serverSocket = new ServerSocket(port);

            // 处理掉一个Socket链接请求之后，再处理下一个链接请求
            while (true) {
                // 开启一个socket连接,由服务端那个serverSocket来接受，被动地等待数据
                Socket socket = serverSocket.accept();
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // 初始化请求对象
                            Request request = new Request(socket, Connector.this);
                            // 初始化响应对象
                            Response response = new Response();

                            // 处理http请求
                            HttpProcessor httpProcessor = new HttpProcessor();
                            httpProcessor.execute(socket, request, response);
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            // 处理完后判断socket是否关闭了
                            try {
                                if (!socket.isClosed()) {
                                    socket.close();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
                top.linzeliang.diytomcat.utils.ThreadUtil.run(runnable);
            }
        } catch (IOException e) {
            LogFactory.get().error(e);
            e.printStackTrace();
        }
    }

    public void init() {
        LogFactory.get().info("Initializing ProtocolHandler [http-bio-{}]", port);
    }

    /**
     * 创建一个线程，以当前类为任务，启动运行，并打印 tomcat 风格的日志
     */
    public void start() {
        LogFactory.get().info("Starting ProtocolHandler [http-bio-{}]", port);
        new Thread(this).start();
    }

    public String getCompression() {
        return compression;
    }

    public void setCompression(String compression) {
        this.compression = compression;
    }

    public int getCompressionMinSize() {
        return compressionMinSize;
    }

    public void setCompressionMinSize(int compressionMinSize) {
        this.compressionMinSize = compressionMinSize;
    }

    public String getNoCompressionUserAgents() {
        return noCompressionUserAgents;
    }

    public void setNoCompressionUserAgents(String noCompressionUserAgents) {
        this.noCompressionUserAgents = noCompressionUserAgents;
    }

    public String getCompressableMimeType() {
        return compressableMimeType;
    }

    public void setCompressableMimeType(String compressableMimeType) {
        this.compressableMimeType = compressableMimeType;
    }
}
