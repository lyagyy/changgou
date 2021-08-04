package com.changgou.order.service.impl;

import com.changgou.goods.feign.SkuFeign;
import com.changgou.goods.feign.SpuFeign;
import com.changgou.goods.pojo.Sku;
import com.changgou.goods.pojo.Spu;
import com.changgou.order.pojo.OrderItem;
import com.changgou.order.service.CartService;
import entity.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private SkuFeign skuFeign;
    @Autowired
    private SpuFeign spuFeign;

    /**
     * 加入购物车
     * @param num 添加商品数量
     * @param id  购买id
     * @param username 购买用户
     */
    @Override
    public void add(Integer num, Long id, String username) {
        if(num<=0){
            //移除购物车该商品
            redisTemplate.boundHashOps("Cart_"+username).delete(id);

            //如果此时该购物车数量为空，则连购物车一起移除
            Long size = redisTemplate.boundHashOps("Cart_" + username).size();
            if(size==null||size<=0){
                redisTemplate.delete("Cart_"+username);
            }

        }



        //查询sku
        Result<Sku> skuResult = skuFeign.findById(id);

        if(skuResult!=null){
            //获取spu
            Sku sku = skuResult.getData();

            Result<Spu> spuResult = spuFeign.findById(sku.getSpuId());

            //将sku，spu转为OrderItem
            OrderItem orderItem = sku2OrderItem(sku, spuResult.getData(), num);

            //添加购物车数据到redis
           redisTemplate.boundHashOps("Cart_"+username).put(id,orderItem);

        }
    }

    /***
     * 查询用户购物车数据
     * @param username
     * @return
     */
    @Override
    public List<OrderItem> list(String username) {
        List<OrderItem> orderItems = redisTemplate.boundHashOps("Cart_" + username).values();
        return orderItems;
    }

    /***
     * 创建一个OrderItem对象
     * @param sku
     * @param num
     * @return
     */
    private OrderItem sku2OrderItem(Sku sku,Spu spu,Integer num){
        OrderItem orderItem = new OrderItem();
        orderItem.setSpuId(sku.getSpuId());
        orderItem.setSkuId(sku.getId());
        orderItem.setName(sku.getName());
        orderItem.setPrice(sku.getPrice());
        orderItem.setNum(num);
        orderItem.setMoney(num*orderItem.getPrice());       //单价*数量
        orderItem.setPayMoney(num*orderItem.getPrice());    //实付金额
        orderItem.setImage(sku.getImage());
        orderItem.setWeight(sku.getWeight()*num);           //重量=单个重量*数量

        //分类ID设置
        orderItem.setCategoryId1(spu.getCategory1Id());
        orderItem.setCategoryId2(spu.getCategory2Id());
        orderItem.setCategoryId3(spu.getCategory3Id());
        return orderItem;
    }

}
