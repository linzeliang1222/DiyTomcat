package top.linzeliang.diytomcat.http;

import top.linzeliang.diytomcat.catalina.Context;

import java.io.File;
import java.util.*;

/**
 * @Description: ApplicationContext
 * @Author: LinZeLiang
 * @Date: 2021-07-21
 */
public class ApplicationContext extends BaseServletContext {

    /**
     * 创建一个attributesMap属性，用于存放属性
     */
    private Map<String, Object> attributesMap;

    /**
     * 内置一个context
     */
    private Context context;

    public ApplicationContext(Context context) {
        this.attributesMap = new HashMap<>();
        this.context = context;
    }

    /**
     * 围绕 attributesMap重写一一批方法，这部分在jsp里的用法就是那个<% application.setAttribute()%>，那个application内置对象就是这个ApplicationContex
     * 删除元素
     */
    @Override
    public void removeAttribute(String name) {
        attributesMap.remove(name);
    }

    /**
     * 添加元素
     */
    @Override
    public void setAttribute(String name, Object value) {
        attributesMap.put(name, value);
    }

    /**
     * 获取元素
     */
    @Override
    public Object getAttribute(String name) {
        return attributesMap.get(name);
    }

    /**
     * 将属性的key转换成枚举类型
     */
    @Override
    public Enumeration<String> getAttributeNames() {
        Set<String> keys = attributesMap.keySet();
        return Collections.enumeration(keys);
    }

    /**
     * 工具path获取硬盘上的真实路径
     */
    @Override
    public String getRealPath(String path) {
        return new File(context.getDocBase(), path).getAbsolutePath();
    }
}
