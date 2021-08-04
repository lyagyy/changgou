package com.changgou.seckill.mq;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 1.延时超时队列-->负责数据暂时存储 queue1
 * 2.真正监听的消息队列 queue2
 * 3.创建交换机
 */
@Configuration
public class QueueConfig {


    /*1.延时超时队列-->负责数据暂时存储 queue1*/
    @Bean
    public Queue delayMessageQueue() {
        return QueueBuilder.durable("delaySeckillQueue")
                .withArgument("x-dead-letter-exchange","seckillExchange")        // 消息超时进入死信队列，绑定死信队列交换机
                .withArgument("x-dead-letter-routing-key","seckillQueue")   // 绑定指定的routing-key
                .build();
    }

    /*2.真正监听的消息队列 queue2*/
    @Bean
    public Queue seckillQueue(){
        return new Queue("seckillQueue");
    }

    /*3.创建交换机*/
    @Bean
    public Exchange seckillExchange(){
        return new DirectExchange("seckillExchange");
    }

    /*4.队列绑定交换机绑定*/
    @Bean
    public Binding seckillQueueBindExchange(Queue seckillQueue,Exchange seckillExchange){
        return BindingBuilder.bind(seckillQueue).to(seckillExchange).with("seckillQueue").noargs();
    }
}
