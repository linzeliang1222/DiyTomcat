package top.linzeliang.diytomcat.exception;

/**
 * @Description: 自定义异常，配置 web.xml 里面发生 servlet 重复配置的时候会抛出
 * @Author: LinZeLiang
 * @Date: 2021-07-19
 */
public class WebConfigDuplicateException extends Exception {

    public WebConfigDuplicateException(String message) {
        super(message);
    }
}
