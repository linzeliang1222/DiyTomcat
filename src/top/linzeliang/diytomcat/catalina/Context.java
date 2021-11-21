package top.linzeliang.diytomcat.catalina;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import org.apache.jasper.JspC;
import org.apache.jasper.compiler.JspRuntimeContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import sun.reflect.misc.ReflectUtil;
import top.linzeliang.diytomcat.classloader.WebappClassLoader;
import top.linzeliang.diytomcat.exception.WebConfigDuplicateException;
import top.linzeliang.diytomcat.http.ApplicationContext;
import top.linzeliang.diytomcat.http.StandardFilterConfig;
import top.linzeliang.diytomcat.http.StandardServletConfig;
import top.linzeliang.diytomcat.utils.ContextXMLUtil;
import top.linzeliang.diytomcat.watcher.ContextFileChangeWatcher;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.util.*;

/**
 * @Description: web应用
 * @Author: LinZeLiang
 * @Date: 2021-06-18
 */
public class Context {
    /**
     *
     */
    private String path;

    /**
     * 应用目录
     */
    private String docBase;

    /**
     * 应用的web.xml文件
     */
    private File contextWebXmlFile;

    /**
     * 所属的主机host
     */
    private Host host;

    /**
     * 是否支持热部署
     */
    private boolean reloadable;

    /**
     * 文件改变监听器
     */
    private ContextFileChangeWatcher contextFileChangeWatcher;

    /**
     * ServletContext属性
     */
    private ServletContext servletContext;

    /**
     * 使用一个map作为存放servlet的池
     */
    private Map<Class<?>, HttpServlet> servletPool;

    /**
     * 存放Servlet初始化的参数
     */
    private Map<String, Map<String, String>> servletClassNameInitParams;

    /**
     * url对应的Servlet类名，url-pattern -> servlet-class
     */
    private Map<String, String> urlServletClassName;

    /**
     * url对应的Servlet名称，路径url-pattern和servlet名字servlet-name相对应
     */
    private Map<String, String> urlServletName;

    /**
     * Servlet名称对应的类名，servlet-name -> servlet-class
     */
    private Map<String, String> servletNameToClassName;

    /**
     * Servlet类名对应的名称，servlet-class -> servlet-name
     */
    private Map<String, String> classNameToServletName;

    /**
     * WebappClassLoader类加载器
     */
    private WebappClassLoader webappClassLoader;

    /**
     * 自启动的Servlet集合
     */
    private List<String> loadOnStartupServletClassNames;

    /**
     * url对应的Filter类名，url-pattern -> filter-class
     */
    private Map<String, List<String>> urlFilterClassName;

    /**
     * url对应的Filter名称，路径url-pattern和filter名字filter-name相对应
     */
    private Map<String, List<String>> urlFilterName;

    /**
     * Filter名称对应的类名，filter-name -> filter-class
     */
    private Map<String, String> filterNameToClassName;

    /**
     * Filter类名对应的名称，filter-class -> filter-name
     */
    private Map<String, String> classNameToFilterName;

    /**
     * 存放Filter初始化的参数
     */
    private Map<String, Map<String, String>> filterClassNameInitParams;

    /**
     * filter池
     */
    private Map<String, Filter> filterPool;

    private List<ServletContextListener> listeners;


    public Context(String path, String docBase, Host host, boolean reloadable) {
        // 开始计时
        TimeInterval timeInterval = DateUtil.timer();

        this.path = path;
        this.docBase = docBase;
        this.contextWebXmlFile = new File(docBase, ContextXMLUtil.getWatchedResource());
        this.host = host;
        this.reloadable = reloadable;
        this.servletContext = new ApplicationContext(this);
        this.servletPool = new HashMap<>();
        this.loadOnStartupServletClassNames = new ArrayList<>();
        this.filterPool = new HashMap<>();
        this.listeners = new ArrayList<>();

        // web.xml中Servlet初始化
        urlServletClassName = new HashMap<>();
        urlServletName = new HashMap<>();
        servletNameToClassName = new HashMap<>();
        classNameToServletName = new HashMap<>();
        servletClassNameInitParams = new HashMap<>();

        // web.xml中Filter初始化
        urlFilterClassName = new HashMap<>();
        urlFilterName = new HashMap<>();
        filterNameToClassName = new HashMap<>();
        classNameToFilterName = new HashMap<>();
        filterClassNameInitParams = new HashMap<>();

        // 在构造器中初始化ClassLoader
        // 这里的Thread.currentThread().getContextClassLoader()就可以获取到Bootstrap里通过Thread.currentThread().setContextClassLoader(commonClassLoader);设置的 commonClassLoader.
        // 根据Tomcat类加载器体系commonClassLoader作为WebappClassLoader父类存在
        ClassLoader commonClassLoader = Thread.currentThread().getContextClassLoader();
        this.webappClassLoader = new WebappClassLoader(this.docBase, commonClassLoader);

        // 输出日志
        LogFactory.get().info("Deploying web application directory {}", this.docBase);
        deploy();
        LogFactory.get().info("Deployment of web application directory {} has finished in {} ms", this.docBase, timeInterval.intervalMs());
    }

    /**
     * 进行部署
     */
    private void deploy() {
        // 加载监听器
        loadListeners();

        // 初始化
        init();
        // 热部署监听
        if (reloadable) {
            // 创建监听器
            contextFileChangeWatcher = new ContextFileChangeWatcher(this);
            // 启动
            contextFileChangeWatcher.start();
        }

        // JspRuntimeContext的初始化
        // 在jsp所转换的java文件里的javax.servlet.jsp.JspFactory.getDefaultFactory()这行能够有返回值
        JspC jspC = new JspC();
        new JspRuntimeContext(servletContext, jspC);
    }

    /**
     * 停止监听
     */
    public void stop() {
        // 释放webappClassLoader类加载器
        webappClassLoader.stop();
        // 停止监听
        contextFileChangeWatcher.stop();
        // 销毁所有的servlet
        destroyServlets();

        // destory监听器
        fireEvent("destroy");
    }

    /**
     *
     */
    public void reload() {
        host.reload(this);
    }

    /**
     * 进行初始化
     */
    private void init() {
        // 如果不存在web.xml直接结束
        if (!contextWebXmlFile.exists()) {
            return;
        }

        try {
            // 检测是否有重复
            checkDuplicated();
        } catch (WebConfigDuplicateException e) {
            e.printStackTrace();
            return;
        }

        // 解析应用下的web.xml文件
        String xml = FileUtil.readUtf8String(contextWebXmlFile);
        Document document = Jsoup.parse(xml);
        // 解析web.xml中的servlet映射
        parseServletMapping(document);
        // 解析web.xml，获取初始化参数
        parseServletInitParams(document);
        // 解析loadOnStartup
        parseLoadOnStartup(document);
        // 加载Servlet
        handleLoadOnStartup();
        // 解析Filter
        parseFilterMapping(document);
        // 解析Filter的初始化参数
        parseFilterInitParams(document);
        // 初始化Filter
        initFilter();

        // init监听器
        fireEvent("init");
    }

    /**
     * 判断是否有重复的元素
     */
    public void checkDuplicated(Document document, String mapping, String description) throws WebConfigDuplicateException {
        Elements elements = document.select(mapping);

        // 先把元素添加到集合中
        List<String> contents = new ArrayList<>();
        for (Element element : elements) {
            contents.add(element.text());
        }

        // 进行排序
        Collections.sort(contents);

        // 检查两临两个元素是否相同
        for (int i = 0; i < contents.size() - 1; i++) {
            String contentPre = contents.get(i);
            String contentNext = contents.get(i + 1);
            if (contentPre.equals(contentNext)) {
                // 存在重复的话就抛出异常
                throw new WebConfigDuplicateException(StrUtil.format(description, contentPre));
            }
        }
    }

    /**
     * 检测web.xml的servlet是否重复了
     */
    public void checkDuplicated() throws WebConfigDuplicateException {
        String xml = FileUtil.readUtf8String(contextWebXmlFile);
        Document document = Jsoup.parse(xml);

        checkDuplicated(document, "servlet-mapping url-pattern", "servlet url 重复，请保持其唯一性:{}");
        checkDuplicated(document, "servlet servlet-name", "servlet 名称重复，请保持其唯一性:{}");
        checkDuplicated(document, "servlet servlet-class", "servlet 类名重复，请保持气=其唯一性:{}");
    }

    /**
     * 通过uri获取对应的servlet类名
     */
    public String getServletClassName(String uri) {
        return urlServletClassName.get(uri);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    /**
     * 获取应用地址
     *
     * @return String 应用地址
     */
    public String getDocBase() {
        return docBase;
    }

    public void setDocBase(String docBase) {
        this.docBase = docBase;
    }

    /**
     * 一个Web应用，应该有自己独立的WebappClassLoader，所以在Context里加上webappClassLoader属性
     */
    public WebappClassLoader getWebappClassLoader() {
        return webappClassLoader;
    }

    public boolean isReloadable() {
        return reloadable;
    }

    public void setReloadable(boolean reloadable) {
        this.reloadable = reloadable;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    /**
     * 根据类对象获取Servlet对象
     */
    public synchronized HttpServlet getServlet(Class<?> clazz) throws IllegalAccessException, InstantiationException, ServletException {
        // 先尝试从servletPool中获取
        HttpServlet servlet = servletPool.get(clazz);

        if (null == servlet) {
            // 如果没有对应的servlet对象，就创建一个新的，同时存放到servletPool
            servlet = (HttpServlet) clazz.newInstance();
            // 获取ServletContext
            ServletContext servletContext = this.getServletContext();
            String className = clazz.getName();
            // 通过className获取servletName
            String servletName = classNameToServletName.get(className);
            // 通过className获取初始化参数的map集合
            Map<String, String> initParameters = servletClassNameInitParams.get(className);
            // 创建ServletConfig
            StandardServletConfig servletConfig = new StandardServletConfig(servletContext, servletName, initParameters);
            // 为servlet初始化servletConfig
            servlet.init(servletConfig);
            // 将servlet添加到servletPool
            servletPool.put(clazz, servlet);
        }

        return servlet;
    }

    /**
     * parseServletMapping 方法，把这些信息从 web.xml 中解析出来
     */
    private void parseServletMapping(Document document) {
        // 解析配置urlServletName
        Elements mappingUrlElements = document.select("servlet-mapping url-pattern");
        for (Element mappingUrlElement : mappingUrlElements) {
            // 获取路径
            String urlPattern = mappingUrlElement.text();
            // 获取映射的类名
            String servletName = mappingUrlElement.parent().selectFirst("servlet-name").text();
            // 添加到集合中
            urlServletName.put(urlPattern, servletName);
        }

        // 解析servlet元素，配置servletNameClassName、classNameServletName
        Elements servletNameElements = document.select("servlet servlet-name");
        for (Element servletNameElement : servletNameElements) {
            String servletName = servletNameElement.text();
            String servletClass = servletNameElement.parent().selectFirst("servlet-class").text();
            servletNameToClassName.put(servletName, servletClass);
            classNameToServletName.put(servletClass, servletName);
        }

        // 配置url-pattern对应的servlet-class
        Set<Map.Entry<String, String>> urls = urlServletName.entrySet();
        for (Map.Entry<String, String> url : urls) {
            String servletClassName = servletNameToClassName.get(url.getValue());
            urlServletClassName.put(url.getKey(), servletClassName);
        }
    }

    /**
     * 从web.xml中解析初始化参数
     */
    private void parseServletInitParams(Document document) {
        Elements servletClassNameElements = document.select("servlet-class");
        // 查找每个servlet的初始化参数
        for (Element servletClassNameElement : servletClassNameElements) {
            // 先获取全限定类名
            String servletClassName = servletClassNameElement.text();
            // 获取该servlet下的所有init-param
            Elements initParamElements = servletClassNameElement.parent().select("init-param");
            // 如果没有初始化参数忽略
            if (initParamElements.isEmpty()) {
                continue;
            }
            // 使用一个map存放初始化参数
            HashMap<String, String> initParam = new HashMap<>();
            for (Element initParamElement : initParamElements) {
                // 获取参数名称
                String paramName = initParamElement.selectFirst("param-name").text();
                // 获取参数值
                String paramValue = initParamElement.selectFirst("param-value").text();
                initParam.put(paramName, paramValue);
            }
            // 将全限定类名和存放参数的map绑定到了一起
            servletClassNameInitParams.put(servletClassName, initParam);
        }
    }

    /**
     * 销毁所有的servlet
     */
    private void destroyServlets() {
        Collection<HttpServlet> servlets = servletPool.values();
        for (HttpServlet servlet : servlets) {
            servlet.destroy();
        }
    }

    /**
     * 解析web.xml，获取自启动的servlet
     */
    public void parseLoadOnStartup(Document document) {
        Elements elements = document.select("load-on-startup");
        for (Element element : elements) {
            String loadOnStartupServletClassName = element.parent().select("servlet-class").text();
            int loadOnStartupValue = Integer.parseInt(element.text());
            if (loadOnStartupValue >= 0) {
                loadOnStartupServletClassNames.add(loadOnStartupServletClassName);
            }
        }
    }

    public void handleLoadOnStartup() {
        for (String loadOnStartupServletClassName : loadOnStartupServletClassNames) {
            try {
                // 加载Servlet
                Class<?> clazz = webappClassLoader.loadClass(loadOnStartupServletClassName);
                getServlet(clazz);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (ServletException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 解析web.xml中的Filter信息
     */
    private void parseFilterMapping(Document document) {
        // 解析url对应的FilterName
        Elements mappingUrlElements = document.select("filter-mapping url-pattern");
        for (Element mappingUrlElement : mappingUrlElements) {
            String urlPattern = mappingUrlElement.text();
            String filterName = mappingUrlElement.parent().selectFirst("filter-name").text();

            List<String> filterNames = urlFilterName.get(urlPattern);
            if (null == filterNames) {
                filterNames = new ArrayList<>();
                urlFilterName.put(urlPattern, filterNames);
            }

            filterNames.add(filterName);
        }

        // 解析filterName对应的filterClass，以及filterClass对应的filterName
        Elements filterNameElements = document.select("filter filter-name");
        for (Element filterNameElement : filterNameElements) {
            String filterName = filterNameElement.text();
            String filterClass = filterNameElement.parent().selectFirst("filter-class").text();
            filterNameToClassName.put(filterName, filterClass);
            classNameToFilterName.put(filterClass, filterName);
        }

        // 解析url对应的filterClass
        Set<String> urls = urlFilterName.keySet();
        for (String url : urls) {
            List<String> filterNames = urlFilterName.get(url);
            if (null == filterNames) {
                filterNames = new ArrayList<>();
                urlFilterClassName.put(url, filterNames);
            }
            for (String filterName : filterNames) {
                String filterClassName = filterNameToClassName.get(filterName);
                List<String> filterClassNames = this.urlFilterClassName.get(url);
                if (null == filterClassNames) {
                    filterClassNames = new ArrayList<>();
                    urlFilterClassName.put(url, filterClassNames);
                }
                filterClassNames.add(filterClassName);
            }
        }
    }

    /**
     * 解析Filter的参数信息
     */
    private void parseFilterInitParams(Document document) {
        Elements filterClassNameElements = document.select("filter-class");
        for (Element filterClassNameElement : filterClassNameElements) {
            String filterClassName = filterClassNameElement.text();

            Elements initElements = filterClassNameElement.parent().select("init-param");
            if (initElements.isEmpty()) {
                continue;
            }

            HashMap<String, String> initParams = new HashMap<>();
            for (Element initElement : initElements) {
                String paramName = initElement.selectFirst("param-name").text();
                String paramValue = initElement.selectFirst("param-value").text();
                initParams.put(paramName, paramValue);
            }
            filterClassNameInitParams.put(filterClassName, initParams);
        }
    }

    /**
     * 初始化Filter，存放到filterPool池中
     */
    private void initFilter() {
        // 获取所有的类名
        Set<String> classNames = classNameToFilterName.keySet();
        for (String className : classNames) {
            try {
                // 通过类名加载类
                Class<?> clazz = this.getWebappClassLoader().loadClass(className);
                // 通过类名获取初始化参数集合
                Map<String, String> initParameters = filterClassNameInitParams.get(className);
                // 通过类名获取filter名称
                String filterName = classNameToFilterName.get(className);
                // 创建FilterConfig
                FilterConfig filterConfig = new StandardFilterConfig(servletContext, initParameters, filterName);
                // 通过类名从filterPool中获取Filter
                Filter filter = filterPool.get(className);
                // 如果不存在就创建新的
                if (null == filter) {
                    filter = (Filter) ReflectUtil.newInstance(clazz);
                    filter.init(filterConfig);
                    filterPool.put(className, filter);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (ServletException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * urlPattern进行匹配
     */
    private boolean match(String pattern, String uri) {
        if (StrUtil.equals(pattern, uri)) {
            // 完全匹配
            return true;
        } else if (StrUtil.equals(pattern, "/*")) {
            // /*模式
            return true;
        } else if (StrUtil.startWith(pattern, "/*.")) {
            // 后缀名匹配 /*.jsp
            String patternExtName = StrUtil.subAfter(pattern, '.', false);
            String uriExtName = StrUtil.subAfter(uri, '.', false);
            if (StrUtil.equals(patternExtName, uriExtName)) {
                return true;
            }
            return true;
        }
        return false;
    }

    /**
     * 获取匹配了的过滤器集合
     */
    public List<Filter> getMatchedFilters(String uri) {
        List<Filter> filters = new ArrayList<>();
        // 获取所有的匹配规则
        Set<String> patterns = urlFilterClassName.keySet();
        Set<String> matchedPatterns = new HashSet<>();
        // 获取符合uri条件的pattern
        for (String pattern : patterns) {
            if (match(pattern , uri)) {
                matchedPatterns.add(pattern);
            }
        }

        Set<String> matchedFilterClassNames = new HashSet<>();
        // 遍历符合条件的pattern，存储对应的全类名，一个pattern可以对应多个类
        for (String matchedPattern : matchedPatterns) {
            List<String> filterClassName = urlFilterClassName.get(matchedPattern);
            matchedFilterClassNames.addAll(filterClassName);
        }

        // 遍历符合条件的所有全类名，通过全限定名从filterPool获取Filter
        for (String filterClassName : matchedFilterClassNames) {
            Filter filter = filterPool.get(filterClassName);
            filters.add(filter);
        }

        return filters;
    }

    public void addListener(ServletContextListener listener) {
        listeners.add(listener);
    }

    /**
     * 加载监听器
     */
    private void loadListeners() {
        try {
            if (!contextWebXmlFile.exists()) {
                return;
            }
            String xml = FileUtil.readUtf8String(contextWebXmlFile);
            Document document = Jsoup.parse(xml);

            Elements elememts = document.select("listener listener-class");
            for (Element elememt : elememts) {
                String listenerClassName = elememt.text();

                Class<?> clazz = this.getWebappClassLoader().loadClass(listenerClassName);
                ServletContextListener listener = (ServletContextListener)clazz.newInstance();
                addListener(listener);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
    }

    /**
     * fireEvent方法，判断是初始化还是销毁
     */
    private void fireEvent(String type) {
        ServletContextEvent event = new ServletContextEvent(servletContext);
        for (ServletContextListener listener : listeners) {
            if ("init".equals(type)) {
                listener.contextInitialized(event);
            } else if ("destroy".equals(type)) {
                listener.contextDestroyed(event);
            }
        }
    }
}