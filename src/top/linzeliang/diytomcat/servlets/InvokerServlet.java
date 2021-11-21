package top.linzeliang.diytomcat.servlets;

import cn.hutool.core.util.ReflectUtil;
import top.linzeliang.diytomcat.catalina.Context;
import top.linzeliang.diytomcat.http.Request;
import top.linzeliang.diytomcat.http.Response;
import top.linzeliang.diytomcat.utils.Constant;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @Description: 处理Servlet，单例
 * @Author: LinZeLiang
 * @Date: 2021-07-19
 */
public class InvokerServlet extends HttpServlet {

    /**
     * 单例
     */
    private static InvokerServlet instance = new InvokerServlet();

    public static InvokerServlet getInstance() {
        return instance;
    }

    public InvokerServlet() {
    }

    @Override
    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
        // 进行强制转换，因为我们的Request和Response都是实现了HttpServletRequest和HttpServletRequest接口
        Request request = (Request)httpServletRequest;
        Response response = (Response)httpServletResponse;

        String uri = request.getUri();
        Context context = request.getContext();
        String servletClassName = context.getServletClassName(uri);

        try {
            // 通过反射获取类，执行service方法
            // 先获取Servlet类
            Class<?> servletClass = context.getWebappClassLoader().loadClass(servletClassName);
            // System.out.println("servletClass:" + servletClass);
            // System.out.println("servletClass'classLoader:" + servletClass.getClassLoader());
            // 获取类对象
            Object servletObject = context.getServlet(servletClass);
            // 反射调用方法
            ReflectUtil.invoke(servletObject, "service", request, response);

            // 有跳转就准备跳转
            if (null != response.getRedirectPath()) {
                response.setStatus(Constant.CODE_302);
            } else {
                // 设置状态码
                response.setStatus(Constant.CODE_200);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
    }
}
