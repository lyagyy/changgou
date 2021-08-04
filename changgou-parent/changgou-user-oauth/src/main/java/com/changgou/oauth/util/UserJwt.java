package com.changgou.oauth.util;


import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * 封装SpringSecurity中User信息以及用户自身基本信息
 */
public class UserJwt extends User {
    private String id;    //用户ID
    private String name;  //用户名字

    private String comny;//设置公司
    private String address;//公司地址

    public String getComny() {
        return comny;
    }

    public void setComny(String comny) {
        this.comny = comny;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public UserJwt(String username, String password, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
