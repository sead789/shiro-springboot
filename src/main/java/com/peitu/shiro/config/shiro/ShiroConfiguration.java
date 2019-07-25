package com.peitu.shiro.config.shiro;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.crazycake.shiro.RedisCacheManager;
import org.crazycake.shiro.RedisManager;
import org.crazycake.shiro.RedisSessionDAO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Rising
 * @date 2019/7/23
 */
@Configuration
public class ShiroConfiguration {

    /**
     * 1.创建realm
     *
     * @return
     */
    @Bean
    public CustomRealm getRealm() {
        return new CustomRealm();
    }

    /**
     * 2.创建安全管理器
     *
     * @param realm
     * @return
     */
    @Bean
    public SecurityManager getSecurityManager(CustomRealm realm) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(realm);

        //将自定义的会话管理器注册到安全管理器中
        securityManager.setSessionManager(sessionManager());
        //将自定义的redis缓存管理器注册到安全管理器中
        securityManager.setCacheManager(redisCacheManager());

        return securityManager;
    }

    /**
     * 3.配置shiro的过滤器工厂
     * 在web程序中，shiro进行权限控制全部是通过一组过滤器集合进行控制
     *
     * @param securityManager
     * @return
     */
    @Bean
    public ShiroFilterFactoryBean shiroFilter(SecurityManager securityManager) {
        //1.创建过滤器工厂
        ShiroFilterFactoryBean filterFactory = new ShiroFilterFactoryBean();
        //2.设置安全管理器
        filterFactory.setSecurityManager(securityManager);
        //3.通用配置（跳转登录页面，为授权跳转的页面）
        //跳转url地址
        filterFactory.setLoginUrl("/login");
        //未授权的url
        filterFactory.setUnauthorizedUrl("/noAuthority");

        //自定义filter
        LinkedHashMap<String, Filter> filtersMap = new LinkedHashMap<>();
        //限制同一帐号同时在线的个数
        filtersMap.put("kickOut", kickOutSessionControlFilter());

        filterFactory.setFilters(filtersMap);

        //4.设置过滤器集合
        /**
         * 设置所有的过滤器：有顺序map
         *     key = 拦截的url地址
         *     value = 过滤器类型
         *
         */
        Map<String, String> filterChain = new LinkedHashMap<>();

        //当前请求地址可以匿名访问
        filterChain.put("/login", "anon");
        filterChain.put("/test", "anon");
        //注销，执行后直接跳转到filterFactory.setLoginUrl();设置的URL
        filterChain.put("/logout", "logout");

        //当前请求地址必须认证之后可以访问
        filterChain.put("/**", "authc");

        //具有某中权限才能访问
        //使用过滤器的形式配置请求地址的依赖权限
        //filterChain.put("/user/perm","perms[user-home]"); //不具备指定的权限，跳转到setUnauthorizedUrl地址
        filterChain.put("/user/add", "perms[/user/add]");

        //使用过滤器的形式配置请求地址的依赖角色
        //filterChain.put("/user/home","roles[系统管理员]");
        filterChain.put("/user/product", "roles[用户单位]");

        //开启限制同一账号登录
        filterChain.put("/**", "kickOut");

        filterFactory.setFilterChainDefinitionMap(filterChain);

        return filterFactory;
    }


    @Value("${spring.redis.host}")
    private String host;
    @Value("${spring.redis.port}")
    private int port;

    /**
     * 1.redis的控制器，操作redis
     */
    @Bean("redisManager")
    public RedisManager redisManager() {
        RedisManager redisManager = new RedisManager();
        redisManager.setHost(host);
        redisManager.setPort(port);
        return redisManager;
    }

    /**
     * 2.sessionDao
     */
    @Bean("redisSessionDAO")
    public RedisSessionDAO redisSessionDAO() {
        RedisSessionDAO sessionDAO = new RedisSessionDAO();
        sessionDAO.setRedisManager(redisManager());
        return sessionDAO;
    }

    /**
     * 3.会话管理器
     */
    @Bean("sessionManager")
    public DefaultWebSessionManager sessionManager() {
//        CustomSessionManager sessionManager = new CustomSessionManager();
//        sessionManager.setSessionDAO(redisSessionDAO());
        DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();
        sessionManager.setSessionDAO(redisSessionDAO());
        return sessionManager;
    }

    /**
     * 4.缓存管理器
     */
    @Bean("redisCacheManager")
    public RedisCacheManager redisCacheManager() {
        RedisCacheManager redisCacheManager = new RedisCacheManager();
        redisCacheManager.setRedisManager(redisManager());
        return redisCacheManager;
    }

    /**
     * 开启对shior注解的支持
     *
     * @param securityManager
     * @return
     */
    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor advisor = new AuthorizationAttributeSourceAdvisor();
        advisor.setSecurityManager(securityManager);
        return advisor;
    }

    /**
     * 并发登录控制
     *
     * @return
     */
    @Bean
    public KickOutSessionControlFilter kickOutSessionControlFilter() {
        KickOutSessionControlFilter kickoutSessionControlFilter = new KickOutSessionControlFilter();
        //用于根据会话ID，获取会话进行踢出操作的；
        kickoutSessionControlFilter.setSessionManager(sessionManager());
        //使用cacheManager获取相应的cache来缓存用户登录的会话；用于保存用户—会话之间的关系的；
        kickoutSessionControlFilter.setRedisManager(redisManager());
        //是否踢出后来登录的，默认是false；即后者登录的用户踢出前者登录的用户；
        kickoutSessionControlFilter.setKickOutAfter(false);
        //同一个用户最大的会话数，默认1；比如2的意思是同一个用户允许最多同时两个人登录；
        kickoutSessionControlFilter.setMaxSession(1);
        //被踢出后重定向到的地址；
        kickoutSessionControlFilter.setKickOutUrl("/login?kickOut=1");
        return kickoutSessionControlFilter;
    }

    /**
     * 配置session监听
     *
     * @return
     */
    @Bean("sessionListener")
    public ShiroSessionListener sessionListener() {
        ShiroSessionListener sessionListener = new ShiroSessionListener();
        return sessionListener;
    }

}
