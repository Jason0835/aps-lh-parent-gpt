package com.zlt.aps.itf.Interceptor;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.itf.annotation.ItfApi;
import com.zlt.aps.itf.config.SysLoginItfConfigs;
import com.zlt.aps.itf.util.BodyReaderHttpServletRequestWrapper;
import com.zlt.aps.itf.util.HttpUtils;
import com.zlt.aps.itf.vo.ItfAjaxResult;
import com.zlt.aps.itf.vo.SysLoginItfVo;
import com.zlt.common.utils.PubUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

import static com.zlt.aps.itf.util.GenerateTokenUtil.validateKey;


/**
 * 签名拦截器
 * @author qinfeng
 */
@Slf4j
public class TokenAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private SysLoginItfConfigs sysLoginItfConfigs;

    // 特殊路径:
    // 1. ItfAjax
    private List<String> itfAjaxPathList = new ArrayList<>();
    // 2. head的token
    private final List<String> headToken = new ArrayList<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        log.info("Token Interceptor request URI = " + requestURI);
        HttpServletRequest requestWrapper = new BodyReaderHttpServletRequestWrapper(request);

        log.info("请求方IP = " + requestWrapper.getRemoteAddr());
        log.info("请求方端口 = " + requestWrapper.getRemotePort());
        log.info("请求方域名 以RemoteHost取值 = " + requestWrapper.getRemoteHost());

        showRequestLog(request);

        //对注解方法进行校验
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        if (handlerMethod.getMethodAnnotation(ItfApi.class) == null) {
            return true;
        }
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=utf-8");
        //1.拦截Json中token
        String token = null;
        try {
            token = getFromToken(requestURI, requestWrapper);
        } catch (Exception e) {
            doResponseValue(response, requestURI, "接口参数解析错误,无法解析token项");
            return false;
        }

        //未携带token不允许通过
        if (token == null) {
            doResponseValue(response, requestURI, "未携带token不允许通过");
            return false;
        }

        SysLoginItfVo itfVo = sysLoginItfConfigs.getBytoken(token);
        if (itfVo == null) {
            //若token在配置中找不到，再验1次IP
            if (validateKey(token, request)){
                return true;
            }
            doResponseValue(response, requestURI, "携带的token无效");
            return false;
        }
        //2.拦截request中的ip
        if (itfVo.isNeedIpCheck()){
            String currentIp = getRemoteIP(request);
            String safeIp = itfVo.getIp();
            if (safeIp.indexOf(currentIp)>=0){
                return true;
            }
            doResponseValue(response, requestURI, "非法IP,不允许访问");
            return false;
        }

        return true;
        //验证是否专属token
       /* if (validateKey(token, request)) {
            return true;
        } else {
            doResponseValue(response, requestURI, "ip与token不匹配");
            return false;
        }*/
    }

    /**
     * 处理返回值
     */
    private void doResponseValue(HttpServletResponse response, String requestURI, String s) throws IOException {
        if (itfAjaxPathList.contains(requestURI)) {
            response.getWriter().write(JSON.toJSONString(ItfAjaxResult.error(s)));
        } else {
            response.getWriter().write(JSON.toJSONString(AjaxResult.error(s)));
        }
    }

    private String getFromToken(String requestURI, HttpServletRequest requestWrapper) throws IOException {
        String token;
        // 获取token
        SortedMap<String, Object> allParams = HttpUtils.getAllParams(requestWrapper);
        log.info("本次拦截参数 = " + JSON.toJSONString(allParams));
        token = (String) allParams.get("token");

        //用来匹配BIP token
        if (PubUtil.isNotEmpty(allParams.get("distributeToken"))){
            token = (String)allParams.get("distributeToken");
        }

        //用来匹配Tms的 token
        if(headToken.contains(requestURI)){
            JSONObject data = JSONObject.parseObject(String.valueOf(allParams.get("Head")));
            token = data.getString("Token");
        }

        // //用来匹配WMS的token
        // if(httpHeadToken.contains(requestURI)){
        //     token = requestWrapper.getHeader("Token");
        // }

        return token;
    }

    private void showRequestLog(HttpServletRequest request) {
        //获取客户端向服务器端传送数据的协议名称
        System.out.println("rotocol: " + request.getProtocol());
        //返回的协议名称.默认是http
        System.out.println("Scheme: " + request.getScheme());
        //可以返回当前页面所在的服务器的名字;如果你的应用部署在本机那么其就返回localhost或者127.0.0.1 ，这两个是等价的
        System.out.println("ServerName: " + request.getServerName());
        //可以返回当前页面所在的服务器使用的端口,就是8083
        System.out.println("ServerPort: " + request.getServerPort());
        //request.getRemoteAddr()是获得客户端的ip地址
        System.out.println("RemoteAddr: " + request.getRemoteAddr());
        //request.getRemoteHost()是获得客户端的主机名。
        System.out.println("RemoteHost: " + request.getRemoteHost());
        //返回字符编码
        System.out.println("CharacterEncoding: " + request.getCharacterEncoding());
        //描述HTTP消息实体的传输长度
        System.out.println("ContentLength: " + request.getContentLength());
        //定义网络文件的类型和网页的编码，决定浏览器将以什么形式、什么编码读取这个文件，
        System.out.println("ContentType: " + request.getContentType());
        //如果servlet由一个鉴定方案所保护，如HTTP基本鉴定，则返回方案名称
        System.out.println("AuthType: " + request.getAuthType());
        //返回HTTP请求方法（例如GET、POST等等）
        System.out.println("HttpMethod: " + request.getMethod());
        //返回在URL中指定的任意附加路径信息。
        System.out.println("pathInfo: " + request.getPathInfo());
        //返回在URL中指定的任意附加路径信息，被子转换成一个实际路径
        System.out.println("pathTrans: " + request.getPathTranslated());
        //返回查询字符串，即URL中?后面的部份。
        System.out.println("QueryString: " + request.getQueryString());
        //如果用户通过鉴定，返回远程用户名，否则为null。
        System.out.println("RemoteUser: " + request.getRemoteUser());
        //返回客户端的会话ID
        System.out.println("SessionId: " + request.getRequestedSessionId());
        //返回URL中一部分，从“/”开始，包括上下文，但不包括任意查询字符串。
        System.out.println("RequestURI: " + request.getRequestURI());
        //返回请求URI上下文后的子串
        System.out.println("ServletPath: " + request.getServletPath());
        //返回指定的HTTP头标指。如果其由请求给出，则名字应为大小写不敏感。
        System.out.println("Accept: " + request.getHeader("Accept"));
        //获取请求的头部信息
        System.out.println("Host: " + request.getHeader("Host"));
        //获取来源页地址
        System.out.println("Referer : " + request.getHeader("Referer"));
        //获取请求方地址
        System.out.println("Origin : " + request.getHeader("Origin"));
        //获取请求方语言
        System.out.println("Accept-Language : " + request.getHeader("Accept-Language"));
        //浏览器支持的编码类型
        System.out.println("Accept-Encoding : " + request.getHeader("Accept-Encoding"));
        //浏览器标识
        System.out.println("User-Agent : " + request.getHeader("User-Agent"));
        //在浏览器中不设置Connection，会默认是keep-alive（长连接）
        System.out.println("Connection : " + request.getHeader("Connection"));
        //获取请求方Cookie
        System.out.println("Cookie : " + request.getHeader("Cookie"));
        //获取请求方IP地址
        System.out.println("X-Forwarded-For : " + request.getHeader("X-Forwarded-For"));
        //获取请求方IP地址
        System.out.println("Proxy-Client-Ip : " + request.getHeader("Proxy-Client-Ip"));
        //获取请求方IP地址
        System.out.println("WL-Proxy-Client-Ip : " + request.getHeader("WL-Proxy-Client-Ip"));
        //获取请求方IP地址
        System.out.println("X-Real-IP : " + request.getHeader("X-Real-IP"));

        //获取真实IP地址
        System.out.println("Real IP : "+ getRemoteIP(request));
    }

    /**
     * 获取真实IP地址，不使用request.getRemoteAddr();的原因是有可能用户使用了代理软件方式避免真实IP地址,
     * 可是，如果通过了多级反向代理的话，X-Forwarded-For的值并不止一个，而是一串IP值，究竟哪个才是真正的用户端的真实IP呢？
     * 答案是取X-Forwarded-For中第一个非unknown的有效IP字符串。
     * <p>
     * 如：X-Forwarded-For：192.168.1.110, 192.168.1.120, 192.168.1.130,
     * 192.168.1.100
     * <p>
     * 用户真实IP为： 192.168.1.110
     *
     * @param request
     * @return
     */
    public static String getRemoteIP(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            // X-Real-IP：nginx服务代理
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.indexOf(",") > 0) {
            String[] parts = ip.split(",");
            for (String part : parts) {
                if (!part.isEmpty() && !"unknown".equalsIgnoreCase(part)) {
                    ip = part.trim();
                    break;
                }
            }
        }
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }
}
