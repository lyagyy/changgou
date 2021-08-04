package com.changgou.order.service.impl;

import com.alibaba.fescar.spring.annotation.GlobalTransactional;
import com.changgou.goods.feign.SkuFeign;
import com.changgou.order.dao.OrderItemMapper;
import com.changgou.order.dao.OrderMapper;
import com.changgou.order.pojo.Order;
import com.changgou.order.pojo.OrderItem;
import com.changgou.order.service.CartService;
import com.changgou.order.service.OrderService;
import com.changgou.user.feign.UserFeign;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import entity.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

import javax.persistence.Id;
import java.text.SimpleDateFormat;
import java.util.*;

/****
 * @Author:shenkunlin
 * @Description:Order业务层接口实现类
 * @Date 2019/6/14 0:16
 *****/
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private CartService cartService;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SkuFeign skuFeign;

    @Autowired
    private UserFeign userFeign;
    /**
     * 增加Order
     * @param order
     */
    @GlobalTransactional(name = "add")//开启全局事务
    @Override
    public Order add(Order order){
        order.setId(String.valueOf(idWorker.nextId()));//主键id
        //查出所有购物车(后续应该剔除未勾选的商品)
        List<OrderItem> orderItems = new ArrayList<OrderItem>();//redisTemplate.boundHashOps("Cart_"+order.getUsername()).values();

        //获取勾选的商品id，需要下单的商品，将要下单的商品的id信息从购物车中移除
        for (Long skuId:order.getSkuIds()){
            orderItems.add((OrderItem) redisTemplate.boundHashOps("Cart_"+order.getUsername()).get(skuId));
            redisTemplate.boundHashOps("Cart_"+order.getUsername()).delete(skuId);
        }
        /*订单添加一次*/
        //统计计算
        int totalMoney=0;
        int totalPayMoney=0;
        int num = 0;
        //封装Map<Long,Integer> 封装递减数据
        Map<String,Integer> decrmap= new HashMap<String, Integer>();
        for (OrderItem orderItem : orderItems) {
            //总金额
            totalMoney +=orderItem.getMoney();
            //实际支付金额
            totalPayMoney+=orderItem.getPayMoney();
            //总数量
            num+=orderItem.getNum();
            orderItem.setId(String.valueOf(idWorker.nextId()));
            orderItem.setIsReturn("0");
            orderItem.setOrderId(order.getId());

            //封装递减数据
            decrmap.put(orderItem.getSkuId().toString(),orderItem.getNum());

        }

        order.setTotalNum(num);
        order.setTotalMoney(totalMoney);
        order.setPayMoney(totalPayMoney);
        order.setPreMoney(totalMoney-totalPayMoney);

        //其他数据完善
        order.setCreateTime(new Date());
        order.setUpdateTime(order.getCreateTime());
        order.setBuyerRate("0");        //0:未评价，1：已评价
        order.setSourceType("1");       //来源，1：WEB
        order.setOrderStatus("0");      //0:未完成,1:已完成，2：已退货
        order.setPayStatus("0");        //0:未支付，1：已支付，2：支付失败
        order.setConsignStatus("0");    //0:未发货，1：已发货，2：已收货
        order.setPayType("1"); // '支付类型，1、在线支付、0 货到付款'
        orderMapper.insertSelective(order);



        /*明细添加多次*/
        //添加订单明细
        for (OrderItem orderItem : orderItems) {
            /*orderItem.setId(String.valueOf(idWorker.nextId()));
            orderItem.setIsReturn("0");
            orderItem.setOrderId(order.getId());*/
            orderItemMapper.insertSelective(orderItem);
        }
        System.out.println("添加订单完成");
        /*库存递减*/
        skuFeign.decrCount(decrmap);
        System.out.println("库存减少完成");
        /*增加积分*/
        userFeign.addPoints(1);
        System.out.println("添加积分完成");
        /*线上支付，记录订单*/
        if(order.getPayType().equalsIgnoreCase("1")){
            redisTemplate.boundHashOps("Order").put(order.getId(),order);
        }

        /*删除购物车信息*/
        return order;
    }

    /***
     * 订单修改
     * @param orderId
     * @param transactionid  微信支付的交易流水号
     */
    @Override
    public void updateStatus(String orderId, String paytime,String transactionid) throws Exception{
        //时间转换
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Date payTimeInfo = simpleDateFormat.parse(paytime);

        //1.修改订单
        Order order = orderMapper.selectByPrimaryKey(orderId);
        //order.setUpdateTime(new Date());    //时间也可以从微信接口返回过来，这里为了方便，我们就直接使用当前时间了
        order.setPayTime(payTimeInfo);
        order.setTransactionId(transactionid);  //交易流水号
        order.setPayStatus("1");    //已支付
        orderMapper.updateByPrimaryKeySelective(order);

        //2.删除Redis中的订单记录
        redisTemplate.boundHashOps("Order").delete(orderId);
    }

    /**
     * 删除（修改订单支付状态）
     * @param id
     */
    @Override
    public void deleteOrder(String id){
        //orderMapper.deleteByPrimaryKey(id);
        //改状态
        Order order = (Order) redisTemplate.boundHashOps("Order").get(id);
        order.setUpdateTime(new Date());
        order.setPayStatus("2");    //支付失败
        orderMapper.updateByPrimaryKeySelective(order);

        //删除缓存
        redisTemplate.boundHashOps("Order").delete(id);
    }

    /**
     * Order条件+分页查询
     * @param order 查询条件
     * @param page 页码
     * @param size 页大小
     * @return 分页结果
     */
    @Override
    public PageInfo<Order> findPage(Order order, int page, int size){
        //分页
        PageHelper.startPage(page,size);
        //搜索条件构建
        Example example = createExample(order);
        //执行搜索
        return new PageInfo<Order>(orderMapper.selectByExample(example));
    }

    /**
     * Order分页查询
     * @param page
     * @param size
     * @return
     */
    @Override
    public PageInfo<Order> findPage(int page, int size){
        //静态分页
        PageHelper.startPage(page,size);
        //分页查询
        return new PageInfo<Order>(orderMapper.selectAll());
    }

    /**
     * Order条件查询
     * @param order
     * @return
     */
    @Override
    public List<Order> findList(Order order){
        //构建查询条件
        Example example = createExample(order);
        //根据构建的条件查询数据
        return orderMapper.selectByExample(example);
    }

    /**
     * 根据id删除订单
     * @param id
     */
    @Override
    public void delete(String id) {
        Order order = new Order();
        order.setId(id);
        orderMapper.delete(order);
    }


    /**
     * Order构建查询对象
     * @param order
     * @return
     */
    public Example createExample(Order order){
        Example example=new Example(Order.class);
        Example.Criteria criteria = example.createCriteria();
        if(order!=null){
            // 订单id
            if(!StringUtils.isEmpty(order.getId())){
                    criteria.andEqualTo("id",order.getId());
            }
            // 数量合计
            if(!StringUtils.isEmpty(order.getTotalNum())){
                    criteria.andEqualTo("totalNum",order.getTotalNum());
            }
            // 金额合计
            if(!StringUtils.isEmpty(order.getTotalMoney())){
                    criteria.andEqualTo("totalMoney",order.getTotalMoney());
            }
            // 优惠金额
            if(!StringUtils.isEmpty(order.getPreMoney())){
                    criteria.andEqualTo("preMoney",order.getPreMoney());
            }
            // 邮费
            if(!StringUtils.isEmpty(order.getPostFee())){
                    criteria.andEqualTo("postFee",order.getPostFee());
            }
            // 实付金额
            if(!StringUtils.isEmpty(order.getPayMoney())){
                    criteria.andEqualTo("payMoney",order.getPayMoney());
            }
            // 支付类型，1、在线支付、0 货到付款
            if(!StringUtils.isEmpty(order.getPayType())){
                    criteria.andEqualTo("payType",order.getPayType());
            }
            // 订单创建时间
            if(!StringUtils.isEmpty(order.getCreateTime())){
                    criteria.andEqualTo("createTime",order.getCreateTime());
            }
            // 订单更新时间
            if(!StringUtils.isEmpty(order.getUpdateTime())){
                    criteria.andEqualTo("updateTime",order.getUpdateTime());
            }
            // 付款时间
            if(!StringUtils.isEmpty(order.getPayTime())){
                    criteria.andEqualTo("payTime",order.getPayTime());
            }
            // 发货时间
            if(!StringUtils.isEmpty(order.getConsignTime())){
                    criteria.andEqualTo("consignTime",order.getConsignTime());
            }
            // 交易完成时间
            if(!StringUtils.isEmpty(order.getEndTime())){
                    criteria.andEqualTo("endTime",order.getEndTime());
            }
            // 交易关闭时间
            if(!StringUtils.isEmpty(order.getCloseTime())){
                    criteria.andEqualTo("closeTime",order.getCloseTime());
            }
            // 物流名称
            if(!StringUtils.isEmpty(order.getShippingName())){
                    criteria.andEqualTo("shippingName",order.getShippingName());
            }
            // 物流单号
            if(!StringUtils.isEmpty(order.getShippingCode())){
                    criteria.andEqualTo("shippingCode",order.getShippingCode());
            }
            // 用户名称
            if(!StringUtils.isEmpty(order.getUsername())){
                    criteria.andLike("username","%"+order.getUsername()+"%");
            }
            // 买家留言
            if(!StringUtils.isEmpty(order.getBuyerMessage())){
                    criteria.andEqualTo("buyerMessage",order.getBuyerMessage());
            }
            // 是否评价
            if(!StringUtils.isEmpty(order.getBuyerRate())){
                    criteria.andEqualTo("buyerRate",order.getBuyerRate());
            }
            // 收货人
            if(!StringUtils.isEmpty(order.getReceiverContact())){
                    criteria.andEqualTo("receiverContact",order.getReceiverContact());
            }
            // 收货人手机
            if(!StringUtils.isEmpty(order.getReceiverMobile())){
                    criteria.andEqualTo("receiverMobile",order.getReceiverMobile());
            }
            // 收货人地址
            if(!StringUtils.isEmpty(order.getReceiverAddress())){
                    criteria.andEqualTo("receiverAddress",order.getReceiverAddress());
            }
            // 订单来源：1:web，2：app，3：微信公众号，4：微信小程序  5 H5手机页面
            if(!StringUtils.isEmpty(order.getSourceType())){
                    criteria.andEqualTo("sourceType",order.getSourceType());
            }
            // 交易流水号
            if(!StringUtils.isEmpty(order.getTransactionId())){
                    criteria.andEqualTo("transactionId",order.getTransactionId());
            }
            // 订单状态,0:未完成,1:已完成，2：已退货
            if(!StringUtils.isEmpty(order.getOrderStatus())){
                    criteria.andEqualTo("orderStatus",order.getOrderStatus());
            }
            // 支付状态,0:未支付，1：已支付，2：支付失败
            if(!StringUtils.isEmpty(order.getPayStatus())){
                    criteria.andEqualTo("payStatus",order.getPayStatus());
            }
            // 发货状态,0:未发货，1：已发货，2：已收货
            if(!StringUtils.isEmpty(order.getConsignStatus())){
                    criteria.andEqualTo("consignStatus",order.getConsignStatus());
            }
            // 是否删除
            if(!StringUtils.isEmpty(order.getIsDelete())){
                    criteria.andEqualTo("isDelete",order.getIsDelete());
            }
        }
        return example;
    }



    /**
     * 修改Order
     * @param order
     */
    @Override
    public void update(Order order){
        orderMapper.updateByPrimaryKey(order);
    }



    /**
     * 根据ID查询Order
     * @param id
     * @return
     */
    @Override
    public Order findById(String id){
        return  orderMapper.selectByPrimaryKey(id);
    }

    /**
     * 查询Order全部数据
     * @return
     */
    @Override
    public List<Order> findAll() {
        return orderMapper.selectAll();
    }
}
