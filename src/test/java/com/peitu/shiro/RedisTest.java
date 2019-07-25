package com.peitu.shiro;

import com.peitu.shiro.domain.User;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.apache.shiro.session.Session;
import org.crazycake.shiro.RedisManager;
import org.crazycake.shiro.exception.SerializationException;
import org.crazycake.shiro.serializer.ObjectSerializer;
import org.crazycake.shiro.serializer.RedisSerializer;
import org.crazycake.shiro.serializer.StringSerializer;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.Serializable;

/**
 * @author Rising
 * @date 2019/7/22
 */
@SpringBootTest
//@RunWith(SpringRunner.class)
@Component
public class RedisTest {

    @Autowired
    RedisTemplate redisTemplate;

    @Test
    public void set() {
        JedisPoolConfig config = new JedisPoolConfig();
        JedisPool jedisPool = new JedisPool(config, "127.0.0.1", 6379, 1000);

        Jedis jedis = jedisPool.getResource();
        User user = new User();
        user.setUserId(19);
        user.setUserName("jedis测试");
        jedis.set("FastJson222", user.toString());
    }

    @Test
    public void set2() throws SerializationException {
        RedisSerializer valueSerializer = new ObjectSerializer();
        RedisSerializer keySerializer = new StringSerializer();
        RedisManager redisManager = new RedisManager();
        Session s = (Session) valueSerializer.deserialize(redisManager.get(keySerializer.serialize(getRedisSessionKey("ae3137f5-566a-4982-8ddf-30d54ce76aa8"))));
        System.out.println(s.toString());
    }

    private String getRedisSessionKey(Serializable sessionId) {
        return "shiro:session:" + sessionId;
    }

    public static void main(String[] args) {
        System.out.println(new Md5Hash("123456", "city", 3).toString());
    }

}
