package com.changgou.token;

import org.junit.Test;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;

/*****
 * @Author: www.itheima
 * @Date:
 * @Description: com.changgou.token
 *  使用公钥解密令牌数据
 ****/
public class ParseJwtTest {

    /***
     * 校验令牌
     */
    @Test
    public void testParseToken(){
        //令牌
        String token =
"eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlcyI6IlJPTEVfVklQLFJPTEVfVVNFUiIsIm5hbWUiOiJpdGhlaW1hIiwiaWQiOiIxIn0.lQk5mqPcIirPUGs6UXcHWdxDachkTVoMmklzkcWKawUwVBL0fyZdn_pp56PFdOaDdYxok0GEKjAmu35e_HBQRIU1tSUtH4XlthAQP6A3zalZZKdQnBbSvH1Gg2hZyewB2HJKU1g2F6mAkf6zDYM1UW_YNzK3yXClPoRV92eBhAKy5K9xQDZu8LsD4GWOQ1XQaRGrDp4Yu5aOQLNPD67UUbBQAFHseVCJlTJhTljiOfO2wKdnA1oJ5Xz1fT7-pAc37MVnZUqEV8h8eZnad51qMhDPbiXxvOYyXJQaaFn0S50P4pvoVPLrMq8xSPBNYG0PJc--yXSsGN7l6FaQN7RKKQ";
        //公钥
        String publickey =
                "-----BEGIN PUBLIC KEY-----MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArzSUMCSy//SMFcK3hHs1Q2XinIWa6edyBvyA4w8vqJogVAX08uFNYL9upcRy2JvJrtSb6M1zZVktzYHa14RYydxw4VtNMOnoZ31XXLGpWC7OWwcva193LZflnBt27W9Tt4YCbc1R5PRD2fE7MydGtc47zrAyKm0q1z7OBQvkYxADDeiIzLFPTTgNAhhIdqkcRc7MPoiYUYdtofwMhUCP5wMEPn3IynwBKQ7gT/tDN9CwAkrzsq7Tg2Qi9w8bpEtmn+nuMoMSub15lGdfOExidLrI4qMAT9p29CJ49kemfz9D+DaACF5mJMB6RL+qny0Zz34yHUX0Tppv9YjHc/KrvwIDAQAB-----END PUBLIC KEY-----";
        //校验Jwt
        Jwt jwt = JwtHelper.decodeAndVerify(token, new RsaVerifier(publickey));

        //获取Jwt原始内容 载荷
        String claims = jwt.getClaims();
        System.out.println(claims);
        //jwt令牌
        String encoded = jwt.getEncoded();
        System.out.println(encoded);
    }
}
