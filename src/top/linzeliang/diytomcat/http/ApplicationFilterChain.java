package top.linzeliang.diytomcat.http;

import cn.hutool.core.util.ArrayUtil;

import javax.servlet.*;
import java.io.IOException;
import java.util.List;

/**
 * @Description: Filter责任链
 * @Author: LinZeLiang
 * @Date: 2021-07-25
 */
public class ApplicationFilterChain implements FilterChain {

    private Filter[] filters;
    private Servlet servlet;
    private int position = 0;

    public ApplicationFilterChain(List<Filter> filters, Servlet servlet) {
        this.filters = ArrayUtil.toArray(filters, Filter.class);
        this.servlet = servlet;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        // 从第一个filter开始执行
        if (position < filters.length) {
            // 只要filter还没到底，就一直执行
            Filter filter = filters[position++];
            // 然后执行下一个filter，这里要传递本身的filter，然后在filter里面调用chain.doFilter再进入下一个filter
            filter.doFilter(request, response, this);
        } else {
            // filter到底了，就执行目标servlet
            servlet.service(request, response);
        }
    }
}
