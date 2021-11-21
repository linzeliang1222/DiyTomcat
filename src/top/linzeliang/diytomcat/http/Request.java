package top.linzeliang.diytomcat.http;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import top.linzeliang.diytomcat.catalina.Connector;
import top.linzeliang.diytomcat.catalina.Context;
import top.linzeliang.diytomcat.catalina.Engine;
import top.linzeliang.diytomcat.catalina.Service;
import top.linzeliang.diytomcat.utils.MiniBrowser;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;

/**
 * @Description: Request对象
 * @Author: LinZeLiang
 * @Date: 2021-06-17
 */
public class Request extends BaseRequest {

    /**
     * 请求内容
     */
    private String requestString;

    /**
     * 请求的uri
     */
    private String uri;

    /**
     * 套接字
     */
    private Socket socket;

    /**
     * 应用名称
     */
    private Context context;

    /**
     * 所属的Connector
     */
    private Connector connector;

    /**
     * 请求方法
     */
    private String method;

    /**
     * 查询字符串
     */
    private String queryString;

    /**
     * 存放参数的Map，Map里存放的值是字符串数组类型
     */
    private Map<String, String[]> parameterMap;

    /**
     * 存放头信息的Map
     */
    private Map<String, String> headerMap;

    /**
     * 存放请求时发送的cookie
     */
    private Cookie[] cookies;

    /**
     * jsessionid对应的session
     */
    private HttpSession session;

    /**
     * 标记是否服务端跳转
     */
    private boolean forwarded;

    /**
     * 服务端跳转时候的参数传递
     */
    private Map<String, Object> attributesMap;


    public Request(Socket socket, Connector connector) throws IOException {
        this.socket = socket;
        this.connector = connector;
        this.parameterMap = new HashMap<>();
        this.headerMap = new HashMap<>();
        this.attributesMap = new HashMap<>();

        // 解析http报文信息
        parseHttpRequest();
        // 如果请求信息是空的，那么就终止解析
        if (StrUtil.isEmpty(requestString)) {
            return;
        }
        // 解析uri
        parseUri();
        // 解析context
        parseContext();
        // 如果资源路径不是/，则需要先将uri中一处资源路径，然后得到的才是正确的uri
        if (!"/".equals(context.getPath())) {
            uri = StrUtil.removePrefix(uri, context.getPath());
            // 防止uri是空的情况
            if (StrUtil.isEmpty(uri)) {
                uri = "/";
            }
        }
        // 解析请求方法
        parseMethod();
        // 解析请求参数
        parseParameters();
        // 解析头信息
        parseHeaders();
        // 解析Cookie
        parseCookies();
    }

    /**
     * 解析获取应用
     */
    private void parseContext() {
        // 通过connector获取service
        Service service = connector.getService();
        // 通过service获取引擎
        Engine engine = service.getEngine();
        // 先通过uri进行匹配，这样/a就也可以匹配到context了
        context = engine.getDefaultHost().getContext(uri);
        // 检查是否context为空，为空就说明直接访问的是应用地址
        if (null != context) {
            return;
        }

        // 解析获取应用路径
        String path = StrUtil.subBetween(uri, "/", "/");
        if (null == path) {
            path = "/";
        } else {
            path = "/" + path;
        }

        // 从主机host中根据路径path获取对应的应用
        context = engine.getDefaultHost().getContext(path);
        if (null == context) {
            context = engine.getDefaultHost().getContext("/");
        }
    }

    /**
     * 获取请求信息
     */
    private void parseHttpRequest() throws IOException {
        // 获取客户端的请求输入流
        InputStream inputStream = socket.getInputStream();
        // 将请求输入流读取到字节数组中
        byte[] bytes = MiniBrowser.readBytes(inputStream, false);
        // 将字节数组转成utf8格式的字符串
        requestString = new String(bytes, "utf-8");
    }

    /**
     * 获取解析候得请求路径
     */
    private void parseUri() {
        // 获取uri
        String temp = StrUtil.subBetween(requestString, " ", " ");
        // 如果uri后面没有跟参数就直接获取
        if (!StrUtil.contains(temp, '?')) {
            uri = temp;
            return;
        }
        // 否则只获取参数前的uri
        uri = StrUtil.subBefore(temp, '?', false);
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getRequestString() {
        return requestString;
    }

    public Context getContext() {
        return context;
    }

    /**
     * 解析请求方法
     */
    private void parseMethod() {
        // 第一个空格前的就是请求方法
        method = StrUtil.subBefore(requestString, " ", false);
    }

    /**
     * 获取请求的方法
     */
    @Override
    public String getMethod() {
        return method;
    }

    /**
     * 获取ServletContext
     */
    @Override
    public ServletContext getServletContext() {
        return context.getServletContext();
    }

    /**
     * 获取真实路径
     */
    @Override
    public String getRealPath(String s) {
        return getServletContext().getRealPath(s);
    }

    /**
     * 根据名字获取请求参数
     */
    @Override
    public String getParameter(String name) {
        String[] values = parameterMap.get(name);
        if (null != values && 0 != values.length) {
            return values[0];
        }
        return null;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return parameterMap;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameterMap.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        return parameterMap.get(name);
    }

    /**
     * 根据不同请求方法来解析参数
     */
    public void parseParameters() {
        if ("GET".equals(this.getMethod())) {
            // 获取请求的url
            String url = StrUtil.subBetween(requestString, " ", " ");
            // GET方法的话直接取url的?后面的部分
            if (StrUtil.contains(url, '?')) {
                queryString = StrUtil.subAfter(url, "?", false);
            } else {
                queryString = null;
            }
        } else if ("POST".equals(this.getMethod())) {
            // POST请求方法，参数是在响应体里的，且在第一个空行\r\n\r\n后(\r\n是下一行，在\r\n就是再下一行，即空一行)
            queryString = StrUtil.subAfter(requestString, "\r\n\r\n", false);
        }

        // 如果没有参数直接结束
        if (null == queryString) {
            return;
        }

        // 先url解码
        queryString = URLUtil.decode(queryString);
        // 无论是GET还是POST参数都是用&分隔的
        String[] parameterValues = queryString.split("&");
        // 遍历每对参数
        for (String parameterValue : parameterValues) {
            String[] nameValues = parameterValue.split("=");
            // 长度为1说明不是一个完整的键值对，那就直接跳过
            if (nameValues.length == 1) {
                continue;
            } else {
                String name = nameValues[0];
                String value = nameValues[1];
                String[] values = parameterMap.get(name);
                if (null == values || 0 == values.length) {
                    values = new String[]{value};
                } else {
                    values = ArrayUtil.append(values, value);
                }
                // 添加到map中
                parameterMap.put(name, values);
            }
        }
    }

    /**
     * 根据name获取头信息
     */
    @Override
    public String getHeader(String name) {
        if (null == name) {
            return null;
        }
        name = name.toLowerCase();
        return headerMap.get(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(headerMap.keySet());
    }

    @Override
    public int getIntHeader(String name) {
        String value = headerMap.get(name);
        return Convert.toInt(value, 0);
    }

    /**
     * 解析头信息
     */
    public void parseHeaders() {
        // 使用StringReader来读取
        StringReader stringReader = new StringReader(requestString);
        // 每一行为一个元素
        List<String> lines = new ArrayList<>();
        IoUtil.readLines(stringReader, lines);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            // 代表读取到了空行，即头部信息读取完了
            if (0 == line.length()) {
                break;
            }
            // 提取分离头信息
            String[] segs = line.split(":");
            String headerName = segs[0].toLowerCase().trim();
            String headerValue = segs[1].trim();
            headerMap.put(headerName, headerValue);
        }
    }

    @Override
    public String getLocalAddr() {
        return socket.getLocalAddress().getHostAddress();
    }

    @Override
    public String getLocalName() {
        return socket.getLocalAddress().getHostName();
    }

    @Override
    public int getLocalPort() {
        return socket.getLocalPort();
    }

    @Override
    public String getProtocol() {
        return "HTTP:/1.1";
    }

    @Override
    public String getRemoteAddr() {
        InetSocketAddress isa = (InetSocketAddress) socket.getRemoteSocketAddress();
        String temp = isa.getAddress().toString();
        return StrUtil.subAfter(temp, "/", false);
    }

    @Override
    public String getRemoteHost() {
        InetSocketAddress isa = (InetSocketAddress) socket.getRemoteSocketAddress();
        return isa.getHostName();
    }

    @Override
    public int getRemotePort() {
        return socket.getPort();
    }

    @Override
    public String getScheme() {
        return "http";
    }

    @Override
    public String getServerName() {
        return getHeader("host").trim();
    }

    @Override
    public int getServerPort() {
        return getLocalPort();
    }

    @Override
    public String getContextPath() {
        String result = this.context.getPath();
        if ("/".equals(result)) {
            return "";
        }
        return result;
    }

    @Override
    public String getRequestURI() {
        return uri;
    }

    @Override
    public StringBuffer getRequestURL() {
        StringBuffer url = new StringBuffer();
        String scheme = getScheme();
        int port = getServerPort();
        if (port < 0) {
            port = 80;
        }
        url.append(scheme);
        url.append("://");
        url.append(getServerName());
        if ((scheme.equals("http") && (port != 80)) || (scheme.equals("https") && (port != 443))) {
            url.append(':');
            url.append(port);
        }
        url.append(getRequestURI());
        return url;
    }

    @Override
    public String getServletPath() {
        return uri;
    }

    @Override
    public Cookie[] getCookies() {
        return cookies;
    }

    /**
     * 解析请求报文中的cookie
     */
    public void parseCookies() {
        List<Cookie> cookieList = new ArrayList<>();
        String cookies = headerMap.get("cookie");
        // 只有浏览器传入cookie时才进行解析
        if (null != cookies) {
            String[] pairs = StrUtil.split(cookies, ";");
            // 遍历每个键值对
            for (String pair : pairs) {
                // 如果是无效字符串就不解析
                if (StrUtil.isBlank(pair) || !StrUtil.contains(pair, '=')) {
                    continue;
                }
                String[] segs = StrUtil.split(pair, "=");
                String name = segs[0].trim();
                String value = segs[1].trim();
                // 创建cookie
                Cookie cookie = new Cookie(name, value);
                cookieList.add(cookie);
            }
        }
        // 存储到request的cookies里
        this.cookies = ArrayUtil.toArray(cookieList, Cookie.class);
    }

    @Override
    public HttpSession getSession() {
        return session;
    }

    public void setSession(HttpSession session) {
        this.session = session;
    }

    /**
     * 获取jsessionid的值
     */
    public String getJSessionIdFromCookie() {
        if (null == cookies) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if ("JSESSIONID".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public Connector getConnector() {
        return this.connector;
    }

    public boolean isForwarded() {
        return forwarded;
    }

    public void setForwarded(boolean forwarded) {
        this.forwarded = forwarded;
    }

    public Socket getSocket() {
        return this.socket;
    }

    /**
     * 返回ApplicationRequestDispatcher对象
     */
    @Override
    public RequestDispatcher getRequestDispatcher(String uri) {
        return new ApplicationRequestDispatcher(uri);
    }

    @Override
    public Object getAttribute(String s) {
        return attributesMap.get(s);
    }
    @Override
    public void setAttribute(String s, Object o) {
        attributesMap.put(s, o);
    }

    @Override
    public void removeAttribute(String s) {
        attributesMap.remove(s);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributesMap.keySet());
    }
}
