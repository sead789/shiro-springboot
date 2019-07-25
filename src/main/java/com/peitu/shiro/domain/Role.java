package com.peitu.shiro.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Rising
 * @date 2019/7/18
 */
@Data
public class Role implements Serializable {

    private Integer roleId;

    private String roleName;

    private String roleDescription;

    private Set<Permission> permissions = new HashSet<>();

    //private Set<User> users = new HashSet<>();

}
