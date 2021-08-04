package com.changgou.seckill.task;

import com.alibaba.fastjson.JSON;
import com.changgou.seckill.dao.SeckillGoodsMapper;
import com.changgou.seckill.pojo.SeckillGoods;
import com.changgou.seckill.pojo.SeckillOrder;
import entity.IdWorker;
import entity.SeckillStatus;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class MultiThreadingCreateOrder {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;
    /***
     * 异步执行
     * @Async：该方法会异步执行（底层多线程方式）
     */
    @Async
    public void createOrder(){


        try {
            System.out.println("准备睡会在下单");
            Thread.sleep(10000);

            //从队列中获取排队信息
            SeckillStatus  seckillStatus = (SeckillStatus) redisTemplate.boundListOps("SeckillOrderQueue").rightPop();


            /*if(seckillStatus==null){
                return;
            }*/

            //从队列中获取一个商品
            Object sgood = redisTemplate.boundListOps("SeckillGoodsCountList_" + seckillStatus.getGoodsId()).rightPop();
            if(sgood==null){
                //清理排队信息
                clearQueue(seckillStatus);
                //如果没有库存，则直接抛出异常
                throw new RuntimeException("已售罄!");
                /*return;*/
            }

            //时间区间
            String time = seckillStatus.getTime();
            //用户登录名
            String username = seckillStatus.getUsername();
            //用户抢购商品id
            Long id = seckillStatus.getGoodsId();

            //获取商品秒杀信息
            SeckillGoods goods = (SeckillGoods) redisTemplate.boundHashOps("SeckillGoods_" + time).get(id);


            //如果有库存，则创建秒杀商品订单
            SeckillOrder seckillOrder =new SeckillOrder();
            seckillOrder.setId(idWorker.nextId());
            seckillOrder.setSeckillId(id);
            seckillOrder.setMoney(goods.getPrice());
            seckillOrder.setUserId(username);
            seckillOrder.setCreateTime(new Date());
            seckillOrder.setStatus("0");


            //将秒杀订单存入到Redis中
            redisTemplate.boundHashOps("SeckillOrder").put(username,seckillOrder);

            //库存减少-->此处控制数量，用redis控制，不要用goods本身，因为每次操作都是基于内存操作，并发情况会导致数据不精准
            //goods.setStockCount(goods.getStockCount()-1); 并发会导致数据不精准
            Long seckillGoodsCount = redisTemplate.boundHashOps("SeckillGoodsCount").increment(id, -1);
            goods.setStockCount(seckillGoodsCount.intValue());
            System.out.println(Thread.currentThread().getId()+"操作后剩余库存="+seckillGoodsCount);
            //判断当前商品是否还有库存
            if(seckillGoodsCount<=0){
                //并且将商品数据同步到MySQL中
                seckillGoodsMapper.updateByPrimaryKey(goods);
                //如果没有库存,则清空Redis缓存中该商品
                redisTemplate.boundHashOps("SeckillGoods_" + time).delete(id);
            }else {
                //如果有库存，则直数据重置到Reids中
                redisTemplate.boundHashOps("SeckillGoods_" + time).put(id,goods);
            }

            //抢单成功，更新抢单状态,排队->等待支付
            seckillStatus.setStatus(2);//待付款
            seckillStatus.setOrderId(seckillOrder.getId());
            seckillStatus.setMoney(Float.valueOf(goods.getCostPrice()));
            redisTemplate.boundHashOps("UserQueueStatus").put(username,seckillStatus);

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
            System.out.println("下单时间"+simpleDateFormat.format(new Date()));

            //发送消息给延迟队列
            rabbitTemplate.convertAndSend("delaySeckillQueue", (Object) JSON.toJSONString(seckillStatus), new MessagePostProcessor() {
                @Override
                public Message postProcessMessage(Message message) throws AmqpException {
                    message.getMessageProperties().setExpiration("10000");
                    return message;
                }
            });

            System.out.println("我结束了");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /***
     * 清理用户排队信息
     * @param seckillStatus
     */
    public void clearQueue(SeckillStatus seckillStatus){
        //清理排队标示
        redisTemplate.boundHashOps("UserQueueCount").delete(seckillStatus.getUsername());

        //清理抢单标示
        redisTemplate.boundHashOps("UserQueueStatus").delete(seckillStatus.getUsername());
    }


}
