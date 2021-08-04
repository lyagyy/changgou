package com.changgou.order.service;

import com.changgou.order.pojo.OrderItem;

import java.util.List;

/**
 * 购物车接口
 */
public interface CartService {

    /**
     * 添加购物车
     * @param num 添加商品数量
     * @param id  购买id
     * @param username 购买用户
     */
    void add(Integer num,Long id,String username);

    /***
     * 查询用户的购物车数据
     * @param username
     * @return
     */
    List<OrderItem> list(String username);
}
