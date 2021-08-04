import com.github.wxpay.sdk.WXPayUtil;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信sdk相关测试
 */
public class WeiXinUtilTest {

    /**
     * 1.生成随机字符
     * 2.将Map转为XML字符串
     * 3.将Map转为XML字符串,并且生成签名
     * 4.将XML字符串转为map
     */
    @Test
    public void testDemo1() throws Exception {
        //生成随机字符
        String str = WXPayUtil.generateNonceStr();
        System.out.println("随机字符串"+str);

        //将Map转为XML字符串
        Map<String,String> map = new HashMap<String, String>();
        map.put("id","NO.001");
        map.put("title","畅购商城");
        map.put("money","998");

        String mapToXml = WXPayUtil.mapToXml(map);
        System.out.println("XML字符串+\n"+mapToXml);

        //将Map转为XML字符串,并且生成签名
        String signedXml = WXPayUtil.generateSignedXml(map, "itcast");
        System.out.println("XML字符串带有签名+\n"+signedXml);


        //将XML字符串转为map
        Map<String, String> xmlToMap = WXPayUtil.xmlToMap(signedXml);
        System.out.println("xml转为map："+xmlToMap);
    }


}
