package com.peitu.shiro.service;


import com.peitu.shiro.domain.User;
import com.peitu.shiro.mapper.UserMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author Rising
 * @date 2019/7/18
 */
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Override
    public User findByUserName(String userName) {
        return userMapper.findByUserName(userName);
    }
}
