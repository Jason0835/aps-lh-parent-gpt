package com.ruoyi.framework.config;

import at.pollux.thymeleaf.shiro.dialect.ShiroDialect;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common4ui.utils.StringUtils;
import com.ruoyi.common4ui.utils.spring.SpringUtils4BootUI;
import com.zlt.framework.cas.CallbackFilter;
import com.zlt.framework.cas.CasRealm;
import com.zlt.framework.config.SsoConfig;
import com.ruoyi.framework.shiro.realm.UserRealm;
import com.zlt.framework.shiro.session.OnlineSessionDAO;
import com.zlt.framework.shiro.session.OnlineSessionFactory;
import com.zlt.framework.shiro.session.OnlineSessionSSODAO;
import com.zlt.framework.shiro.web.RemoteCredentialsMatcher;
import com.ruoyi.framework.shiro.web.filter.kickout.KickoutSessionFilter;
import com.zlt.framework.shiro.web.filter.LogoutFilter;
import com.zlt.framework.shiro.web.online.CurrentSystemAuthFilter;
import com.zlt.framework.shiro.web.online.OnlineSessionFilter;
import com.zlt.framework.shiro.web.session.OnlineCasWebSessionManager;
import com.zlt.framework.shiro.web.session.OnlineWebSessionManager;
import com.ruoyi.framework.shiro.web.session.SpringSessionValidationScheduler;
import io.buji.pac4j.filter.SecurityFilter;
import io.buji.pac4j.subject.Pac4jSubjectFactory;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.cache.ehcache.EhCacheManager;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.config.ConfigurationException;
import org.apache.shiro.io.ResourceUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.filter.AccessControlFilter;
import org.apache.shiro.web.mgt.CookieRememberMeManager;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.DefaultWebSubjectFactory;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.pac4j.core.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.DelegatingFilterProxy;

import javax.servlet.Filter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 权限配置加载
 * 
 * @author ruoyi
 */
@Getter
@Configuration()
@Configurable(preConstruction = true)
public class ShiroConfig
{
    /**
     * Session超时时间，单位为毫秒（默认30分钟）
     */
    @Value("${shiro.session.expireTime}")
    private int expireTime;

    /**
     * 相隔多久检查一次session的有效性，单位毫秒，默认就是10分钟
     */
    @Value("${shiro.session.validationInterval}")
    private int validationInterval;

    /**
     * 同一个用户最大会话数
     */
    @Value("${shiro.session.maxSession}")
    private int maxSession;

    /**
     * 踢出之前登录的/之后登录的用户，默认踢出之前登录的用户
     */
    @Value("${shiro.session.kickoutAfter}")
    private boolean kickoutAfter;

    /**
     * 验证码开关
     */
    @Value("${shiro.user.captchaEnabled}")
    private boolean captchaEnabled;

    /**
     * 验证码类型
     */
    @Value("${shiro.user.captchaType}")
    private String captchaType;

    /**
     * 设置Cookie的域名
     */
    @Value("${shiro.cookie.domain}")
    private String domain;

    /**
     * 设置cookie的有效访问路径
     */
    @Value("${shiro.cookie.path}")
    private String path;

    /**
     * 设置HttpOnly属性
     */
    @Value("${shiro.cookie.httpOnly}")
    private boolean httpOnly;

    /**
     * 设置Cookie的过期时间，秒为单位
     */
    @Value("${shiro.cookie.maxAge}")
    private int maxAge;

    /**
     * 设置cipherKey密钥
     */
    @Value("${shiro.cookie.cipherKey}")
    private String cipherKey;

    /**
     * 登录地址
     */
    @Value("${shiro.user.loginUrl}")
    private String loginUrl;

    /**
     * 登出地址
     */
    private String logoutUrl;

    private String casServerUrl;

    /**
     * 权限认证失败地址
     */
    @Value("${shiro.user.unauthorizedUrl}")
    private String unauthorizedUrl;

    @Value("${shiro.user.indexUrl}")
    private String indexUrl;

    @Autowired
    SsoConfig ssoConfig;

    private EnterpriseCacheSessionDAO sessionDAO;

    private void init(){
        //当开启时，把shiro的属性更新掉。
        if(ssoConfig.getEnable()) {
            loginUrl = ssoConfig.getLoginUrl();
            unauthorizedUrl = ssoConfig.getUnauthorizedUrl();
            indexUrl = ssoConfig.getIndexUrl();
            logoutUrl=ssoConfig.getLogoutUrl();
            casServerUrl=ssoConfig.getCas();
        }
    }

    /**
     * 缓存管理器 使用Ehcache实现
     */
    @Bean
    public EhCacheManager getEhCacheManager()
    {
        net.sf.ehcache.CacheManager cacheManager = net.sf.ehcache.CacheManager.getCacheManager("ruoyi");
        EhCacheManager em = new EhCacheManager();
        if (StringUtils.isNull(cacheManager))
        {
            em.setCacheManager(new net.sf.ehcache.CacheManager(getCacheManagerConfigFileInputStream()));
            return em;
        }
        else
        {
            em.setCacheManager(cacheManager);
            return em;
        }
    }

    /**
     * 返回配置文件流 避免ehcache配置文件一直被占用，无法完全销毁项目重新部署
     */
    protected InputStream getCacheManagerConfigFileInputStream()
    {
        String configFile = "classpath:ehcache/ehcache-shiro.xml";
        InputStream inputStream = null;
        try
        {
            inputStream = ResourceUtils.getInputStreamForPath(configFile);
            byte[] b = IOUtils.toByteArray(inputStream);
            InputStream in = new ByteArrayInputStream(b);
            return in;
        }
        catch (IOException e)
        {
            throw new ConfigurationException(
                    "Unable to obtain input stream for cacheManagerConfigFile [" + configFile + "]", e);
        }
        finally
        {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /**
     * 自定义Realm
     */
    @Bean
    public AuthorizingRealm userRealm(EhCacheManager cacheManager)
    {
        AuthorizingRealm userRealm;
        //根据设置，判断是否走单点登录 20201218 linbn
        if(ssoConfig.getEnable()){
            userRealm = new CasRealm();
        }else{
            userRealm = new UserRealm();
        }

        userRealm.setAuthorizationCacheName(Constants.SYS_AUTH_CACHE);
        userRealm.setCredentialsMatcher(remoteCredentialsMatcher());
        userRealm.setCacheManager(cacheManager);
        return userRealm;
    }

    @Bean
    public RemoteCredentialsMatcher remoteCredentialsMatcher(){
        return new RemoteCredentialsMatcher();
    }

    /**
     * 自定义sessionDAO会话
     */
    @Bean
    public EnterpriseCacheSessionDAO sessionDAO()
    {
        if(ssoConfig.getEnable()){
            sessionDAO = new OnlineSessionSSODAO();
        }else{
            sessionDAO = new OnlineSessionDAO();
        }
        return sessionDAO;
    }

    /**
     * 自定义sessionFactory会话
     */
    @Bean
    public OnlineSessionFactory sessionFactory()
    {
        OnlineSessionFactory sessionFactory = new OnlineSessionFactory();
        return sessionFactory;
    }

    /**
     * 会话管理器
     */
    @Bean
    public DefaultWebSessionManager sessionManager()
    {
        DefaultWebSessionManager manager=null;
        if(ssoConfig.getEnable()){
            manager=new OnlineCasWebSessionManager();
        }else{
            manager = new OnlineWebSessionManager();
        }

        // 加入缓存管理器
        manager.setCacheManager(getEhCacheManager());
        // 删除过期的session
        manager.setDeleteInvalidSessions(true);
        // 设置全局session超时时间
        manager.setGlobalSessionTimeout(expireTime * 60 * 1000);
        // 去掉 JSESSIONID
        manager.setSessionIdUrlRewritingEnabled(false);
        // 定义要使用的无效的Session定时调度器
        manager.setSessionValidationScheduler(SpringUtils4BootUI.getBean(SpringSessionValidationScheduler.class));
        // 是否定时检查session
        manager.setSessionValidationSchedulerEnabled(true);
        // 自定义SessionDao
        manager.setSessionDAO(sessionDAO());
        // 自定义sessionFactory
        manager.setSessionFactory(sessionFactory());
        return manager;
    }

    /**
     * 安全管理器
     */
    @Bean
    public SecurityManager securityManager(AuthorizingRealm userRealm)
    {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        // 设置realm.
        securityManager.setRealm(userRealm);
        // 记住我
        securityManager.setRememberMeManager(rememberMeManager());
        // 注入缓存管理器;
        securityManager.setCacheManager(getEhCacheManager());
        // session管理器
        securityManager.setSessionManager(sessionManager());
        //换用cas的subject
        securityManager.setSubjectFactory(subjectFactory());
        return securityManager;
    }

    /**
     * 使用 pac4j 的 subjectFactory
     * @return
     */
    @Bean
    public DefaultWebSubjectFactory subjectFactory(){
        return new Pac4jSubjectFactory();
    }

    /**
     * 退出过滤器
     */
    public LogoutFilter logoutFilter(Config config)
    {
        LogoutFilter logoutFilter = new LogoutFilter();
        logoutFilter.setLoginUrl(loginUrl);
        logoutFilter.setConfig(config);
        logoutFilter.setCentralLogout(true);
        logoutFilter.setLogoutUrlPattern(logoutUrl);//设置登出地址
        logoutFilter.setCasEnable(ssoConfig.getEnable());//设置是否开启CAS
        logoutFilter.setDefaultUrl(ssoConfig.callbackUrl());
        return logoutFilter;
    }

    /**
     * Shiro过滤器配置
     */

    @Bean
    public ShiroFilterFactoryBean shiroFilterFactoryBean(SecurityManager securityManager, Config config)
    {
        init();
        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
        // Shiro的核心安全接口,这个属性是必须的
        shiroFilterFactoryBean.setSecurityManager(securityManager);
        // 身份认证失败，则跳转到登录页面的配置
        shiroFilterFactoryBean.setLoginUrl(loginUrl);
        // 权限认证失败，则跳转到指定页面
        shiroFilterFactoryBean.setUnauthorizedUrl(unauthorizedUrl);
        loadShiroFilterChain(shiroFilterFactoryBean);

        Map<String, Filter> filters = new LinkedHashMap<String, Filter>();
        filters.put("onlineSession", onlineSessionFilter());
        filters.put("kickout", kickoutSessionFilter());
        filters.put("systemAuth", currentSystemAuthFilter());
        if(ssoConfig.getEnable()) {
            filters.put("securityFilter", securityFilter(config));
            filters.put("callbackFilter", callbackFilter(config));
        }
        // 注销成功，则跳转到指定页面
        filters.put("logout", logoutFilter(config));
        shiroFilterFactoryBean.setFilters(filters);

        return shiroFilterFactoryBean;
    }

    /**
     * //cas 资源认证拦截器 pac4j
     * @return
     */
    SecurityFilter securityFilter(Config config){
        SecurityFilter securityFilter = new SecurityFilter();
        securityFilter.setConfig(config);
        securityFilter.setClients(ssoConfig.getClientName());
        return  securityFilter;
    }

    /**
     * pac4j 登录成功后回调拦截器
     * @return
     */
    CallbackFilter callbackFilter(Config config){
        CallbackFilter callbackFilter = new CallbackFilter();
        callbackFilter.setConfig(config);
        callbackFilter.setDefaultUrl(ssoConfig.getIndexUrl());
        return callbackFilter;
    }

    /***
     * 过滤路径配置
     * @param shiroFilterFactoryBean
     */
    private void loadShiroFilterChain(ShiroFilterFactoryBean shiroFilterFactoryBean) {
        // Shiro连接约束配置，即过滤链的定义
        LinkedHashMap<String, String> filterChainDefinitionMap = new LinkedHashMap<>();
        // 对静态资源设置匿名访问
        filterChainDefinitionMap.put("/favicon.ico**", "anon");
        filterChainDefinitionMap.put("/ruoyi.png**", "anon");
        filterChainDefinitionMap.put("/css/**", "anon");
        filterChainDefinitionMap.put("/docs/**", "anon");
        filterChainDefinitionMap.put("/fonts/**", "anon");
        filterChainDefinitionMap.put("/img/**", "anon");
        filterChainDefinitionMap.put("/ajax/**", "anon");
        filterChainDefinitionMap.put("/js/**", "anon");
        filterChainDefinitionMap.put("/ruoyi/**", "anon");
        filterChainDefinitionMap.put("/captcha/captchaImage**", "anon");
        filterChainDefinitionMap.put("/locales/**", "anon");
        filterChainDefinitionMap.put("/unauth", "anon");
        // 退出 logout地址，shiro去清除session
        filterChainDefinitionMap.put("/logout", "logout");
        // 不需要拦截的访问
        filterChainDefinitionMap.put("/login", "anon");
        // 注册相关
        filterChainDefinitionMap.put("/register", "anon");
        // 系统权限列表
        // filterChainDefinitionMap.putAll(SpringUtils.getBean(IMenuService.class).selectPermsAll());

        //CAS 拦截器
        if(ssoConfig.getEnable()) {
            filterChainDefinitionMap.put("/", "securityFilter");
            filterChainDefinitionMap.put("/toIndex", "securityFilter");
            filterChainDefinitionMap.put("/index", "securityFilter");
            filterChainDefinitionMap.put("/callback", "callbackFilter");
        }

        // 所有请求需要认证
        filterChainDefinitionMap.put("/**", "user,kickout,onlineSession,systemAuth");
        shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);
    }

    /**
     * 自定义在线用户处理过滤器
     */
    public AccessControlFilter onlineSessionFilter()
    {
        AccessControlFilter onlineSessionFilter
                = new OnlineSessionFilter(loginUrl, sessionDAO);
        return onlineSessionFilter;
    }

    /**
     * 系统授权处理过滤器
     */
    @Bean
    public CurrentSystemAuthFilter currentSystemAuthFilter()
    {
        CurrentSystemAuthFilter currentSystemAuthFilter = new CurrentSystemAuthFilter();
        currentSystemAuthFilter.setLoginUrl(loginUrl);
        List<String> path = new ArrayList<>();
        path.add("/css/**");
        path.add("/js/**");
        path.add("/img/**");
        path.add("/locales/**");
        path.add("/login");
        path.add("/ruoyi/**");
        path.add("/unauth");
        currentSystemAuthFilter.setAnonPath(path);
        return currentSystemAuthFilter;
    }

    /**
     * cookie 属性设置
     */
    public SimpleCookie rememberMeCookie()
    {
        SimpleCookie cookie = new SimpleCookie("rememberMe");
        cookie.setDomain(domain);
        cookie.setPath(path);
        cookie.setHttpOnly(httpOnly);
        cookie.setMaxAge(maxAge * 24 * 60 * 60);
        return cookie;
    }

    /**
     * 记住我
     */
    public CookieRememberMeManager rememberMeManager()
    {
        CookieRememberMeManager cookieRememberMeManager = new CookieRememberMeManager();
        cookieRememberMeManager.setCookie(rememberMeCookie());
        cookieRememberMeManager.setCipherKey(Base64.decode(cipherKey));
        return cookieRememberMeManager;
    }

    /**
     * 同一个用户多设备登录限制
     */
    public KickoutSessionFilter kickoutSessionFilter()
    {
        KickoutSessionFilter kickoutSessionFilter = new KickoutSessionFilter();
        kickoutSessionFilter.setCacheManager(getEhCacheManager());
        kickoutSessionFilter.setSessionManager(sessionManager());
        // 同一个用户最大的会话数，默认-1无限制；比如2的意思是同一个用户允许最多同时两个人登录
        kickoutSessionFilter.setMaxSession(maxSession);
        // 是否踢出后来登录的，默认是false；即后者登录的用户踢出前者登录的用户；踢出顺序
        kickoutSessionFilter.setKickoutAfter(kickoutAfter);
        // 被踢出后重定向到的地址；
        kickoutSessionFilter.setKickoutUrl("/login?kickout=1");
        return kickoutSessionFilter;
    }

    /**
     * thymeleaf模板引擎和shiro框架的整合
     */
    @Bean
    public ShiroDialect shiroDialect()
    {
        return new ShiroDialect();
    }

    /**
     * 开启Shiro注解通知器
     */
    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(
            @Qualifier("securityManager") SecurityManager securityManager)
    {
        AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor = new AuthorizationAttributeSourceAdvisor();
        authorizationAttributeSourceAdvisor.setSecurityManager(securityManager);
        return authorizationAttributeSourceAdvisor;
    }

    /**
     * 委托代理过滤器,解决UnavailableSecurityManagerException问题
     * 在没有登录状态下的处理
     * @return
     */
    @Bean
    public FilterRegistrationBean delegatingFilterProxy(){
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
        DelegatingFilterProxy proxy = new DelegatingFilterProxy();
        proxy.setTargetFilterLifecycle(true);
        proxy.setTargetBeanName("shiroFilterFactoryBean");
        filterRegistrationBean.setFilter(proxy);
        return filterRegistrationBean;
    }

}
