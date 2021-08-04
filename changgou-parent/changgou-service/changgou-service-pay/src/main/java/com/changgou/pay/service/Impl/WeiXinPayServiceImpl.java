package com.changgou.pay.service.Impl;

import com.alibaba.fastjson.JSON;
import com.changgou.pay.service.WeiXinPayService;
import com.github.wxpay.sdk.WXPayUtil;
import entity.HttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class WeiXinPayServiceImpl implements WeiXinPayService {



    @Value("${weixin.appid}")
    private String appid;

    @Value("${weixin.partner}")
    private String partner;

    @Value("${weixin.partnerkey}")
    private String partnerkey;

    @Value("${weixin.notifyurl}")
    private String notifyurl;

    /*
     * 获取二维码
     * @param parameterMap
     * @return
       */
    @Override
    public Map createNative(Map<String, String> parameterMap) {


        try {
            Map<String,String> paramMap = new HashMap<String, String>();
            //封装参数
            paramMap.put("appid",appid); //应用ID
            paramMap.put("mch_id", partner); //商户ID号
            paramMap.put("nonce_str", WXPayUtil.generateNonceStr()); //随机字符串
            paramMap.put("body","畅购"); //订单描述
            paramMap.put("out_trade_no",parameterMap.get("outtradeno"));   //商户订单号
            paramMap.put("total_fee",parameterMap.get("totalfee"));   //交易金额
            paramMap.put("spbill_create_ip", "127.0.0.1"); //终端IP
            paramMap.put("notify_url", notifyurl); //回调地址
            paramMap.put("trade_type", "NATIVE"); //交易类型


            //获取自定义数据
            String exchange = parameterMap.get("exchange");
            String routingkey = parameterMap.get("routingkey");
            Map<String,String> attchMap = new HashMap<String, String>();
            attchMap.put("exchange",exchange);
            attchMap.put("routingkey",routingkey);
            //如果是秒杀订单，需要传username
            String username = parameterMap.get("username");
            if(!StringUtils.isEmpty(username)){
                attchMap.put("username", username);
            }
            String attach= JSON.toJSONString(attchMap);
            paramMap.put("attach", attach);

            //将Map转成xml字符，并携带签名
            String paramXml = WXPayUtil.generateSignedXml(paramMap,partnerkey);

            //url地址
            String url = "https://api.mch.weixin.qq.com/pay/unifiedorder";
            HttpClient httpClient = new HttpClient(url);
            //提交方式
            httpClient.setHttps(true);
            //提交参数
            httpClient.setXmlParam(paramXml);
            //执行请求
            httpClient.post();
            //获取返回的数据
            String result = httpClient.getContent();
            //返回的数据转为Map
            Map<String,String> resultMap = WXPayUtil.xmlToMap(result);
            return resultMap;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

  /*  *//****
     * 创建二维码
     * @param out_trade_no : 客户端自定义订单编号
     * @param total_fee    : 交易金额,单位：分
     * @return
     *//*
    @Override
    public Map createNative(String out_trade_no, String total_fee){
        try {
            //1、封装参数
            Map param = new HashMap();
            param.put("appid", appid);                              //应用ID
            param.put("mch_id", partner);                           //商户ID号
            param.put("nonce_str", WXPayUtil.generateNonceStr());   //随机数
            param.put("body", "畅购");                            	//订单描述
            param.put("out_trade_no",out_trade_no);                 //商户订单号
            param.put("total_fee", total_fee);                      //交易金额
            param.put("spbill_create_ip", "127.0.0.1");           //终端IP
            param.put("notify_url", notifyurl);                    //回调地址
            param.put("trade_type", "NATIVE");                     //交易类型

            //2、将参数转成xml字符，并携带签名
            String paramXml = WXPayUtil.generateSignedXml(param, partnerkey);

            ///3、执行请求
            HttpClient httpClient = new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");
            httpClient.setHttps(true);
            httpClient.setXmlParam(paramXml);
            httpClient.post();

            //4、获取参数
            String content = httpClient.getContent();
            Map<String, String> stringMap = WXPayUtil.xmlToMap(content);
            System.out.println("stringMap:"+stringMap);

            //5、获取部分页面所需参数
            Map<String,String> dataMap = new HashMap<String,String>();
            dataMap.put("code_url",stringMap.get("code_url"));
            dataMap.put("out_trade_no",out_trade_no);
            dataMap.put("total_fee",total_fee);

            return dataMap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }*/

    /**
     * 查询订单状态
     * @param parameterMap
     * @return
     */
    @Override
    public Map queryPayStatus(String outtradeno) {


        try {
            Map<String,String> paramMap = new HashMap<String, String>();
            //封装参数
            paramMap.put("appid",appid); //应用ID
            paramMap.put("mch_id", partner); //商户ID号
            paramMap.put("nonce_str", WXPayUtil.generateNonceStr()); //随机字符串
            paramMap.put("out_trade_no",outtradeno);   //商户订单号
            //将Map转成xml字符，并携带签名
            String paramXml = WXPayUtil.generateSignedXml(paramMap,partnerkey);

            //url地址
            String url = "https://api.mch.weixin.qq.com/pay/orderquery";
            HttpClient httpClient = new HttpClient(url);
            //提交方式
            httpClient.setHttps(true);
            //提交参数
            httpClient.setXmlParam(paramXml);
            //执行请求
            httpClient.post();
            //获取返回的数据
            String result = httpClient.getContent();
            //返回的数据转为Map
            Map<String,String> resultMap = WXPayUtil.xmlToMap(result);
            return resultMap;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
