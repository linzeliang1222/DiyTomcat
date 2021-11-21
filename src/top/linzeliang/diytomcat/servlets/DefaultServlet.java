package top.linzeliang.diytomcat.servlets;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import top.linzeliang.diytomcat.catalina.Context;
import top.linzeliang.diytomcat.http.Request;
import top.linzeliang.diytomcat.http.Response;
import top.linzeliang.diytomcat.utils.Constant;
import top.linzeliang.diytomcat.utils.WebXMLUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * @Description: 处理静态资源
 * @Author: LinZeLiang
 * @Date: 2021-07-19
 */
public class DefaultServlet extends HttpServlet {
    /**
     * 单例
     */
    private static final DefaultServlet INSTANCE = new DefaultServlet();

    public static DefaultServlet getInstance() {
        return INSTANCE;
    }

    public DefaultServlet() {
    }

    @Override
    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
        Request request = (Request)httpServletRequest;
        Response response = (Response)httpServletResponse;

        Context context = request.getContext();

        String uri = request.getUri();
        if ("/500.html".equals(uri)) {
            throw new RuntimeException("this is a deliberately create exception!");
        } else {
            // 当 uri 等于 "/" 的时候， uri 就修改成欢迎文件，后面就当作普通文件来处理了
            if ("/".equals(uri)) {
                uri = WebXMLUtil.getWelcomeFile(request.getContext());
            }

            // 如果发现是jsp文件，那就交给JspServlet来处理
            if (uri.endsWith(".jsp")) {
                JspServlet.getInstance().service(request, response);
                return;
            }

            // 获取文件名
            String fileName = StrUtil.removePrefix(uri, "/");
            // 加上资源路径获取文件
            File file = FileUtil.file(request.getRealPath(fileName));

            // 判断是不是文件
            if (!file.isFile()) {
                // 如果只输入多级目录，那么我们还需要添加上欢迎页文件名
                uri += "/" + WebXMLUtil.getWelcomeFile(request.getContext());
                fileName = StrUtil.removePrefix(uri, "/");
                file = new File(request.getRealPath(fileName));
            }

            // 判断文件是否存在
            if (file.exists()) {
                // 获取文件后缀名
                String extension = FileUtil.extName(file);
                // 获取mime类型
                String mimeType = WebXMLUtil.getMimeType(extension);
                // 设置contentType
                response.setContentType(mimeType);

                // 获取内容，直接读取成二进制文件添加到response中
                byte[] body = FileUtil.readBytes(file);
                response.setBody(body);

                // 如果请求的是该文件，就sleep 1秒
                if (fileName.equals("timeConsume.html")) {
                    ThreadUtil.sleep(1000);
                }

                response.setStatus(Constant.CODE_200);
            } else {
                // 不存在的话设置状态码为404
                response.setStatus(Constant.CODE_404);
            }
        }
    }
}
