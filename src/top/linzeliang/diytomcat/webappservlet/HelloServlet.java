package top.linzeliang.diytomcat.webappservlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @Description: TODO
 * @Author: LinZeLiang
 * @Date: 2021-07-19
 */
public class HelloServlet extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            response.getWriter().println("HelloServlet!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
