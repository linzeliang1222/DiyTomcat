package top.linzeliang.diytomcat.catalina;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.log.LogFactory;
import top.linzeliang.diytomcat.http.ApplicationFilterChain;
import top.linzeliang.diytomcat.http.Request;
import top.linzeliang.diytomcat.http.Response;
import top.linzeliang.diytomcat.servlets.DefaultServlet;
import top.linzeliang.diytomcat.servlets.InvokerServlet;
import top.linzeliang.diytomcat.servlets.JspServlet;
import top.linzeliang.diytomcat.utils.Constant;
import top.linzeliang.diytomcat.utils.SessionManager;

import javax.servlet.Filter;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

/**
 * @Description: 处理Http请求
 * @Author: LinZeLiang
 * @Date: 2021-07-19
 */
public class HttpProcessor {

    public void execute(Socket socket, Request request, Response response) {
        try {
            // 获取uri
            String uri = request.getUri();
            if (null == uri) {
                return;
            }

            // 解析cookie，准备session
            prepareSession(request, response);

            Context context = request.getContext();
            String servletClassName = context.getServletClassName(uri);
            // 因为Servlet的service方法是在chain里面调用的，所以原来的调用去掉，并且用一个workingServlet分别指向它们
            HttpServlet workingServlet;

            if (null != servletClassName) {
                // 处理servlet
                // 反射交给InvokerServlet的service方法，这边只需调用，不过是单例的
                workingServlet = InvokerServlet.getInstance();
            } else if (uri.endsWith(".jsp")) {
                // 处理Jsp文件
                workingServlet = JspServlet.getInstance();
            } else {
                // 仅处理静态资源
                workingServlet = DefaultServlet.getInstance();
            }

            // 执行过滤器
            List<Filter> filters = request.getContext().getMatchedFilters(request.getRequestURI());
            ApplicationFilterChain filterChain = new ApplicationFilterChain(filters, workingServlet);
            filterChain.doFilter(request, response);

            // 如果是服务端请求转发就不进行后序处理了
            if (request.isForwarded()) {
                return;
            }

            if (Constant.CODE_200 == response.getStatus()) {
                // 返回结果的处理逻辑，200状态
                handle200(socket, request, response);
            } else if (Constant.CODE_302 == response.getStatus()) {
                // 状态码为302就要进行跳转
                handle302(socket, response);
            } else if (Constant.CODE_404 == response.getStatus()) {
                // 返回结果的处理逻辑，404状态，页面未找到
                handle404(socket, uri);
            }
        } catch (Exception e) {
            LogFactory.get().error(e);
            handle500(socket, e);
        }
    }

    /**
     * 处理200响应
     *
     * @param socket   客户端socket
     * @param response 响应体内容
     */
    private void handle200(Socket socket, Request request, Response response) throws IOException {
        // 获取Content-type
        String contentType = response.getContentType();
        // 获取响应体内容
        byte[] body = response.getBody();
        // 获取cookie信息
        String cookiesHeader = response.getCookiesHeader();
        // 定义响应头信息模板
        String headText;

        // 判断数据是否要压缩
        boolean gzip = isGzip(request, body, contentType);
        // 选取不同的头信息模板
        if (gzip) {
            headText = Constant.RESPONSE_HEAD_200_GZIP;
            // 进行压缩
            body = ZipUtil.gzip(body);
        } else {
            headText = Constant.RESPONSE_HEAD_200;
        }
        // 填充响应头的参数
        headText = StrUtil.format(headText, contentType, cookiesHeader);

        // 将头信息转换为字节数组
        byte[] head = headText.getBytes();
        // 新建一个数组,长度就是请求头+请求体
        byte[] responseBytes = new byte[head.length + body.length];
        // 将head和body都拷贝到同一个字节数组里
        ArrayUtil.copy(head, 0, responseBytes, 0, head.length);
        ArrayUtil.copy(body, 0, responseBytes, head.length, body.length);

        // 从socket获取一个输出流，将响应信息写入到socket中
        OutputStream outputStream = socket.getOutputStream();
        // 通过这个输出流将真正的返回结果输出给服务端
        outputStream.write(responseBytes);
        outputStream.flush();
        outputStream.close();
    }

    /**
     * 处理302响应
     *
     * @param socket   socket套接字
     * @param response 响应
     */
    private void handle302(Socket socket, Response response) throws IOException {
        OutputStream outputStream = socket.getOutputStream();

        String redirectPath = response.getRedirectPath();
        String headText = Constant.RESPONSE_HEAD_302;

        String header = StrUtil.format(headText, redirectPath);
        byte[] responseBytes = header.getBytes("utf-8");

        outputStream.write(responseBytes);
    }

    /**
     * 处理404响应
     *
     * @param socket 请求端socket
     * @param uri    请求的未找到资源的uri
     */
    private void handle404(Socket socket, String uri) throws IOException {
        // 获取响应的输出流
        OutputStream outputStream = socket.getOutputStream();
        // 获取404页面模板
        String responseText = StrUtil.format(Constant.TEXT_FORMAT_404, uri, uri);
        // 将404页面模板和404页面响应头信息进行拼接
        responseText = Constant.RESPONSE_HEAD_404 + responseText;
        // 将响应内容转换城utf8格式的字节数组
        byte[] responseBytes = responseText.getBytes("utf-8");
        // 将结果进行写入返回到页面
        outputStream.write(responseBytes);
    }

    /**
     * 处理500响应
     *
     * @param socket    请求端socket
     * @param exception 产生的异常
     */
    private void handle500(Socket socket, Exception exception) {
        try {
            // 获取响应输出流
            OutputStream outputStream = socket.getOutputStream();
            // 拿到exception的异常堆栈
            StackTraceElement[] stackTrace = exception.getStackTrace();
            // 创建StringBuffer存放异常信息
            StringBuffer stringBuffer = new StringBuffer();
            // 先把exception.toString()信息放进去
            stringBuffer.append(exception.toString());
            // 添加一个换行符
            stringBuffer.append("\r\n");
            // 挨个把堆栈里的信息放进去
            for (StackTraceElement stackTraceElement : stackTrace) {
                stringBuffer.append("\t");
                stringBuffer.append(stackTraceElement.toString());
                stringBuffer.append("\r\n");
            }

            // 获取异常的消息
            String msg = exception.getMessage();
            // 如果消息超过20个，且不为空，则进行截取
            if (null != msg && msg.length() > 20) {
                msg = msg.substring(0, 19);
            }

            // 将异常信息结合响应头和模板信息，响应给客户端
            String responseText = StrUtil.format(Constant.TEXT_FORMAT_500, msg, exception.toString(), stringBuffer.toString());
            responseText = Constant.RESPONSE_HEAD_500 + responseText;
            // 将响应内容转换成二进制格式进行传输
            byte[] responseBytes = responseText.getBytes("utf-8");
            // 进行响应
            outputStream.write(responseBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 准备Session
     */
    public void prepareSession(Request request, Response response) {
        String sid = request.getJSessionIdFromCookie();
        HttpSession session = SessionManager.getSession(sid, request, response);
        request.setSession(session);
    }

    /**
     * 判断是否要进行gzip压缩
     */
    private boolean isGzip(Request request, byte[] body, String mimeType) {
        // 如果客户端没有说明支持压缩的格式，那么就默认不支持
        String acceptEncodings = request.getHeader("Accept-Encoding");
        if (!StrUtil.containsAny(acceptEncodings, "gzip")) {
            return false;
        }

        Connector connector = request.getConnector();
        // mimeType有多个就选取第一个
        if (mimeType.contains(";")) {
            mimeType = StrUtil.subBefore(mimeType, ";", false);
        }
        // 检查server.xml配置文件中是否开启压缩
        if (!"on".equals(connector.getCompression())) {
            return false;
        }
        // 如果响应数据长度没有达到最小压缩的长度，那么就不压缩
        if (body.length < connector.getCompressionMinSize()) {
            return false;
        }

        // 检查对应该请求的浏览器的响应是否要进行压缩
        String userAgents = connector.getNoCompressionUserAgents();
        String[] eachUserAgents = userAgents.split(";");
        for (String eachUserAgent : eachUserAgents) {
            eachUserAgent = eachUserAgent.trim();
            String userAgent = request.getHeader("User-Agent");
            if (StrUtil.containsAny(userAgent, eachUserAgent)) {
                return false;
            }
        }
        // 检查mimeType类型是否支持压缩
        String mimeTypes = connector.getCompressableMimeType();
        String[] eachMimeTypes = mimeTypes.split(",");
        for (String eachMimeType : eachMimeTypes) {
            if (mimeType.equals(eachMimeType)) {
                return true;
            }
        }
        return false;
    }
}
