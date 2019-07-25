package com.peitu.shiro.domain;

import lombok.Data;
import org.crazycake.shiro.AuthCachePrincipal;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * AuthCachePrincipal: redis和shiro插件包提供的接口
 * 需实现该接口才能往redis存安全数据
 *
 * @author Rising
 * @date 2019/7/18
 */
@Data
public class User implements Serializable, AuthCachePrincipal {

    private Integer userId;

    private String userName;

    private String password;

    private Set<Role> roles = new HashSet<>();


    @Override
    public String getAuthCacheKey() {
        return null;
    }
}
