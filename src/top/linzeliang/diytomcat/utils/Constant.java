package top.linzeliang.diytomcat.utils;

import cn.hutool.system.SystemUtil;

import java.io.File;

/**
 * @Description: 静态数据
 * @Author: LinZeLiang
 * @Date: 2021-06-17
 */
public class Constant {
    /**
     * 200响应头公共代码
     */
    public static final String RESPONSE_HEAD_200 = "HTTP/1.1 200 OK\r\n" + "Content-Type: {}{}" + "\r\n\r\n";

    /**
     * 200响应头公共代码（数据被压缩）
     */
    public static final String RESPONSE_HEAD_200_GZIP = "HTTP/1.1 200 OK\r\nContent-Type: {}{}\r\n" + "Content-Encoding: gzip" + "\r\n\r\n";

    /**
     * 302响应体公共代码
     */

    public static final String RESPONSE_HEAD_302 = "HTTP/1.1 302 Found\r\nLocation: {}\r\n\r\n";

    /**
     * 404响应头公共代码
     */
    public static final String RESPONSE_HEAD_404 = "HTTP/1.1 404 Not Found\r\n" + "Content-Type: text/html\r\n\r\n";

    /**
     * 404模板代码
     */
    public static final String TEXT_FORMAT_404 =
            "<html><head><title>DIY Tomcat/1.0.1 - Error report</title><style>" +
                    "<!--H1 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:22px;} " +
                    "H2 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:16px;} " +
                    "H3 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:14px;} " +
                    "BODY {font-family:Tahoma,Arial,sans-serif;color:black;background-color:white;} " +
                    "B {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;} " +
                    "P {font-family:Tahoma,Arial,sans-serif;background:white;color:black;font-size:12px;}" +
                    "A {color : black;}A.name {color : black;}HR {color : #525D76;}--></style> " +
                    "</head><body><h1>HTTP Status 404 - {}</h1>" +
                    "<HR size='1' noshade='noshade'><p><b>type</b> Status report</p><p><b>message</b> <u>{}</u></p><p><b>description</b> " +
                    "<u>The requested resource is not available.</u></p><HR size='1' noshade='noshade'><h3>DiyTocmat 1.0.1</h3>" +
                    "</body></html>";

    /**
     * 500响应头公共代码
     */
    public static final String RESPONSE_HEAD_500 = "HTTP/1.1 500 Internal Server Error\r\n" + "Content-Type: text/html\r\n\r\n";

    /**
     * 500模板代码
     */
    public static final String TEXT_FORMAT_500 = "<html><head><title>DIY Tomcat/1.0.1 - Error report</title><style>"
            + "<!--H1 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:22px;} "
            + "H2 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:16px;} "
            + "H3 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:14px;} "
            + "BODY {font-family:Tahoma,Arial,sans-serif;color:black;background-color:white;} "
            + "B {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;} "
            + "P {font-family:Tahoma,Arial,sans-serif;background:white;color:black;font-size:12px;}"
            + "A {color : black;}A.name {color : black;}HR {color : #525D76;}--></style> "
            + "</head><body><h1>HTTP Status 500 - An exception occurred processing {}</h1>"
            + "<HR size='1' noshade='noshade'><p><b>type</b> Exception report</p><p><b>message</b> <u>An exception occurred processing {}</u></p><p><b>description</b> "
            + "<u>The server encountered an internal error that prevented it from fulfilling this request.</u></p>"
            + "<p>Stacktrace:</p>" + "<pre>{}</pre>" + "<HR size='1' noshade='noshade'><h3>DiyTocmat 1.0.1</h3>"
            + "</body></html>";

    /**
     * 包含用户站点的目录
     */
    public static final File WEBAPP_FOLDER = new File(SystemUtil.get("user.dir"), "webapps");

    /**
     * 默认ROOT站点目录
     */
    public static final File ROOT_FOLDER = new File(WEBAPP_FOLDER, "ROOT");

    /**
     * 配置文件目录
     */
    public static final File CONF_FOLDER = new File(SystemUtil.get("user.dir"), "conf");

    /**
     * server.xml配置文件路径
     */
    public static final File SERVER_XML_FILE = new File(CONF_FOLDER, "server.xml");

    /**
     * web.xml配置文件路径
     */
    public static final File WEB_XML_FILE = new File(CONF_FOLDER, "web.xml");

    /**
     * CONTEXT_XML_FILE常量
     */
    public static final File CONTEXT_XML_FILE = new File(CONF_FOLDER, "context.xml");

    /**
     * 200状态码
     */
    public static final int CODE_200 = 200;

    /**
     * 302状态码
     */
    public static final int CODE_302 = 302;

    /**
     * 404状态码
     */
    public static final int CODE_404 = 404;

    /**
     * 500状态码
     */
    public static final int CODE_500 = 500;

    /**
     * jsp被转译成为.java文件之后，会被保存在%TOMCAT_HOME%/work目录下
     */
    public static final String WORK_FOLDER = SystemUtil.get("user.dir") + File.separator + "work";
}
