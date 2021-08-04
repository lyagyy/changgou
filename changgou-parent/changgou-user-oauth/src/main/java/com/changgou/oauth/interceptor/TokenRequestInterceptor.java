package com.changgou.oauth.interceptor;

import com.changgou.oauth.util.AdminToken;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Configuration;


@Configuration
public class TokenRequestInterceptor implements RequestInterceptor {

    /**
     * 在feign调用之前，进行拦截
     * @param template
     */
    @Override
    public void apply(RequestTemplate template) {
        /*
        *从数据库加载用户信息
        * 1.没有令牌，feign调用之前，生成令牌(admin)
        * 2.feign调用之前，令牌需要携带过去
        * 3.feign调用之前，令牌需要存放到Header文件中
        * 4.请求->feign调用->拦截器RequestInterceptor->feign调用之前执行拦截
         */
        //生成admin令牌
        String token = AdminToken.adminToken();
        template.header("Authorization","bearer "+token);
    }
}
