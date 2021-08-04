package com.changgou.seckill.mq;

import com.alibaba.fastjson.JSON;
import com.changgou.seckill.service.SeckillOrderService;
import com.github.wxpay.sdk.WXPayUtil;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RabbitListener(queues = "${mq.pay.queue.seckillorder}")
public class SeckillMessageListener {

    @Autowired
    private SeckillOrderService seckillOrderService;


    @RabbitHandler
    public void getMessage(String message){
        System.out.println(message);
        try {
            //将string转为map

            Map<String,String> resultMap= JSON.parseObject(message,Map.class);
            //状态码
            String return_code = resultMap.get("return_code");
            //判断return_code是否为success
            if(return_code.equals("SUCCESS")){
                //订单号
                String out_trade_no = resultMap.get("out_trade_no");
                //自定义数据
                String attach = resultMap.get("attach");
                Map<String,String> attachMap= JSON.parseObject(attach,Map.class);

                //result_code-->业务结果--SUCCESS-->改订单状态
                String result_code = resultMap.get("result_code");
                if (result_code.equals("SUCCESS")){
                    //改订单状态
                    seckillOrderService.updatePayStatus(attachMap.get("username"),resultMap.get("transaction_id"),resultMap.get("time_end"));

                }else {
                    //fail-->删除订单[真实工作中是存入mysql]-->回滚库存
                    seckillOrderService.deleteOrder(attachMap.get("username"));
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
