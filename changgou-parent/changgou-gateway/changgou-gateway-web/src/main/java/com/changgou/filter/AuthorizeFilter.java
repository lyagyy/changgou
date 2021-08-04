package com.changgou.filter;

import com.changgou.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/*
@Component
public class AuthorizeFilter implements GlobalFilter,Ordered {
    //令牌头名字
    private static final String AUTHORIZE_TOKEN = "Authorization";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //获取Request、Response对象
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();


       */
/*


        //获取用户令牌信息
        //1.头文件中
        String token = request.getHeaders().getFirst(AUTHORIZE_TOKEN);

        //2.如果头文件中没有，则从请求参数中获取令牌
        if(StringUtils.isEmpty(token)){
            token= request.getQueryParams().getFirst(AUTHORIZE_TOKEN);
        }

        //3.cookie
        if(StringUtils.isEmpty(token)){
            HttpCookie httpCookie = request.getCookies().getFirst(AUTHORIZE_TOKEN);
            if (httpCookie!=null){
                token=httpCookie.getValue();
            }
        }


        //如果没有令牌，则拦截
        if(StringUtils.isEmpty(token)){

            //设置方法不允许被访问，405错误代码
            response.setStatusCode(HttpStatus.METHOD_NOT_ALLOWED);
            //响应空数据
          return   response.setComplete();
        }

        try {
            //如果有令牌，则校验是否有效
            JwtUtil.parseJWT(token);
        } catch (Exception e) {
            e.printStackTrace();
            //无效则拦截

            //设置没有权限的状态码 401
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            //响应空数据
            return  response.setComplete();
        }

        //有效放行
        return chain.filter(exchange);
    }


    */
/***
     * 过滤器执行顺序
     * @return
     *//*

    @Override
    public int getOrder() {
        return 0;
    }
}
*/
@Component
public class AuthorizeFilter implements GlobalFilter, Ordered {

    //令牌头名字
    private static final String AUTHORIZE_TOKEN = "Authorization";

    /***
     * 全局过滤器
     * @param exchange
     * @param chain
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //获取Request、Response对象
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        //获取请求的URI
        String path = request.getURI().getPath();

        //如果是登录、goods等开放的微服务[这里的goods部分开放],则直接放行,这里不做完整演示，完整演示需要设计一套权限系统
        if (!URLFilter.hasAuthorize(path)) {
            //放行
            Mono<Void> filter = chain.filter(exchange);
            return filter;

        }

        //获取头文件中的令牌信息
        String token = request.getHeaders().getFirst(AUTHORIZE_TOKEN);
        //true 令牌在头文件中  false令牌不在头文件中->将令牌封装到头文件中，在传递给其他微服务
        boolean hasToken =true;

        //如果头文件中没有，则从请求参数中获取
        if (StringUtils.isEmpty(token)) {
            token = request.getQueryParams().getFirst(AUTHORIZE_TOKEN);
            hasToken=false;
        }


        //从cookie中获取令牌数据
        if (StringUtils.isEmpty(token)){
        HttpCookie first = request.getCookies().getFirst("AUTHORIZE_TOKEN");
        if(first!=null){
            token=first.getValue();
        }
        }


        //如果为空，则输出错误代码
        if (StringUtils.isEmpty(token)) {
            //设置方法不允许被访问，405错误代码
            response.setStatusCode(HttpStatus.METHOD_NOT_ALLOWED);
            return response.setComplete();
        }else {
            if (!hasToken){
                //判断当前令牌是否有bearer前缀，如果没有，则添加前缀
                if(!token.startsWith("bearer")&&!token.startsWith("Bearer")){
                    token="bearer"+token;
                }

                //将令牌数据添加到头文件中
                request.mutate().header(AUTHORIZE_TOKEN,token);
            }
        }


        //放行
        return chain.filter(exchange);
    }


    /***
     * 过滤器执行顺序
     * @return
     */
    @Override
    public int getOrder() {
        return 0;
    }
}