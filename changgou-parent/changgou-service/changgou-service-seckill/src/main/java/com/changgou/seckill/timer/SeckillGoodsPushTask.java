package com.changgou.seckill.timer;

import com.changgou.seckill.dao.SeckillGoodsMapper;
import com.changgou.seckill.pojo.SeckillGoods;
import entity.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 定时任务-->获取秒杀商品信息，存入redis
 */
@Component
public class SeckillGoodsPushTask {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RedisTemplate redisTemplate;


    @Scheduled(cron = "0/5 * * * * ?")
    public void loadGoodsPushRedis(){
        /*1.查询活动没结束的所有秒杀商品
	        1)计算秒杀时间段
	        2)状态必须为审核通过 status=1
	        3)商品库存个数>0
	        4)活动没有结束  endTime>=now()
	            时间菜单的开始时间=<star_time && end_time<时间菜单的开始时间+2小时
	        5)在Redis中没有该商品的缓存
	        6)执行查询获取对应的结果集
          2.将活动没有结束的秒杀商品入库
        * */
        //获取时间段集合
        List<Date> dateMenus = DateUtil.getDateMenus();

        //循环时间段
        for (Date dateMenu : dateMenus) {
            //时间的字符串格式yyyyMMddHH
            String timespace = DateUtil.data2str(dateMenu, "yyyyMMddHH");

            /*
            * 1.状态必须为审核通过 status=1
	          2.商品库存个数>0
	          3.活动没有结束  endTime>=now()
	            时间菜单的开始时间=<star_time && end_time<时间菜单的开始时间+2小时
	        */

            //根据时间段数据查询对应的秒杀商品数据
            Example example = new Example(SeckillGoods.class);
            Example.Criteria criteria = example.createCriteria();
            // 1)商品必须审核通过  status=1
            criteria.andEqualTo("status","1");
            // 2)库存>0
            criteria.andGreaterThan("stockCount",0);
            // 3) 时间菜单的开始时间=<star_time && end_time<时间菜单的开始时间+2小时
            criteria.andGreaterThanOrEqualTo("startTime",dateMenu);
            criteria.andLessThan("endTime",DateUtil.addDateHour(dateMenu,2));

            //排除已经存入到redis的seckillGoods-->1.求出当前命名空间所有的商品的id(key) 2.每次查询排除掉之前存在的商品的key的数据
            Set keys = redisTemplate.boundHashOps("SeckillGoods_"+timespace).keys();
            if(keys!=null&&keys.size()>0){
                //排除
                criteria.andNotIn("id",keys);
            }

            //查询数据
            List<SeckillGoods> seckillGoods = seckillGoodsMapper.selectByExample(example);

            //存入redis
            for (SeckillGoods seckillGood : seckillGoods) {
                redisTemplate.boundHashOps("SeckillGoods_"+timespace).put(seckillGood.getId(),seckillGood);

                //商品数据队列存储,防止高并发超卖
                Long[] ids = pushIds(seckillGood.getStockCount(), seckillGood.getId());
                redisTemplate.boundListOps("SeckillGoodsCountList_"+seckillGood.getId()).leftPushAll(ids);
                //自增计数器
                redisTemplate.boundHashOps("SeckillGoodsCount").increment(seckillGood.getId(),seckillGood.getStockCount());
            }
        }
    }


    /***
     * 将商品ID存入到数组中
     * @param len:长度
     * @param id :值
     * @return
     */
    public Long[] pushIds(int len,Long id){
        Long[] ids = new Long[len];
        for (int i = 0; i <ids.length ; i++) {
            ids[i]=id;
        }
        return ids;
    }

}
