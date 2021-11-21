package top.linzeliang.diytomcat.http;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description: ServletConfig是在Servlet初始化的时候，传递进去的参数对象
 * @Author: LinZeLiang
 * @Date: 2021-07-21
 */
public class StandardServletConfig implements ServletConfig {

    private ServletContext servletContext;
    private String servletName;
    private Map<String, String> initParameters;


    public StandardServletConfig(ServletContext servletContext, String servletName, Map<String, String> initParameters) {
        this.servletContext = servletContext;
        this.servletName = servletName;
        this.initParameters = initParameters;
        if (null == initParameters) {
            this.initParameters = new HashMap<>();
        }
    }

    @Override
    public String getServletName() {
        return servletName;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
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
