package com.changgou.pay.service;

import java.util.Map;

public interface WeiXinPayService {

    /**
     * 获取二维码
     * @param parameterMap
     * @return
     */
    Map createNative(Map<String,String> parameterMap);
    //Map createNative(String out_trade_no, String total_fee);

    /**
     * 查询订单状态
     * @param parameterMap
     * @return
     */
    Map queryPayStatus(String outtradeno);

}
