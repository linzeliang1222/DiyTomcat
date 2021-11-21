package top.linzeliang.diytomcat.utils;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import top.linzeliang.diytomcat.http.Request;
import top.linzeliang.diytomcat.http.Response;
import top.linzeliang.diytomcat.http.StandardSession;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description: 用于管理StandardSession
 * @Author: LinZeLiang
 * @Date: 2021-07-22
 */
public class SessionManager {

    /**
     * 存放服务端的所有Session
     */
    private static Map<String, StandardSession> sessionMap = new ConcurrentHashMap<>();

    /**
     *
     */
    private static int defaultTimeout = getTimeout();

    static {
        startSessionOutdateCheckThread();
    }




    /**
     * 获取Session的默认失效时间，在web.xml中查找
     */
    private static int getTimeout() {
        // 默认时间
        int defaultResult = 30;
        // 获取配置文件中的时间
        try {
            Document document = Jsoup.parse(Constant.WEB_XML_FILE, "utf-8");
            Element element = document.selectFirst("session-config session-timeout");
            if (null == element) {
                return defaultResult;
            } else {
                return Convert.toInt(element.text());
            }
        } catch (IOException e) {
            // 解析文档出现异常时直接返回默认的时间
            return defaultResult;
        }
    }

    /**
     * 启动线程，每隔30秒调用一次，检测是否有失效的session
     */
    private static void startSessionOutdateCheckThread() {
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    // 检测失效session
                    checkOutDateSession();
                    // 睡眠30秒
                    ThreadUtil.sleep(1000* 30);
                }
            }
        }.start();
    }

    /**
     * 检测失效的session
     * 从sessionMap里根据lastAccessedTime筛选出过期的jsessionids,然后把他们从sessionMap里去掉
     */
    private static void checkOutDateSession() {
        Set<String> jSessionIds = sessionMap.keySet();
        ArrayList<String> outDateJSessionIds = new ArrayList<>();
        // 寻找无效的session
        for (String jSessionId : jSessionIds) {
            StandardSession session = sessionMap.get(jSessionId);
            // 获取时间间隔
            long interval = System.currentTimeMillis() - session.getLastAccessedTime();
            // 如果当前时间间隔大于最大的有效间隔，就是无效session
            if (interval >  session.getMaxInactiveInterval() * 60 * 1000) {
                outDateJSessionIds.add(jSessionId);
            }
        }
        // 将失效的session从sessionMap中移除
        for (Object jSessionId : outDateJSessionIds) {
            sessionMap.remove(jSessionId);
        }
    }

    /**
     * 生成随机sessionId
     */
    public static synchronized String generateSessionId() {
        String result = null;
        // 每一个byte范围-128~127
        byte[] bytes = RandomUtil.randomBytes(16);
        // 转换成对应的字符串，比如65对应A
        result = new String(bytes);
        // 将字符串结果进行MD5加密，得到32位的16进制字符串
        result = SecureUtil.md5(result);
        // 将小写字母转换成大写的
        result = result.toUpperCase();
        // 返回结果
        return result;
    }

    /**
     * 通过jsessionid获取session
     */
    public static HttpSession getSession(String jSessionId, Request request, Response response) {
        // 如果没有传入id的话，就生成一个新的Session并返回
        if (null == jSessionId) {
            return newSession(request, response);
        } else {
            // 根据id从sessionMap中获取session
            StandardSession currentSession = sessionMap.get(jSessionId);
            // 如果没有此id对应的session，也重新生成一个新的session
            if (null == currentSession) {
                return newSession(request, response);
            } else {
                // 设置最后访问时间为现在
                currentSession.setLastAccessedTime(System.currentTimeMillis());
                // 创建对应的cookie，将jsessionid发送给用户
                createCookieBySession(currentSession, request, response);
                return currentSession;
            }
        }
    }

    /**
     * 创建新的Session
     */
    private static HttpSession newSession(Request request, Response response) {
        // 创建session
        ServletContext servletContext = request.getServletContext();
        String jsSessionId = generateSessionId();
        StandardSession session = new StandardSession(jsSessionId, servletContext);
        // 设置session的最后访问时间为当前时间
        session.setLastAccessedTime(System.currentTimeMillis());
        // 设置session的存活时间
        session.setMaxInactiveInterval(defaultTimeout);
        // 将session存到sessionMap中
        sessionMap.put(jsSessionId, session);
        // 同时创建对应的cookie
        createCookieBySession(session, request, response);
        return session;
    }

    /**
     * 创建携带JSESSION的cookie
     */
    private static void createCookieBySession(HttpSession session, Request request, Response response) {
        // 创建cookie
        Cookie cookie = new Cookie("JSESSIONID", session.getId());
        cookie.setMaxAge(session.getMaxInactiveInterval());
        cookie.setPath(request.getContext().getPath());
        // 将cookie添加到响应中
        response.addCookie(cookie);
    }
}
