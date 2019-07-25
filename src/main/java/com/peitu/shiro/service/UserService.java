package com.peitu.shiro.service;


import com.peitu.shiro.domain.User;

/**
 * @author Rising
 * @date 2019/7/18
 */
public interface UserService {

    User findByUserName(String userName);
}
