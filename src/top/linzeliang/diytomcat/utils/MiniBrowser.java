package top.linzeliang.diytomcat.utils;

import cn.hutool.http.HttpUtil;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 注意:核心是方法:getHttpBytes,看懂这个方法就看懂全部了
 */
public class MiniBrowser {

    public static void main(String[] args) {
        //初始化请求地址,这个请求地址就是待会会去链接的地址
        String url = "http://static.how2j.cn/diytomcat.html";
        //调用该方法,获取 http请求返回值的返回内容(简而言之就是去除掉了返回头的那些字符串)(请进到这个调用方法继续看)
        String contentString = getContentString(url, false);
        System.out.println(contentString);

        System.out.println("------------");

        //这个方法就是获取全部的Http返回内容的字符串的方法了,各位看了刚才的一些解析这个方法就没啥好说的了
        String httpString = getHttpString(url, false);
        System.out.println(httpString);
    }

    public static byte[] getContentBytes(String url) {
        return getContentBytes(url, false, null, true);
    }

    public static byte[] getContentBytes(String url, boolean gzip) {
        return getContentBytes(url, gzip, null, true);
    }

    public static byte[] getContentBytes(String url, Map<String, Object> params, boolean isGet) {
        return getContentBytes(url, false, params, isGet);
    }

    public static byte[] getContentBytes(String url, boolean gzip, Map<String, Object> params, boolean isGet) {
        //这里是真正的逻辑,就是与请求地址建立连接的逻辑,是整个类的核心,其他方法都只是处理这个方法返回值的一些逻辑而已
        byte[] response = getHttpBytes(url, gzip, params, isGet);
        //这个doubleReturnq其实是这样来的:我们获取的返回值正常其实是这样的
        //也就是说响应头部分和具体内容部分其实隔了一行, \r表示回到行首\n表示换到下一行,那么\r\n就相当于说先到了空格一行的那一行的行首,接着又到了具体内容的那部分的行首
        byte[] doubleReturn = "\r\n\r\n".getBytes();

        //接着这里初始化一个记录值,做记录用,往下看
        int pos = -1;
        //开始遍历返回内容
        for (int i = 0; i < response.length - doubleReturn.length; i++) {
            //这里的意思就是不断去初始化一个数组(从原数组进行拷贝),目的其实是为了获取到\r\n这一行的起始位置
            byte[] temp = Arrays.copyOfRange(response, i, i + doubleReturn.length);

            //来到这里,就是比较内容,当走到这里,说明temp这个字节数组的内容就是\r\n\r\n的内容了,说明我们找到了他的其实位置
            if (Arrays.equals(temp, doubleReturn)) {
                //将pos等于i,记录位置
                pos = i;
                break;
            }
        }

        //如果没记录到,那就说明压根没具体内容,那其实就是null
        if (-1 == pos) {
            return null;
        }

        //接着pos就是\r\n\n的第一个\的这个位置,加上\r\n\r\n的长度,相当于来到了具体内容的其实位置
        pos += doubleReturn.length;

        //最后,确定了具体内容是在哪个字节开始,就拷贝这部分内容返回
        return Arrays.copyOfRange(response, pos, response.length);
    }

    public static String getContentString(String url) {
        return getContentString(url, false, null, true);
    }

    public static String getContentString(String url, boolean gzip) {
        return getContentString(url, gzip, null, true);
    }

    public static String getContentString(String url, Map<String, Object> params, boolean isGet) {
        return getContentString(url, false, params, isGet);
    }

    public static String getContentString(String url, boolean gzip, Map<String, Object> params, boolean isGet) {
        //这里获取返回体具体内容的字节数组,请跟进去看
        byte[] result = getContentBytes(url, gzip, params, isGet);
        //getContentString 表示获取内容的字符串,我们获取到具体内容的字节数组后还需要进行编码
        if (null == result) {
            return null;
        }
        //这里就是一个编码过程了,我这里跟源代码不同,用StandarCahrset这个类可以避免抛异常,这里引入一个知识,因为这个是个常量,jvm可以知道你会选的是utf-8,所以不要求你抛异常
        return new String(result, StandardCharsets.UTF_8).trim();
    }

    public static String getHttpString(String url) {
        return getHttpString(url, false, null, true);
    }

    public static String getHttpString(String url, boolean gzip) {
        return getHttpString(url, gzip, null, true);
    }

    public static String getHttpString(String url, Map<String, Object> params, boolean isGet) {
        return getHttpString(url, false, params, isGet);
    }

    public static String getHttpString(String url, boolean gzip, Map<String, Object> params, boolean isGet) {
        byte[] bytes = getHttpBytes(url, gzip, params, isGet);
        return new String(bytes).trim();
    }


    public static byte[] getHttpBytes(String url, boolean gzip, Map<String, Object> params, boolean isGet) {
        // 获取请求方法
        String method = isGet ? "GET" : "POST";
        //首先初始化一个返回值,这个返回值是一个字节数组,utf-8编码的
        byte[] result = null;

        try {
            //通过url来new一个URL对象,这样你就不用自己去截取他的端口啊或者请求路径啥的,可以直接调他的方法获取
            URL u = new URL(url);
            //开启一个socket链接,client指的就是你现在的这台计算机
            Socket client = new Socket();
            //获取到端口号,要是端口号是-1,那就默认取80端口(这个端口也是web常用端口)
            int port = u.getPort();
            if (port == -1) {
                port = 80;
            }
            //这个是socket编程的内容,简单来说就是通过一个host+端口,和这个url建立连接
            InetSocketAddress inetSocketAddress = new InetSocketAddress(u.getHost(), port);
            //开始连接了,1000是超时时间,等于说超过1秒就算你超时了
            client.connect(inetSocketAddress, 1000);
            //初始化请求头
            Map<String, String> requestHeaders = new HashMap<>();

            //这几个参数都是http请求时会带上的请求头
            requestHeaders.put("Host", u.getHost() + ":" + port);
            requestHeaders.put("Accept", "text/html");
            requestHeaders.put("Connection", "close");
            requestHeaders.put("User-Agent", "my mini browser / java1.8");

            //gzip是确定客户端或服务器端是否支持压缩
            if (gzip) {
                requestHeaders.put("Accept-Encoding", "gzip");
            }

            //获取到path,其实就是/diytomcat.html,如果没有的话就默认是/
            String path = u.getPath();
            if (path.length() == 0) {
                path = "/";
            }

            // 判断是否为get请求，且参数是否存在
            if (null != params && isGet) {
                String paramsString = HttpUtil.toParams(params);
                path = path + "?" + paramsString;
            }

            //接着开始拼接请求的字符串,其实所谓的请求头和请求内容就是这么一串字符串拼接出来
            String firstLine = method + " " + path + " HTTP/1.1\r\n";

            StringBuffer httpRequestString = new StringBuffer();
            //拼接firstLine的内容
            httpRequestString.append(firstLine);
            Set<String> headers = requestHeaders.keySet();

            //遍历header的那个map进行拼接
            for (String header : headers) {
                String headerLine = header + ":" + requestHeaders.get(header) + "\r\n";
                httpRequestString.append(headerLine);
            }

            // 如果参数不为空，且不是get请求，则为post，就将参数放到请求体中
            if (null != params && !isGet) {
                String paramsString = HttpUtil.toParams(params);
                httpRequestString.append("\r\n");
                httpRequestString.append(paramsString);
            }

            /**走到这的时候,httpRequestString已经拼接好了,内容是:
             GET /diytomcat.html HTTP/1.1
             Accept:text/html
             Connection:close
             User-Agent:how2j mini browser / java1.8
             Host:static.how2j.cn:80
             */
            //通过输出流,将这么一串字符串输出给连接的地址,后面的true是autoFlush,表示开始自动刷新
            PrintWriter printWriter = new PrintWriter(client.getOutputStream(), true);
            printWriter.println(httpRequestString);
            //这时候你已经将需要的请求所需的字符串发给上面那个url了,其实所谓的http协议就是这样,你发给他这么一串符合规范的字符串,他就给你响应,接着他那边就给你返回数据
            //所以这时候我们开启一个输出流
            InputStream inputStream = client.getInputStream();

            result = readBytes(inputStream, true);
            //这是个好习惯,不过最好是放在finally进行关闭比较好,这里就是关闭连接了
            client.close();
        } catch (Exception e) {
            //接着这里是异常打印
            e.printStackTrace();
            //这里是将返回的异常信息进行字节数组编码,其实就是兼容这个方法
            try {
                result = e.toString().getBytes("utf-8");

            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }
        }

        //返回结果
        return result;
    }


    /**
     * 读取浏览器请求的输入流
     */
    public static byte[] readBytes(InputStream inputStream, boolean fully) throws IOException {
        //一次接受1m的数据就行了,1024byte = 1m
        int bufferSize = 1024;
        //这里初始化一个输出流,待会存取url返回给我们的数据用
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        //开始new一个1m大小的字节数组
        byte[] buffer = new byte[bufferSize];
        //循环继续读取
        while (true) {
            //从输入流获取数据,调read方法存到buffer数组中
            int length = inputStream.read(buffer);
            //读到的长度如果是-1,说明没读到数据了,直接退出
            if (length == -1) {
                break;
            }
            //接着先将读到的1m数据输出到我们初始化的那个输出流中
            byteArrayOutputStream.write(buffer, 0, length);
            //这里是一个结束的操作,length != bufferSize,说明已经是最后一次读取了,为什么这么说?
            //举个例子,如果你的数据是1025字节,当你第二次循环的时候就是只有一个字节了,这时候就说明处理完这一个字节的数组就可以结束了,因为已经没数据了
            if (length != bufferSize && !fully) {
                break;
            }
        }

        return byteArrayOutputStream.toByteArray();
    }
}