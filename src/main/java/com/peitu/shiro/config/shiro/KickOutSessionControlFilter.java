package com.peitu.shiro.config.shiro;

import com.peitu.shiro.domain.User;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.DefaultSessionKey;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.AccessControlFilter;
import org.apache.shiro.web.util.WebUtils;
import org.crazycake.shiro.RedisManager;
import org.crazycake.shiro.exception.SerializationException;
import org.crazycake.shiro.serializer.ObjectSerializer;
import org.crazycake.shiro.serializer.RedisSerializer;
import org.crazycake.shiro.serializer.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Deque;
import java.util.LinkedList;

/**
 * @author: WangSaiChao
 * @date: 2018/5/23
 * @description: shiro 自定义filter 实现 并发登录控制
 */
public class KickOutSessionControlFilter extends AccessControlFilter {

    private static final Object KEY = "kickOut";
    /**
     * 踢出后到的地址
     */
    private String kickOutUrl;

    /**
     * 踢出之前登录的/之后登录的用户 默认踢出之前登录的用户
     */
    private boolean kickOutAfter = false;

    /**
     * 同一个帐号最大会话数 默认1
     */
    private int maxSession = 1;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    RedisManager redisManager;

    private Cache<String, Deque<Serializable>> cache;

    public static final String DEFAULT_KICKOUT_CACHE_KEY_PREFIX = "shiro:cache:kickout:";

    private String keyPrefix = "shiro:cache:kickout:";

    public void setKickOutUrl(String kickOutUrl) {
        this.kickOutUrl = kickOutUrl;
    }

    public void setKickOutAfter(boolean kickOutAfter) {
        this.kickOutAfter = kickOutAfter;
    }

    public void setMaxSession(int maxSession) {
        this.maxSession = maxSession;
    }

    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void setRedisManager(RedisManager redisManager) {
        this.redisManager = redisManager;
    }

    /**
     * 是否允许访问，返回true表示允许
     */
    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) throws Exception {
        return false;
    }

    private RedisSerializer keySerializer = new StringSerializer();
    private RedisSerializer valueSerializer = new ObjectSerializer();

    private byte[] getRedisKickOutKey(String username) throws SerializationException {
        return keySerializer.serialize(this.keyPrefix + username);
    }

    /**
     * 表示访问拒绝时是否自己处理，如果返回true表示自己不处理且继续拦截器链执行，返回false表示自己已经处理了（比如重定向到另一个页面）。
     */
    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        Subject subject = getSubject(request, response);
        if (!subject.isAuthenticated() && !subject.isRemembered()) {
            //如果没有登录，直接进行之后的流程
            return true;
        }

        Session session = subject.getSession();
        //这里获取的User是实体 因为我在 自定义ShiroRealm中的doGetAuthenticationInfo方法中
        //new SimpleAuthenticationInfo(user, password, getName()); 传的是 User实体 所以这里拿到的也是实体,如果传的是userName 这里拿到的就是userName
        String userName = ((User) subject.getPrincipal()).getUserName();
        Serializable sessionId = session.getId();

        // 初始化用户的队列放到缓存里
        Deque<Serializable> deque = (Deque<Serializable>) valueSerializer.deserialize(redisManager.get(getRedisKickOutKey(userName)));

        if (deque == null || deque.size() == 0) {
            deque = new LinkedList<Serializable>();
        }

        //如果队列里没有此sessionId，且用户没有被踢出；放入队列
        if (!deque.contains(sessionId) && session.getAttribute(KEY) == null) {
            deque.push(sessionId);
        }

        //如果队列里的sessionId数超出最大会话数，开始踢人
        while (deque.size() > maxSession) {
            Serializable kickOutSessionId;
            //如果踢出后者
            if (kickOutAfter) {
                kickOutSessionId = deque.removeFirst();
            } else { //否则踢出前者
                kickOutSessionId = deque.removeLast();
            }
            try {
                Session kickOutSession = sessionManager.getSession(new DefaultSessionKey(kickOutSessionId));
                if (kickOutSession != null) {
                    //设置会话的kickOut属性表示踢出了
                    kickOutSession.setAttribute(KEY, true);
                }
                //ignore exception
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //如果被踢出了，直接退出，重定向到踢出后的地址
        if (session.getAttribute(KEY) != null) {
            //会话被踢出了
            subject.logout();
            WebUtils.issueRedirect(request, response, kickOutUrl);
            return false;
        }

        //正常登录，保留入缓存
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(deque);
        byte[] bytes = baos.toByteArray();
        redisManager.set(getRedisKickOutKey(userName), bytes, 1800);

        return true;
    }

}
