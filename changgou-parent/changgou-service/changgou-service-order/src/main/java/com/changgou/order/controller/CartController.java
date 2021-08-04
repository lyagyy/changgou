package com.changgou.order.controller;

import com.changgou.order.pojo.OrderItem;
import com.changgou.order.service.CartService;
import entity.Result;
import entity.StatusCode;
import entity.TokenDecode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping(value = "/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    /**
     * 加入购物车
     * @param num 购买数量
     * @param id  商品的（sku）id
     * @return
     */
    @RequestMapping("/add")
    public Result add(Integer num,Long id){
        //获取用户令牌信息-->解析令牌信息,username
        Map<String, String> userInfo = TokenDecode.getUserInfo();
        String username = userInfo.get("username");
        //用户名
        //String username="szitheima";
        cartService.add(num,id,username);
        return new Result(true, StatusCode.OK,"加入购物车成功");
    }

    /***
     * 查询用户购物车列表
     * @return
     */
    @GetMapping(value = "/list")
    public Result list(){
        //获取用户令牌信息-->解析令牌信息,username
        Map<String, String> userInfo = TokenDecode.getUserInfo();
        String username = userInfo.get("username");
        //用户名
        //String username="szitheima";
        List<OrderItem> orderItems = cartService.list(username);
        return new Result(true,StatusCode.OK,"购物车列表查询成功！",orderItems);
    }
}
