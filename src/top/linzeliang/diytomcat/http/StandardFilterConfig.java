package top.linzeliang.diytomcat.http;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description: Filter加载初始化
 * @Author: LinZeLiang
 * @Date: 2021-07-24
 */
public class StandardFilterConfig implements FilterConfig {

    private ServletContext servletContext;
    private Map<String, String> initParameters;
    private String filterName;

    public StandardFilterConfig(ServletContext servletContext, Map<String, String> initParameters, String filterName) {
        this.servletContext = servletContext;
        this.initParameters = initParameters;
        this.filterName = filterName;
        if (null == initParameters) {
            initParameters = new HashMap<>();
        }
    }

    @Override
    public String getFilterName() {
        return this.filterName;
    }

    @Override
    public ServletContext getServletContext() {
        return this.servletContext;
    }

    @Override
    public String getInitParameter(String s) {
        return initParameters.get(s);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(initParameters.keySet());
    }
}
