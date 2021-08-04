package com.changgou.goods.feign;


import com.changgou.goods.pojo.Category;
import com.changgou.goods.pojo.Sku;
import com.changgou.goods.pojo.Spu;
import entity.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient("goods")
@RequestMapping("/sku")
public interface SkuFeign {

    /***
     * 根据审核状态查询Sku
     */
    @GetMapping("/status/{status}")
    Result<List<Sku>> findByStatus(@PathVariable(value = "status") String status);

    /**
     * 根据条件搜索
     * @param sku
     * @return
     */
    @PostMapping(value = "/search" )
    public Result<List<Sku>> findList(@RequestBody(required = false) Sku sku);

    /***
     * 根据ID查询SKU信息
     * @param id : sku的ID
     */
    @GetMapping(value = "/{id}")
    public Result<Sku> findById(@PathVariable(value = "id", required = true) Long id);


    /**
     * 商品信息递减
     * Map<Long,Integer> key:要递减的商品id value:要递减的数量
     * @param decrmap
     * @return
     */
    @GetMapping(value = "/decr/count")
    Result decrCount(@RequestParam Map<String,Integer> decrmap);
}
