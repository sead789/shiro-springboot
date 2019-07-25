package com.peitu.shiro.mapper;

import com.peitu.shiro.domain.User;
import org.apache.ibatis.annotations.Param;

/**
 * @author Rising
 * @date 2019/7/18
 */
public interface UserMapper {


    User findByUserName(@Param("userName") String userName);
}
