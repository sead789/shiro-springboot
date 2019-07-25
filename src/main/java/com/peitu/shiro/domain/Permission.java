package com.peitu.shiro.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * @author Rising
 * @date 2019/7/18
 */
@Data
public class Permission implements Serializable {

    private Integer id;

    private String name;

    private String code;

    private String description;

}
