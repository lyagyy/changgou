package com.changgou.pay.mq;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class MQconfig {

    /**
     * 读取配置文件中的信息的对象
     */
    @Autowired
    private Environment env;

    /****************普通订单**************/
    /***
     * 创建DirectExchange交换机
     * @return
     *//*
    @Bean
    public DirectExchange basicExchange(){
        return new DirectExchange(env.getProperty("mq.pay.exchange.order"), true,false);
    }

    *//***
     * 创建队列
     * @return
     *//*
    @Bean
    public Queue queueOrder(){
        return new Queue(env.getProperty("mq.pay.queue.order"), true);
    }

    *//****
     * 队列绑定到交换机上
     * @return
     *//*
    @Bean
    public Binding basicBinding(){
        return BindingBuilder.bind(queueOrder()).to(basicExchange()).with(env.getProperty("mq.pay.routing.key"));
    }


    *//*********************秒杀************************//*
    *//***
     * 创建DirectExchange交换机
     * @return
     *//*
    @Bean
    public DirectExchange basicSeckillExchange(){
        return new DirectExchange(env.getProperty("mq.pay.exchange.seckillorder"), true,false);
    }

    *//***
     * 创建队列
     * @return
     *//*
    @Bean(name = "queueSeckillOrder")
    public Queue queueSeckillOrder(){
        return new Queue(env.getProperty("mq.pay.queue.seckillorder"), true);
    }

    *//****
     * 队列绑定到交换机上
     * @return
     *//*
    @Bean
    public Binding basicSeckillBinding(){
        return BindingBuilder.bind(queueSeckillOrder()).to(basicSeckillExchange()).with(env.getProperty("mq.pay.routing.seckillkey"));
    }
*/
    //创建队列
    @Bean
    public Queue createQueue(){
        return new Queue(env.getProperty("mq.pay.queue.order"));
    }

    //创建交换机

    @Bean
    public DirectExchange basicExchanage(){
        return new DirectExchange(env.getProperty("mq.pay.exchange.order"));
    }

    //绑定

    @Bean
    public Binding basicBinding(){
        return  BindingBuilder.bind(createQueue()).to(basicExchanage()).with(env.getProperty("mq.pay.routing.key"));
    }



    //创建秒杀队列
    @Bean(name="seckillQueue")
    public Queue createSeckillQueue(){
        return new Queue(env.getProperty("mq.pay.queue.seckillorder"));
    }

    //创建秒杀交换机

    @Bean(name="seckillExchanage")
    public DirectExchange basicSeckillExchanage(){
        return new DirectExchange(env.getProperty("mq.pay.exchange.seckillorder"));
    }

    //绑定秒杀

    @Bean(name="SeckillBinding")
    public Binding basicSeckillBinding(){
        return  BindingBuilder.bind(createSeckillQueue()).to(basicSeckillExchanage()).with(env.getProperty("mq.pay.routing.seckillkey"));
    }

}
