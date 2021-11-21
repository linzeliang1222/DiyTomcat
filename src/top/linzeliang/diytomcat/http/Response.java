package top.linzeliang.diytomcat.http;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;

import javax.servlet.http.Cookie;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * @Description: Response对象
 * @Author: LinZeLiang
 * @Date: 2021-06-17
 */
public class Response extends BaseResponse {

    /**
     * 用于将printerWriter写入数据的转换成字符串
     */
    private StringWriter stringWriter;

    /**
     * 用于写出到stringWriter
     */
    private PrintWriter writer;

    /**
     * contentType类型
     */
    private String contentType;

    /**
     * 响应体内容
     */
    private byte[] body;

    /**
     * 响应状态码
     */
    private int status;

    /**
     * 存放Cookie的集合
     */
    private List<Cookie> cookies;

    /**
     * 保存客户端的跳转地址
     */
    private String redirectPath;

    public Response() {
        this.stringWriter = new StringWriter();
        this.writer = new PrintWriter(stringWriter);
        this.contentType = "text/html";
        this.cookies = new ArrayList<>();
    }

    /**
     * 返回内容类型
     */
    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * 获取PrinterWriter，便于打印response.getWriter().println();
     */
    @Override
    public PrintWriter getWriter() {
        return writer;
    }

    /**
     * 获取字节数组的内容
     */
    public byte[] getBody() throws UnsupportedEncodingException {
        if (body == null) {
            // 获取内容类型
            String content = stringWriter.toString();
            // 将内容类型转换成字节数组，编码格式为utf-8
            body = content.getBytes("utf-8");
        }

        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    @Override
    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }

    public List<Cookie> getCookies() {
        return this.cookies;
    }

    /**
     * 把Cookie集合转换成cookieHeader
     */
    public String getCookiesHeader() {
        // cookies为空返回字符串
        if (null == cookies || 0 == cookies.size()) {
            return "";
        }

        // 时间格式
        String pattern = "EEE, d MMM yyyy HH:mm:ss 'GMT'";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern, Locale.ENGLISH);
        StringBuffer stringBuffer = new StringBuffer();

        for (Cookie cookie : cookies) {
            stringBuffer.append("\r\n");
            // 为Response设置Cookie
            stringBuffer.append("Set-Cookie: ");
            stringBuffer.append(cookie.getName() + "=" + cookie.getValue() + ";");

            // 为Cookie设置有效时间
            // 为负数代表只是存在浏览器内存中，关闭浏览器就消失了，不会存到硬盘中；为0代表删除Cookie；为正整数就是在硬盘的保存时间
            if (cookie.getMaxAge() > 0) {
                // 设置cookie的有效期
                stringBuffer.append("Expires=");
                // 当前时间
                Date now = new Date();
                // 有效期时间
                DateTime expires = DateUtil.offset(now, DateField.SECOND, cookie.getMaxAge());
                stringBuffer.append(simpleDateFormat.format(expires) + ";");
            }

            // 为Cookie添加Path
            if (null != cookie.getPath()) {
                stringBuffer.append("Path=" + cookie.getPath());
            }
        }

        return stringBuffer.toString();
    }

    public String getRedirectPath() {
        return this.redirectPath;
    }

    /**
     * 重定向地址
     */
    @Override
    public void sendRedirect(String redirect) {
        this.redirectPath = redirect;
    }

    @Override
    public void resetBuffer() {
        this.stringWriter.getBuffer().setLength(0);
    }
}
