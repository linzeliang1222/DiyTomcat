package top.linzeliang.diytomcat.servlets;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import top.linzeliang.diytomcat.catalina.Context;
import top.linzeliang.diytomcat.classloader.JspClassLoader;
import top.linzeliang.diytomcat.http.Request;
import top.linzeliang.diytomcat.http.Response;
import top.linzeliang.diytomcat.utils.Constant;
import top.linzeliang.diytomcat.utils.JspUtil;
import top.linzeliang.diytomcat.utils.WebXMLUtil;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * @Description: 处理Jsp
 * @Author: LinZeLiang
 * @Date: 2021-07-24
 */
public class JspServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final JspServlet INSTANCE = new JspServlet();

    public static synchronized JspServlet getInstance() {
        return INSTANCE;
    }

    private JspServlet() {
    }

    @Override
    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        try {
            Request request = (Request) httpServletRequest;
            Response response = (Response) httpServletResponse;

            String uri = request.getRequestURI();

            // 如果填写的是主页根地址，就访问index文件
            if ("/".equals(uri)) {
                uri = WebXMLUtil.getWelcomeFile(request.getContext());
            }

            String fileName = StrUtil.removePrefix(uri, "/");
            File file = FileUtil.file(request.getRealPath(fileName));

            File jspFile = file;
            if (jspFile.exists()) {
                Context context = request.getContext();
                // path就是应用名称
                String path = context.getPath();
                String subFolder;
                // 确定work目录下的子目录
                if ("/".equals(path)) {
                    subFolder = "_";
                } else {
                    subFolder = StrUtil.subAfter(path, '/', false);
                }
                String servletClassPath = JspUtil.getServletClassPath(uri, subFolder);
                File jspServletClassFile = new File(servletClassPath);
                // 如果还没有编译成class文件就编译
                if (!jspServletClassFile.exists()) {
                    JspUtil.compileJsp(context, jspFile);
                } else if (jspFile.lastModified() > jspServletClassFile.lastModified()) {
                    // 如果jsp文件最后修改的时间大于class文件的修改时间，就重新编译
                    JspUtil.compileJsp(context, jspFile);
                    // 当发现jsp更新之后，就会调用invalidJspClassLoader是指与之前的JspClassLoader脱钩
                    JspClassLoader.invalidJspClassLoader(uri, context);
                }

                // 通过文件扩展名匹配mimeType
                String extName = FileUtil.extName(file);
                String mimeType = WebXMLUtil.getMimeType(extName);
                response.setContentType(mimeType);

                JspClassLoader jspClassLoader = JspClassLoader.getJspClassLoader(uri, context);
                String jspServletClassName = JspUtil.getJspServletClassName(uri, subFolder);
                Class<?> jspServletClass = jspClassLoader.loadClass(jspServletClassName);

                // 进入到jsp编译后的servlet中执行doGet、doPost方法
                HttpServlet servlet = context.getServlet(jspServletClass);
                servlet.service(request, response);

                // 设置状态码
                if (null != response.getRedirectPath()) {
                    response.setStatus(Constant.CODE_302);
                } else {
                    response.setStatus(Constant.CODE_200);
                }
            } else {
                response.setStatus(Constant.CODE_404);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
