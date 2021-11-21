package top.linzeliang.diytomcat.http;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description: 实现Session功能
 * @Author: LinZeLiang
 * @Date: 2021-07-22
 */
public class StandardSession implements HttpSession {

    /**
     * 存放Session的数据
     */
    private Map<String, Object> attributesMap;

    /**
     * 每一个Session都有一个唯一的id
     */
    private String id;

    /**
     * 创建时间
     */
    private long creationTime;

    /**
     * 最后一次访问时间
     */
    private long lastAccessedTime;

    /**
     * ServletContext
     */
    private ServletContext servletContext;

    /**
     * 最大持续时间的分钟数，默认30分钟
     */
    private int maxInactiveInterval;

    public StandardSession(String jSessionId, ServletContext servletContext) {
        this.attributesMap = new HashMap<>();
        this.id = jSessionId;
        this.creationTime = System.currentTimeMillis();
        this.servletContext = servletContext;
    }

    @Override
    public long getCreationTime() {
        return this.creationTime;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public long getLastAccessedTime() {
        return this.lastAccessedTime;
    }

    public void setLastAccessedTime(long lastAccessedTime) {
        this.lastAccessedTime = lastAccessedTime;
    }

    @Override
    public ServletContext getServletContext() {
        return this.servletContext;
    }

    @Override
    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    @Override
    public void setMaxInactiveInterval(int i) {
        this.maxInactiveInterval = i;
    }

    @Override
    public Object getAttribute(String s) {
        return attributesMap.get(s);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributesMap.keySet());
    }

    @Override
    public void setAttribute(String s, Object o) {
        attributesMap.put(s, o);
    }

    @Override
    public void removeAttribute(String s) {
        attributesMap.remove(s);
    }

    /**
     * 清空session内容
     */
    @Override
    public void invalidate() {
        attributesMap.clear();
    }

    @Override
    public boolean isNew() {
        return creationTime == lastAccessedTime;
    }

    @Override
    public Object getValue(String s) {
        return null;
    }

    @Override
    public String[] getValueNames() {
        return new String[0];
    }

    @Override
    public void putValue(String s, Object o) {
    }

    @Override
    public void removeValue(String s) {
    }

    @Override
    public HttpSessionContext getSessionContext() {
        return null;
    }
}
